/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "facedetection.h"
#include "crvimage.h"
#include "eviacam.h"
#include "timeutil.h"

#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/video/tracking.hpp>
#include <pthread.h>
#include <unistd.h>
#include <sys/resource.h>
#include <linux/sched.h>

namespace eviacam {

void* thread_entry (void* t)
{
	/*
	 * Set the priority of the thread (the nice value of the linux
	 * kernel) a little below (a positive value) the normal priority
	 * (which is 0) to improve responsiveness (i.e. scanning for a
	 * face is costly and does not need to be run with hard read-time
	 * constraints).
	 *
	 * For more info, check the setpriority man page
	 *
	 * This is how is done in by the Android framework see:
	 * 	os_changeThreadPriority
	 */
	//LOGD("Face detection thread id (gettid): %d \n", gettid());
	//LOGD("Face detection thread id (getpid): %d \n", getpid());
	int tid= gettid();

	/*
	 * He we initially tried SCHED_BATCH but it seems that this
	 * thread suffers form starvation under high CPU load.
	 */
	struct sched_param param;
	param.sched_priority = 0;
	if (sched_setscheduler(tid, SCHED_NORMAL, &param)) {
		LOGW("sched_setscheduler failed");
	}

	if (setpriority(PRIO_PROCESS, tid, 1)!= 0) {
		LOGW("setpriority failed");
	}

	FaceDetection* obj= static_cast<FaceDetection*>(t);
	obj->threadEntry();
	
	pthread_exit(NULL);

	return NULL;	// make the compiler happy
}

FaceDetection::FaceDetection (const char* cascadePath)
: m_finishThread(false)
, m_processingFrame(true) // true to avoid race conditions during initialization
, m_lastSubmit_tstamp(0)
, m_cpuUsage(CPU_NORMAL)
, m_faceCascade(NULL)
, m_storage(NULL)
, m_faceDetected(true)
, m_detectorInfoRetrieved(true)
{
	//
	// open face haar-cascade
	// 
	try {
		m_faceCascade = (CvHaarClassifierCascade*) cvLoad(cascadePath, 0, 0, 0);
	}
	catch (cv::Exception& e) {
		LOGW("%s:%d %s\n", __FILE__, __LINE__, e.what());
		LOGW("continuing without face detection");
	}

	//
	// create and start thread
	//
	if (m_faceCascade) {	
		m_storage = cvCreateMemStorage(0);
		
		//
		// TODO: check return values
		//		
		
		// initialize mutex and condition objects
		pthread_mutex_init (&m_condition_mutex, NULL);
		pthread_cond_init (&m_condition, NULL);
		
		// spawn thread		
		pthread_attr_init(&m_attr);
		pthread_attr_setdetachstate(&m_attr, PTHREAD_CREATE_JOINABLE);
		pthread_create(&m_thread, &m_attr, thread_entry, this);
	}
}

FaceDetection::~FaceDetection ()
{
	if (m_faceCascade) {
		// this means that thread has been started and needs to be stopped
		
		// lock
		pthread_mutex_lock(&m_condition_mutex);
		m_finishThread= true;
		if (!m_processingFrame)
			// thread sleeping, needs signal
			pthread_cond_signal(&m_condition);			
		// unlock
		pthread_mutex_unlock(&m_condition_mutex);
			
		// join thread
		pthread_join(m_thread, NULL);
		
		// clean-up
		pthread_attr_destroy(&m_attr);
		pthread_mutex_destroy(&m_condition_mutex);
		pthread_cond_destroy(&m_condition);
  
		cvReleaseHaarClassifierCascade(&m_faceCascade);
		
		cvReleaseMemStorage(&m_storage);
		m_storage = NULL;
		m_faceCascade = NULL;
	}

	LOGD("FaceDetection: cleanup completed");
}

void FaceDetection::threadEntry()
{
	LOGD("FaceDetection: threadEntry(): start");

	for (;;) {
		// lock
		pthread_mutex_lock(&m_condition_mutex);
		
		m_processingFrame= false;
		
		// need to exit?
		if (m_finishThread) {
			pthread_mutex_unlock(&m_condition_mutex);
			break;
		}

		// go to sleep
		pthread_cond_wait(&m_condition, &m_condition_mutex);
	
		// need to exit?
		if (m_finishThread) {
			pthread_mutex_unlock(&m_condition_mutex);
			break;
		}
		
		m_processingFrame= true;
		
		pthread_mutex_unlock(&m_condition_mutex);
		
		// process frame
		computeFaceTrackArea();
	}
	
	LOGD("FaceDetection: threadEntry(): finish");
}

void FaceDetection::computeFaceTrackArea ()
{
	CvSeq *face = cvHaarDetectObjects(
		m_frame.ptr(),
		m_faceCascade,
		m_storage,
		1.5, 2, CV_HAAR_DO_CANNY_PRUNING,
		cvSize(65, 65)
	);
	
	if (face->total> 0) {
		// face found, store results
		m_frameSize= m_frame.GetSize();
		CvRect* faceRect = (CvRect*) cvGetSeqElem(face, 0);		
		m_faceRegion= *faceRect;
		m_faceDetected= true;

		//LOGV("face detected: location (%d, %d) size (%d, %d)",
		//	m_faceRegion.x, m_faceRegion.y, m_faceRegion.width, m_faceRegion.height);
	}
	else
		m_faceDetected= false;
	
	m_detectorInfoRetrieved= false;

	cvClearMemStorage(m_storage);
}

void FaceDetection::submitFrame (CIplImage& image)
{
	if (m_faceCascade== NULL) return;	// initialization failed, do nothing

	// enough time elapsed since last submitted frame?
	unsigned long now = CTimeUtil::GetMiliCount();
	if (now - m_lastSubmit_tstamp< getThreadPeriod()) return;
	
	// detector information retrieved?
	if (!m_detectorInfoRetrieved) return;
	
	// lock
	pthread_mutex_lock(&m_condition_mutex);
	if (m_processingFrame) {
		// still working, do nothing
		goto exit_submitFrame;
	}

	// reallocate buffer when needed
	if (!m_frame.Initialized () || 
		image.Width() != m_frame.Width() || image.Height() != m_frame.Height() ) {		
		m_frame.Create (image.Width(), image.Height(), IPL_DEPTH_8U, "GRAY");
	}
	
	// copy frame to internal buffer
	cvCopy(image.ptr(), m_frame.ptr());
	
	// update time stamp
	m_lastSubmit_tstamp= now;

	// signal (awake worker thread)
	pthread_cond_signal(&m_condition);
	
exit_submitFrame:
	pthread_mutex_unlock(&m_condition_mutex);		
}

bool FaceDetection::retrieveDetectionInfo (bool &faceDetected, CvSize& frameSize, CvRect& faceRegion)
{
	if (m_faceCascade== NULL) return false;	// initialization failed, do nothing

	// face localization retrieved?
	if (m_detectorInfoRetrieved) return false;	// no new information available
	
	bool retval= false;
	
	// lock
	pthread_mutex_lock(&m_condition_mutex);
	if (m_processingFrame) {
		// still working, do nothing
		goto exit_retrieveFaceLocation;
	}
	
	retval= true;
	
	faceDetected= m_faceDetected;
	if (faceDetected) {
		frameSize= m_frameSize;
		faceRegion= m_faceRegion;
	}
	
	m_detectorInfoRetrieved= true;

exit_retrieveFaceLocation:
	pthread_mutex_unlock(&m_condition_mutex);
	
	return retval;
}

FaceDetection::ECpuUsage FaceDetection::getCpuUsage () const
{
	return m_cpuUsage;
}

void FaceDetection::setCpuUsage (FaceDetection::ECpuUsage value)
{
	assert (value>= CPU_LOWEST && value<= CPU_HIGHEST);
	m_cpuUsage= value;
}

// Period values (in s) for: CPU_LOWEST, CPU_LOW, CPU_NORMAL, CPU_HIGH, CPU_HIGHEST
static unsigned long CPU_PERIODS[]= { 1500, 800, 400, 100, 0 };
unsigned long FaceDetection::getThreadPeriod () const
{
	return CPU_PERIODS[m_cpuUsage];
}

}

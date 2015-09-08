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

#ifndef FACEDETECTION_H
#define FACEDETECTION_H

#include "crvimage.h"

namespace eviacam {

class FaceDetection
{
public:
	FaceDetection (const char* cascadePath);
	virtual ~FaceDetection ();

	// Submit a new frame for processing. The frame might be ignored if the 
	// detector is still processing another frame, not enough time has elapsed
	// since last processed frame or last face location has not been retrieved.
	// This call does not block the calling thread.
	void submitFrame (CIplImage& image);

	// Retrieve information about the detection face performed
	//
	// Return true if new data is available (i.e. a frame has been processed 
	// since the previous call to this method). Return false otherwise. In this
	// case parameters are left is untouched.
	//
	// If face has been detected, first argument is set to true and faceDetected
	// and frameSize are updated accordingly.
	bool retrieveDetectionInfo (bool &faceDetected, CvSize& frameSize, CvRect& faceRegion);
	
	// Set/get the value for the CPU usage
	enum ECpuUsage {CPU_LOWEST= 0, CPU_LOW, CPU_NORMAL, CPU_HIGH, CPU_HIGHEST};
	void setCpuUsage (ECpuUsage value);
	ECpuUsage getCpuUsage () const;
	
private:
	// Threading and synchronization stuff
	pthread_attr_t m_attr;
	pthread_t m_thread;
	pthread_mutex_t m_condition_mutex;
	pthread_cond_t m_condition;
	
	volatile bool m_finishThread; 	// used to finish thread gracefully
	volatile bool m_processingFrame; // true while thread is processing a frame

	// Timing stuff
	unsigned long m_lastSubmit_tstamp;
	ECpuUsage m_cpuUsage;
	
	// Image buffer
	CIplImage m_frame;
	
	// Cascade classifier stuff
	CvHaarClassifierCascade* m_faceCascade;
	CvMemStorage* m_storage;

	// Face location detection stuff
	CvSize m_frameSize;
	CvRect m_faceRegion;
	bool m_faceDetected;
	bool m_detectorInfoRetrieved;

	//
	// Private methods
	//
	void computeFaceTrackArea ();
	unsigned long getThreadPeriod () const;
	void threadEntry();
	
	friend void* thread_entry (void* t);
};

}
#endif

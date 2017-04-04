/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2008 - 2015 Cesar Mauri Loba (CREA Software Systems)
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

#include "visionpipeline.h"
#include "crvimage.h"
#include "timeutil.h"
#include "eviacam.h"

#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv/highgui.h>
#include <math.h>

namespace eviacam {

// constants
#define DEFAULT_TRACK_AREA_WIDTH 0.6f
#define DEFAULT_TRACK_AREA_HEIGHT 0.6f

VisionPipeline::VisionPipeline(const char* cascadePath)
: m_faceDetection(cascadePath)
, m_trackFace(true)
, m_corner_count(0)
{
	m_floatTrackArea.set(
		cvPoint2D32f((1.0f - DEFAULT_TRACK_AREA_WIDTH) / 2.0f,
					(1.0f - DEFAULT_TRACK_AREA_HEIGHT) / 2.0f),
		cvSize2D32f(DEFAULT_TRACK_AREA_WIDTH, DEFAULT_TRACK_AREA_HEIGHT));
	memset(m_corners, 0, sizeof(m_corners));
}

VisionPipeline::~VisionPipeline ()
{
}

bool VisionPipeline::allocWorkingSpace (int width, int height)
{
	bool retval;

	if (!m_imgPrev.Initialized () ||
		width != m_imgPrev.Width() ||
		height != m_imgPrev.Height() ) {

		retval= m_imgPrev.Create (width, height, IPL_DEPTH_8U, "GRAY");
		// TODO provide better error checking
		assert (retval);

		retval= m_imgCurr.Create (width, height, IPL_DEPTH_8U, "GRAY");
		// TODO provide better error checking
		assert (retval);

		return true;
	}

	return false;
}

static 
cv::Point rotatePoint (int rotation, const cv::Point& p, int newWidth, int newHeight) {
	cv::Point result;

	switch (rotation) {
	case 0:
		result.x= p.x;
		result.y= p.y;
		break;
	case 90:
		result.x= p.y;
		result.y= -p.x + newHeight;
		break;
	case 180:
		result.x= -p.x + newWidth;
		result.y= -p.y + newHeight;
		break;
	case 270:
		result.x= -p.y + newWidth;
		result.y= p.x;
		break;
	}

	return result;
}

static
void drawCorners(CIplImage &image, CvPoint2D32f corners[], int num_corners, CvScalar color, int rotation)
{
	for (int i = 0; i < num_corners; i++) {
		cv::Point p= rotatePoint(
				rotation,
				cvPoint(corners[i].x, corners[i].y),
				image.Width(), image.Height());
		cvCircle(image.ptr(), p, 1, color);
	}
}

static
void drawCross(CIplImage &image, const cv::Point& p,
			   CvScalar color, int thickness= 1, int radius= 2)
{
	CvPoint p1, p2;

	/*
	 * Horizontal line
	 */
	p1.x= p.x - radius;
	p1.y= p.y;
	p2.x= p.x + radius;
	p2.y= p.y;
	cvLine(image.ptr(), p1, p2, color, thickness);

	/*
 	 * Vertical line
	 */
	p1.x= p.x;
	p1.y= p.y - radius;
	p2.x= p.x;
	p2.y= p.y + radius;
	cvLine(image.ptr(), p1, p2, color, thickness);
}

bool VisionPipeline::motionTracker(CIplImage &image, int rotation, float &xVel, float &yVel)
{
	bool updateFeatures = false;
	bool faceDetected= false;

	// Check if face detected and update floatTrackArea as needed
	{
	CvSize frameSize;
	CvRect faceRegion;
	if (m_faceDetection.retrieveDetectionInfo(faceDetected, frameSize, faceRegion) && faceDetected) {
		// update floatTrackArea
		m_floatTrackArea.setReferenceSize(frameSize);
		m_floatTrackArea.set(faceRegion);
		updateFeatures = true;
	}
	else
		// need to update corners?
		// TODO: force corner update when image rotated
		if (m_corner_count< NUM_CORNERS) updateFeatures = true;
	}

	// Submit frame for face detection
	m_faceDetection.submitFrame(m_imgCurr);

	// set current image size
	m_floatTrackArea.setReferenceSize(m_imgCurr.GetSize());

	// Get actual coordinates of floatTrackArea
	CvPoint2D32f trackAreaLocation;
	CvSize2D32f trackAreaSize;
	m_floatTrackArea.get(trackAreaLocation, trackAreaSize);



	if (updateFeatures) {
		// 
		// Set smaller area to extract features to track
		//
		#define SMALL_AREA_RATIO 0.4f

		CvRect featuresTrackArea;
		featuresTrackArea.x = trackAreaLocation.x + 
			trackAreaSize.width * ((1.0f - SMALL_AREA_RATIO) / 2.0f);
		featuresTrackArea.y = trackAreaLocation.y + 
			trackAreaSize.height * ((1.0f - SMALL_AREA_RATIO) / 2.0f);
		featuresTrackArea.width = trackAreaSize.width * SMALL_AREA_RATIO;
		featuresTrackArea.height = trackAreaSize.height * SMALL_AREA_RATIO;

		//
		// Find features to track
		//
		#define QUALITY_LEVEL  0.001   // 0.01
		#define MIN_DISTANTE 2

		m_imgPrev.SetROI(featuresTrackArea);
		m_imgCurr.SetROI(featuresTrackArea);
		m_corner_count = NUM_CORNERS;
		cvGoodFeaturesToTrack(m_imgPrev.ptr(), NULL, NULL, m_corners,
			&m_corner_count, QUALITY_LEVEL, MIN_DISTANTE);
		CvTermCriteria termcrit (CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03);
		cvFindCornerSubPix(m_imgPrev.ptr(), m_corners, m_corner_count,
			cvSize(5, 5), cvSize(-1, -1), termcrit);
		m_imgPrev.ResetROI();
		m_imgCurr.ResetROI();

		//
		// Update features location
		//
		for (int i = 0; i < m_corner_count; i++) {
			m_corners[i].x += featuresTrackArea.x;
			m_corners[i].y += featuresTrackArea.y;
		}

		//LOGV("Features updated\n");
	}

	//
	// Track corners
	//
	CvPoint2D32f new_corners[NUM_CORNERS];
	char status[NUM_CORNERS];
	
	CvTermCriteria termcrit;
	termcrit.type = CV_TERMCRIT_ITER | CV_TERMCRIT_EPS;
	termcrit.max_iter = 14;
	termcrit.epsilon = 0.03;
	
	CvRect ofTrackArea;
	ofTrackArea.x = trackAreaLocation.x;
	ofTrackArea.y = trackAreaLocation.y;
	ofTrackArea.width = trackAreaSize.width;
	ofTrackArea.height = trackAreaSize.height;
	m_imgPrev.SetROI(ofTrackArea);
	m_imgCurr.SetROI(ofTrackArea);

	// Update corners location for the new ROI
	for (int i = 0; i < m_corner_count; i++) {
		m_corners[i].x -= ofTrackArea.x;
		m_corners[i].y -= ofTrackArea.y;
	}
	
	cvCalcOpticalFlowPyrLK(m_imgPrev.ptr(), m_imgCurr.ptr(), NULL,
		NULL, m_corners, new_corners, m_corner_count, cvSize(11, 11), 0, status,
		NULL, termcrit, 0);	
	
	m_imgPrev.ResetROI();
	m_imgCurr.ResetROI();

	// Update corners location
	for (int i = 0; i < m_corner_count; i++) {
		m_corners[i].x += ofTrackArea.x;
		m_corners[i].y += ofTrackArea.y;
		new_corners[i].x += ofTrackArea.x;
		new_corners[i].y += ofTrackArea.y;
	}

	//
	// accumulate motion
	//	
	int valid_corners = 0;
	xVel= 0; yVel= 0;

	for (int i = 0; i< m_corner_count; i++) {
		if (status[i] &&
			m_corners[i].x >= trackAreaLocation.x &&
			m_corners[i].x < trackAreaLocation.x + trackAreaSize.width &&
			m_corners[i].y >= trackAreaLocation.y &&
			m_corners[i].y < trackAreaLocation.y + trackAreaSize.height) {
			xVel += new_corners[i].x - m_corners[i].x;
			yVel += new_corners[i].y - m_corners[i].y;

			// Save new corner location
			m_corners[valid_corners++] = new_corners[i];
		}
	}
	m_corner_count = valid_corners;

	if (valid_corners) {
		xVel = xVel / (float) valid_corners;
		yVel = yVel / (float) valid_corners;
	}
	else {
		xVel = yVel = 0;
	}

	// update tracking area location
	if (m_trackFace) m_floatTrackArea.move(cvPoint2D32f(xVel, yVel));
	
	//
	// Provide feedback
	//

	// TODO: enable this block only in debug mode
	/*
	// draw tracking area
	cv::Mat tmp(image.ptr());
	cv::rectangle(tmp,
			rotatePoint(
					rotation,
					cvPoint(trackAreaLocation.x, trackAreaLocation.y),
					image.Width(), image.Height()),
			rotatePoint(
					rotation,
					cvPoint(trackAreaLocation.x + trackAreaSize.width,
							trackAreaLocation.y + trackAreaSize.height),
					image.Width(), image.Height()),
			cvScalar(255, 0, 0) );

	// draw corners
	drawCorners(image, m_corners, m_corner_count, cvScalar(0, 255, 0), rotation);
	 */

	// draw a cross in the center of the tracking area
	drawCross(image,
			  rotatePoint(
					  rotation,
					  cvPoint(trackAreaLocation.x + trackAreaSize.width/2,
							  trackAreaLocation.y + trackAreaSize.height/2),
					  image.Width(), image.Height()),
			  cvScalar(255, 255, 255), 10, 25);

	return faceDetected;
}

bool VisionPipeline::processImage (CIplImage& image, int flip, int rotation, float& xVel, float& yVel)
{
	bool faceDetected= false;

	try {
		bool bufferReallocation= false;

		// check and allocate temporal buffer
		if (!m_tmpImg.Initialized() ||
				image.Width() != m_tmpImg.Width() ||
				image.Height() != m_tmpImg.Height()) {
			m_tmpImg.Create (image.Width(), image.Height(), IPL_DEPTH_8U, "GRAY");
			bufferReallocation= true;
		}

		// manage physical rotation of the camera
		switch (rotation) {
		case 0:
			bufferReallocation|= allocWorkingSpace(image.Width(), image.Height());
			cvCvtColor(image.ptr(), m_imgCurr.ptr(), CV_BGR2GRAY);
			/*
			    This is the same as:
			    if (flip== VERTICAL) cvFlip(m_imgCurr.ptr(), NULL, 0);
            	else if (flip== HORIZONTAL) cvFlip(m_imgCurr.ptr(), NULL, 1);
            */
			if (flip) cvFlip(m_imgCurr.ptr(), NULL, flip - 1);
			break;
		case 90:
			bufferReallocation|= allocWorkingSpace(image.Height(), image.Width());
			cvCvtColor(image.ptr(), m_tmpImg.ptr(), CV_BGR2GRAY);
			if (flip) cvFlip(m_tmpImg.ptr(), NULL, flip - 1);
			cvTranspose(m_tmpImg.ptr(), m_imgCurr.ptr());
			cvFlip(m_imgCurr.ptr(), NULL, 1);
			break;
		case 180:
			bufferReallocation|= allocWorkingSpace(image.Width(), image.Height());
			cvCvtColor(image.ptr(), m_imgCurr.ptr(), CV_BGR2GRAY);
			if (flip) cvFlip(m_imgCurr.ptr(), NULL, flip - 1);
			cvFlip(m_imgCurr.ptr(), NULL, -1);
			break;
		case 270:
			bufferReallocation|= allocWorkingSpace(image.Height(), image.Width());
			cvCvtColor(image.ptr(), m_tmpImg.ptr(), CV_BGR2GRAY);
			if (flip) cvFlip(m_tmpImg.ptr(), NULL, flip - 1);
			cvTranspose(m_tmpImg.ptr(), m_imgCurr.ptr());
			cvFlip(m_imgCurr.ptr(), NULL, 0);
			break;
		}

		// process frame. skip if buffer reallocated
		if (!bufferReallocation) {
			faceDetected= motionTracker(image, rotation, xVel, yVel);
		}
		else LOGV("Skip frame");

		// Store current image as previous
		m_imgPrev.Swap(&m_imgCurr);
	}
	catch (const std::exception& e) {
		LOGE("Exception: %s\n", e.what());
		exit(1);
	}

	return faceDetected;
}

}

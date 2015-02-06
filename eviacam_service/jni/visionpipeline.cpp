/////////////////////////////////////////////////////////////////////////////
// Name:        visionpipeline.cpp
// Purpose:  
// Author:      Cesar Mauri Loba (cesar at crea-si dot com)
// Modified by: 
// Created:     
// Copyright:   (C) 2008-14 Cesar Mauri Loba - CREA Software Systems
// 
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
/////////////////////////////////////////////////////////////////////////////
#include "visionpipeline.h"
#include "crvimage.h"
#include "timeutil.h"
#include "eviacam.h"

#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/video/tracking.hpp>

#include <math.h>

namespace eviacam {

// Constants
#define DEFAULT_TRACK_AREA_WIDTH_PERCENT 0.50f
#define DEFAULT_TRACK_AREA_HEIGHT_PERCENT 0.30f
#define DEFAULT_TRACK_AREA_X_CENTER_PERCENT 0.5f
#define DEFAULT_TRACK_AREA_Y_CENTER_PERCENT 0.5f

VisionPipeline::VisionPipeline(const char* cascadePath)
: m_faceDetection(cascadePath)
, m_trackFace(true)
, m_corner_count(0)
{
	m_floatTrackArea.SetSize (DEFAULT_TRACK_AREA_WIDTH_PERCENT, DEFAULT_TRACK_AREA_HEIGHT_PERCENT);
	m_floatTrackArea.SetCenter (DEFAULT_TRACK_AREA_X_CENTER_PERCENT, DEFAULT_TRACK_AREA_Y_CENTER_PERCENT);
	memset(m_corners, 0, sizeof(m_corners));
}

VisionPipeline::~VisionPipeline ()
{
}

void VisionPipeline::allocWorkingSpace (CIplImage &image)
{
	bool retval;

	if (!m_imgPrev.Initialized () ||
		image.Width() != m_imgPrev.Width() ||
		image.Height() != m_imgPrev.Height() ) {

		retval= m_imgPrev.Create (image.Width(), image.Height(), 
								  IPL_DEPTH_8U, "GRAY");
		assert (retval);

		retval= m_imgCurr.Create (image.Width(), image.Height(), 
								  IPL_DEPTH_8U, "GRAY");
		assert (retval);
	}
}

static 
void DrawCorners(CIplImage &image, CvPoint2D32f corners[], int num_corners, CvScalar color)
{
	for (int i = 0; i < num_corners; i++)
		cvCircle(image.ptr(), cvPoint(corners[i].x, corners[i].y), 1, color);
}

void VisionPipeline::newTracker(CIplImage &image, float &xVel, float &yVel)
{
	bool updateFeatures = false;

	// Check if face detected and update floatTrackArea as needed
	{
	bool faceDetected= false;
	CvSize frameSize;
	CvRect faceRegion;
	if (m_faceDetection.retrieveDetectionInfo(faceDetected, frameSize, faceRegion) && faceDetected) {
		// Update m_floatTrackArea
		m_floatTrackArea.SetP1Move(0, 0);
		m_floatTrackArea.SetSizeInteger(frameSize, faceRegion.width, faceRegion.height);
		m_floatTrackArea.SetP1MoveInteger(frameSize, faceRegion.x, faceRegion.y);
		updateFeatures = true;
	}
	else
		// Need to update corners?
		if (m_corner_count< NUM_CORNERS) updateFeatures = true;
	}

	// Submit frame for face detection
	m_faceDetection.submitFrame(m_imgCurr);

	// Get actual coordinates of floatTrackArea
	CvPoint2D32f trackAreaLocation;
	CvSize2D32f trackAreaSize;
	{
	CvRect box;
	m_floatTrackArea.GetBoxImg(&image, box);
	trackAreaLocation.x = box.x;
	trackAreaLocation.y = box.y;
	trackAreaSize.width = box.width;
	trackAreaSize.height = box.height;

	// DEBUG: show tracking area
	cv::Mat tmp(image.ptr());
	cv::rectangle(tmp, box, cvScalar(255, 0, 0) );
	}

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
		CvTermCriteria termcrit = { CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03 };
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

		LOGD("Features updated\n");
	}

	// TODO: use flag to tell DEBUG from RELEASE builds
	//if (slog_get_priority() >= SLOG_PRIO_DEBUG)
	//	DrawCorners(image, m_corners, m_corner_count, cvScalar(255, 0, 0));

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
	// Accumulate motion (TODO: remove outliers?)
	//	
	int valid_corners = 0;
	float dx = 0, dy = 0;

	for (int i = 0; i< m_corner_count; i++) {
		if (status[i] &&
			m_corners[i].x >= trackAreaLocation.x &&
			m_corners[i].x < trackAreaLocation.x + trackAreaSize.width &&
			m_corners[i].y >= trackAreaLocation.y &&
			m_corners[i].y < trackAreaLocation.y + trackAreaSize.height) {
			dx += m_corners[i].x - new_corners[i].x;
			dy += m_corners[i].y - new_corners[i].y;

			// Save new corner location
			m_corners[valid_corners++] = new_corners[i];
		}
	}
	m_corner_count = valid_corners;

	if (valid_corners) {
		dx = dx / (float) valid_corners;
		dy = dy / (float) valid_corners;

		xVel = 2.0 * dx;
		yVel = 2.0 * -dy;
	}
	else {
		xVel = yVel = 0;
	}

	//
	// Update tracking area location
	//
	if (m_trackFace) {
		trackAreaLocation.x -= dx;
		trackAreaLocation.y -= dy;
	}
	
	//
	// Update visible tracking area
	//
	m_floatTrackArea.SetSizeImg(&image, trackAreaSize.width, trackAreaSize.height);
	m_floatTrackArea.SetCenterImg(&image,
		trackAreaLocation.x + trackAreaSize.width / 2.0f, 
		trackAreaLocation.y + trackAreaSize.height / 2.0f);

	//
	// Draw corners
	//
	DrawCorners(image, m_corners, m_corner_count, cvScalar(0, 255, 0));
}


bool VisionPipeline::processImage (CIplImage& image, float& xVel, float& yVel)
{
	try {
		allocWorkingSpace(image);

		cvCvtColor(image.ptr(), m_imgCurr.ptr(), CV_BGR2GRAY);

		newTracker(image, xVel, yVel);

		// Store current image as previous
		m_imgPrev.Swap(&m_imgCurr);
	}
	catch (const std::exception& e) {
		LOGE("Exception: %s\n", e.what());
		exit(1);
	}

	return false;
}

}

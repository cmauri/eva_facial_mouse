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

#ifndef VISIONPIPELINE_H
#define VISIONPIPELINE_H

#include "crvimage.h"
#include "normroi2.h"
#include "facedetection.h"

namespace  eviacam {

class VisionPipeline
{
public:
	VisionPipeline (const char* cascadePath);
	virtual ~VisionPipeline();

	/**
	* Entry point to process camera frames
	*
	* @param image reference to an OpenCV image
	* @param flip flip operation to perform before rotation
	*   0: no flip
	*   1: vertical flip (around X-axis)
	*   2: horizontal flip (around Y-axis)
	* @param rotation rotation (clockwise) in degrees that needs to be applied to the image
	*     before processing it so that the subject appears right.
	*     Valid values: 0, 90, 180, 270.
	* @param xVel updated with motion extracted in the X axis
	* @param yVel updated with motion extracted in the X axis
	* @return true if face detected in the last frame (or few frames ago)
	*/
	bool processImage (CIplImage& image, int flip, int rotation, float& xVel, float& yVel);

	bool getTrackFace () const { return m_trackFace; }
	void setTrackFace (bool state) { m_trackFace= state; }

	int getCPUUsage ();
	void setCPUUsage (int value);

private:
	// Face detector
	FaceDetection m_faceDetection;

	// Track area
	bool m_trackFace;
	CIplImage m_imgPrev, m_imgCurr;
	CIplImage m_tmpImg;
	NormROI2 m_floatTrackArea;

	// Corner array
	enum { NUM_CORNERS = 15 };
	CvPoint2D32f m_corners[NUM_CORNERS];
	int m_corner_count;
	
	//
	// Private methods
	//

	// return true if buffers reallocated
	bool allocWorkingSpace (int width, int height);
	bool motionTracker(CIplImage &image, int rotation, float &xVel, float &yVel);
};

}

#endif

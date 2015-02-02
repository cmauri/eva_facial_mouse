/////////////////////////////////////////////////////////////////////////////
// Name:        visionpipeline.h
// Purpose:  
// Author:      Cesar Mauri Loba (cesar at crea-si dot com)
// Modified by: 
// Created:     
// Copyright:   (C) 2008-15 Cesar Mauri Loba - CREA Software Systems
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

#ifndef VISIONPIPELINE_H
#define VISIONPIPELINE_H

#include "crvimage.h"
#include "crvhistogram.h"
#include "crvnormroi.h"
#include "waittime.h"

class CVisionPipeline
{
public:
	enum ECpuUsage {CPU_LOWEST= 0, CPU_LOW, CPU_NORMAL, CPU_HIGH, CPU_HIGHEST};

	// Methods
	CVisionPipeline ();
	~CVisionPipeline();

	bool ProcessImage (CIplImage& image, float& xVel, float& yVel);

	bool GetTrackFace () const { return m_trackFace; }
	void SetTrackFace (bool state) { m_trackFace= state; }

	int GetCpuUsage ();
	void SetCpuUsage (int value);

	bool IsTrackFaceAllowed () { return (m_faceCascade!= NULL); }	

private:
	// Track area
	bool m_trackFace;
	bool m_isRunning;
	CIplImage m_imgThread;
	CIplImage m_imgPrevProc, m_imgCurrProc;
	CIplImage m_imgPrev, m_imgCurr;
	TCrvLookupTable m_prevLut;
	CvHaarClassifierCascade* m_faceCascade;
	CvMemStorage* m_storage;
	int m_threadPeriod;
	CNormROI m_trackArea;

	// Face location detection
	CvRect m_faceLocation;
	volatile bool m_faceDetected;
	volatile int m_faceLocationStatus; // 0 -> not available, 1 -> available

	// Corner array
	enum { NUM_CORNERS = 15 };
	CvPoint2D32f m_corners[NUM_CORNERS];
	int m_corner_count;
	
	// Private methods
	void AllocWorkingSpace (CIplImage &image);
	int PreprocessImage ();
	void ComputeFaceTrackArea (CIplImage &image);
	void SetThreadPeriod (int value);
	void NewTracker(CIplImage &image, float &xVel, float &yVel);
	int Entry();
};

#endif

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

#ifndef NORMROI2_H
#define NORMROI2_H
#include <opencv/cv.h>

namespace eviacam {

/**
 * Stores a ROI (region of interest) in float coordinates and
 * manages conversions when frame size changes
 */

class NormROI2
{
public:
	NormROI2();

	// update reference frame size
	void setReferenceSize (const CvSize& ref);

	// initialize internal state. p1 and size are relative to the reference size
	void set (const CvPoint2D32f& p1, const CvSize2D32f& size);

	// initialize internal state form CvRect region relative to the reference size
	void set (const CvRect& roi);

	// get roi size relative to the reference size
	void get (CvPoint2D32f& p1, CvSize2D32f& size) const;

	// move roi
	void move (const CvPoint2D32f& delta);

private:
	 CvSize m_referenceSize;
	 CvPoint2D32f m_p1;
	 CvSize2D32f m_size;

	 void fit();
};

}

#endif

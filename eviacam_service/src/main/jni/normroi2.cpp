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

#include "normroi2.h"

namespace eviacam {

NormROI2::NormROI2() {
	m_referenceSize.width= 1;
	m_referenceSize.height= 1;
	m_p1.x= 0;
	m_p1.y= 0;
	m_size.width= 1.0f;
	m_size.height= 1.0f;

	fit();
}

void NormROI2::setReferenceSize (const CvSize& ref) {
	if (ref.width== m_referenceSize.width && ref.height== m_referenceSize.height) return;

	assert (ref.height> 0 && ref.width> 0);

	// scale internal point and size
	float mX= (float) ref.width / (float) m_referenceSize.width;
	m_referenceSize.width= ref.width;
	m_p1.x*= mX;
	m_size.width*= mX;

	float mY= (float) ref.height / (float) m_referenceSize.height;
	m_referenceSize.height= ref.height;
	m_p1.y*= mY;
	m_size.height*= mY;

	fit();
}

void NormROI2::set (const CvPoint2D32f& p1, const CvSize2D32f& size) {
	assert(p1.x>= 0);
	assert(p1.y>= 0);
	assert(p1.x< (float) m_referenceSize.width);
	assert(p1.y< (float) m_referenceSize.height);
	assert(size.width>= 0);
	assert(size.height>= 0);
	assert(p1.x + size.width<= (float) m_referenceSize.width);
	assert(p1.y + size.height<= (float) m_referenceSize.height);

	m_p1= p1;
	m_size= size;
}

void NormROI2::set (const CvRect& roi) {
	CvPoint2D32f p1;
	p1.x= (float) roi.x;
	p1.y= (float) roi.y;
	CvSize2D32f size;
	size.width= roi.width;
	size.height= roi.height;

	set (p1, size);
}

void NormROI2::get (CvPoint2D32f& p1, CvSize2D32f& size) const {
	p1= m_p1;
	size= m_size;
}

void NormROI2::move (const CvPoint2D32f& delta) {
	m_p1.x+= delta.x;
	m_p1.y+= delta.y;

	fit();
}

static inline
void fitPoint (float& p, int max_size) {
	if (p< 0)
		p= 0;
	else if (p>= (float) max_size)
		p= (float) (max_size - 1);
}

static inline
void fitSize (float& s, int max_size) {
	if (s< 0)
		s= 0;
	else if (s> (float) max_size)
		s= (float) max_size;
}

static inline
void fitPointWithSize (float& p, float s, int max_size) {
	if (p + s<= (float) max_size) return;

	p= (float) max_size - s;

	assert(p>= 0);
	assert(p + s<= (float) max_size);
}

void NormROI2::fit() {
	fitPoint(m_p1.x, m_referenceSize.width);
	fitPoint(m_p1.y, m_referenceSize.height);

	fitSize (m_size.width, m_referenceSize.width);
	fitSize (m_size.height, m_referenceSize.height);

	fitPointWithSize (m_p1.x, m_size.width, m_referenceSize.width);
	fitPointWithSize (m_p1.y, m_size.height, m_referenceSize.height);
}

}

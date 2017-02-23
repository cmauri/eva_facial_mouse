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

#include <jni.h>
#include <opencv2/core/core.hpp>

#include "eviacam.h"
#include "visionpipeline.h"
#include "crvimage.h"

extern "C" {

eviacam::VisionPipeline* g_visionPipeline= NULL;

JNIEXPORT void JNICALL Java_com_crea_1si_eviacam_common_VisionPipeline_init
	(JNIEnv* env, jobject, jstring jCascadeName)
{
	LOGD ("init called");

	assert (g_visionPipeline== NULL);

	const char* cascadeName= env->GetStringUTFChars(jCascadeName, 0);

	g_visionPipeline= new eviacam::VisionPipeline(cascadeName);

	env->ReleaseStringUTFChars(jCascadeName, cascadeName);
}

JNIEXPORT void JNICALL Java_com_crea_1si_eviacam_common_VisionPipeline_cleanup
	(JNIEnv*, jobject)
{
	LOGD ("cleanup called");

	assert (g_visionPipeline);

	delete g_visionPipeline;

	g_visionPipeline= NULL;
}

JNIEXPORT jboolean JNICALL Java_com_crea_1si_eviacam_common_VisionPipeline_processFrame
	(JNIEnv* env, jobject, jlong addrFrame, jint flip, jint rotation, jobject jPoint)
{
	assert (g_visionPipeline);

	float xVel= 0, yVel= 0;

	IplImage iplimg = *(cv::Mat*) addrFrame;
	CIplImage frame(&iplimg);
	jboolean result= g_visionPipeline->processImage(frame, flip, rotation, xVel, yVel);

	jclass pointClass = env->GetObjectClass(jPoint);
	jfieldID jXVelId = env->GetFieldID(pointClass, "x", "F");
	jfieldID jYVelId = env->GetFieldID(pointClass, "y", "F");
	env->SetFloatField(jPoint, jXVelId, xVel);
	env->SetFloatField(jPoint, jYVelId, yVel);

	return result;
}

} // extern "C"

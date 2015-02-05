#include <jni.h>
#include <opencv2/core/core.hpp>

#include "eviacam.h"
#include "visionpipeline.h"
#include "crvimage.h"

extern "C" {

eviacam::VisionPipeline* g_visionPipeline= NULL;

JNIEXPORT void JNICALL Java_com_crea_1si_eviacam_service_VisionPipeline_init
	(JNIEnv* env, jobject, jstring jCascadeName)
{
	LOGD ("init called");

	assert (g_visionPipeline== NULL);

	const char* cascadeName= env->GetStringUTFChars(jCascadeName, 0);

	g_visionPipeline= new eviacam::VisionPipeline(cascadeName);

	env->ReleaseStringUTFChars(jCascadeName, cascadeName);
}

JNIEXPORT void JNICALL Java_com_crea_1si_eviacam_service_VisionPipeline_finish
	(JNIEnv*, jobject)
{
	LOGD ("finish called");

	assert (g_visionPipeline);

	delete g_visionPipeline;

	g_visionPipeline= NULL;
}

JNIEXPORT void JNICALL Java_com_crea_1si_eviacam_service_VisionPipeline_processFrame
	(JNIEnv* env, jobject, jlong addrFrame, jobject jPoint)
{
	assert (g_visionPipeline);

	float xVel= 0, yVel= 0;

	IplImage iplimg = *(cv::Mat*) addrFrame;
	CIplImage frame(&iplimg);
	g_visionPipeline->processImage(frame, xVel, yVel);

	jclass pointClass = env->GetObjectClass(jPoint);
	jfieldID jXVelId = env->GetFieldID(pointClass, "x", "F");
	jfieldID jYVelId = env->GetFieldID(pointClass, "y", "F");
	env->SetFloatField(jPoint, jXVelId, xVel);
	env->SetFloatField(jPoint, jYVelId, yVel);
}

}

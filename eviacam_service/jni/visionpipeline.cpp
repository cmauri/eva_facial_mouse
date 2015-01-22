#include <jni.h>
#include <android/log.h>

#define LOG_TAG "EVIACAM-native"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT void JNICALL Java_com_crea_1si_eviacam_service_VisionPipeline_ProcessFrame
	(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);

JNIEXPORT void JNICALL Java_com_crea_1si_eviacam_service_VisionPipeline_ProcessFrame
	(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{
	LOGD ("Hello World!");
}

}

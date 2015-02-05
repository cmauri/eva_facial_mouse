LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(OPENCV_ANDROID_SDK)/native/jni/OpenCV.mk

LOCAL_MODULE    := visionpipeline
LOCAL_SRC_FILES := crvimage.cpp \
	timeutil.cpp \
	crvnormroi.cpp \
	facedetection.cpp \
	visionpipeline.cpp \
	visionpipeline_jni.cpp  
LOCAL_LDLIBS    += -lm -llog -landroid
LOCAL_STATIC_LIBRARIES += android_native_app_glue

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/native_app_glue)

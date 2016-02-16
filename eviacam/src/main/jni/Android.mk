LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=off
OPENCV_INSTALL_MODULES:=on

include $(OPENCV_ANDROID_SDK)/native/jni/OpenCV.mk

LOCAL_MODULE    := visionpipeline
LOCAL_SRC_FILES := crvimage.cpp \
	timeutil.cpp \
	normroi2.cpp \
	facedetection.cpp \
	visionpipeline.cpp \
	visionpipeline_jni.cpp  
LOCAL_LDLIBS    += -lm -llog -landroid
LOCAL_STATIC_LIBRARIES += android_native_app_glue

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/native_app_glue)

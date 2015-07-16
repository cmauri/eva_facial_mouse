APP_ABI := armeabi armeabi-v7a mips x86
APP_STL := gnustl_static
APP_CPPFLAGS := -frtti -fexceptions
APP_PLATFORM := android-9
ifeq ($(BUILD_CONFIG),DEBUG)
	APP_OPTIM := debug
else 
	APP_OPTIM := release
endif
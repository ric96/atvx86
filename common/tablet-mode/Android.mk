# Copyright 2012 The Android-x86 Open Source Project

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= tablet-mode.c

LOCAL_C_INCLUDES := $(KERNEL_HEADERS)
LOCAL_SHARED_LIBRARIES := liblog

LOCAL_CFLAGS := -O2 -Wall

ifeq ($(TARGET_ARCH),x86)
LOCAL_CFLAGS += -Ulinux
endif

LOCAL_MODULE := tablet-mode
LOCAL_MODULE_TAGS := optional
#LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)

include $(BUILD_EXECUTABLE)

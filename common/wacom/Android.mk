# Copyright 2011 The Android-x86 Open Source Project

#ifeq ($(BOARD_USES_WACOMINPUT),true)

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= wactablet.c wacserial.c wacusb.c wacom-input.c

LOCAL_C_INCLUDES := $(KERNEL_HEADERS)

LOCAL_CFLAGS := -O2 -Wall -Wno-unused-parameter

ifeq ($(TARGET_ARCH),x86)
LOCAL_CFLAGS += -Ulinux
endif

LOCAL_MODULE := wacom-input
LOCAL_MODULE_TAGS := optional
#LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)

include $(BUILD_EXECUTABLE)

#endif

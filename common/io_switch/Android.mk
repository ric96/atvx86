# Copyright (C) 2011 The Android-x86 Open Source Project

LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := io_switch.c
LOCAL_CFLAGS := -Werror

LOCAL_MODULE := io_switch
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)

include $(BUILD_EXECUTABLE)

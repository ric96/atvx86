#
# Copyright (C) 2015 The Android-x86 Open Source Project
#
# Licensed under the GNU General Public License Version 2 or later.
# You may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.gnu.org/licenses/gpl.html
#

LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libnb
LOCAL_SRC_FILES := src/libnb.cpp
LOCAL_CFLAGS := -Werror -Wall
LOCAL_CPPFLAGS := -std=c++11
LOCAL_SHARED_LIBRARIES := libcutils libdl liblog
LOCAL_MULTILIB := both

include $(BUILD_SHARED_LIBRARY)

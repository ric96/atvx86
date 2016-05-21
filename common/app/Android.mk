#
# Copyright (C) 2011-2015 The Android-x86 Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#

LOCAL_PATH := $(call my-dir)
LOCAL_APPS := $(subst $(LOCAL_PATH)/,,$(wildcard $(LOCAL_PATH)/*$(COMMON_ANDROID_PACKAGE_SUFFIX)))

define include-app
include $$(CLEAR_VARS)

LOCAL_LIBS := $$(shell zipinfo -1 $$(LOCAL_PATH)/$(1) | grep ^lib/ | grep -v /$$$$)

LOCAL_MODULE := $$(basename $(1))
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $$(suffix $(1))
LOCAL_BUILT_MODULE_STEM := package.apk
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_SRC_FILES := $(1)
LOCAL_DEX_PREOPT := false
LOCAL_MODULE_TARGET_ARCH := $$(call get-prebuilt-src-arch,$$(notdir $$(patsubst %/,%,$$(dir $$(LOCAL_LIBS)))))
LOCAL_PREBUILT_JNI_LIBS := $$(addprefix @,$$(filter lib/$$(LOCAL_MODULE_TARGET_ARCH)/%,$$(LOCAL_LIBS)))
#$$(info $$(LOCAL_MODULE) LOCAL_MODULE_TARGET_ARCH=$$(LOCAL_MODULE_TARGET_ARCH))
#$$(info $$(LOCAL_MODULE) LOCAL_PREBUILT_JNI_LIBS=$$(LOCAL_PREBUILT_JNI_LIBS))
include $$(BUILD_PREBUILT)

ALL_DEFAULT_INSTALLED_MODULES += $$(LOCAL_INSTALLED_MODULE)
endef

$(foreach a,$(LOCAL_APPS),$(eval $(call include-app,$(a))))

#
# Copyright (C) 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

PRODUCT_IS_ATV_SDK := true

PRODUCT_PACKAGES := \
    EmulatorSmokeTests \
    LeanbackSampleApp \
    TelephonyProvider \
    audio.primary.goldfish \
    rild

DEVICE_PACKAGE_OVERLAYS := \
    device/google/atv/sdk_overlay \
    development/sdk_overlay

PRODUCT_COPY_FILES := \
    device/generic/goldfish/data/etc/apns-conf.xml:system/etc/apns-conf.xml \
    device/generic/goldfish/camera/media_codecs.xml:system/etc/media_codecs.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_audio.xml:system/etc/media_codecs_google_audio.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_telephony.xml:system/etc/media_codecs_google_telephony.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_video.xml:system/etc/media_codecs_google_video.xml \
    hardware/libhardware_legacy/audio/audio_policy.conf:system/etc/audio_policy.conf

# Put en_US first in the list, so make it default.
PRODUCT_LOCALES := en_US

# Include drawables for various densities.
PRODUCT_AAPT_CONFIG := normal large xlarge tvdpi hdpi xhdpi xxhdpi

# Add TV skins to SDK, in addition to (not replacing) original SDK tree
PRODUCT_SDK_ATREE_FILES := \
    development/build/sdk.atree \
    device/google/atv/sdk/atv_sdk.atree

# Define the host tools and libs that are parts of the SDK.
-include sdk/build/product_sdk.mk
-include development/build/product_sdk.mk

include $(SRC_TARGET_DIR)/product/emulator.mk

$(call inherit-product, device/google/atv/products/atv_base.mk)

# include available languages for TTS in the system image
-include external/svox/pico/lang/PicoLangDeDeInSystem.mk
-include external/svox/pico/lang/PicoLangEnGBInSystem.mk
-include external/svox/pico/lang/PicoLangEnUsInSystem.mk
-include external/svox/pico/lang/PicoLangEsEsInSystem.mk
-include external/svox/pico/lang/PicoLangFrFrInSystem.mk
-include external/svox/pico/lang/PicoLangItItInSystem.mk

# Overrides
PRODUCT_NAME := sdk_atv_armv7
PRODUCT_DEVICE := generic
PRODUCT_BRAND := generic

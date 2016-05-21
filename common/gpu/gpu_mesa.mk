#
# Copyright (C) 2011 The Android-x86 Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#

PRODUCT_PACKAGES := \
    hwcomposer.drm  \
    gralloc.drm     \
    libGLES_mesa

PRODUCT_PROPERTY_OVERRIDES := \
    ro.opengles.version = 196608

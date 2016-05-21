/*
 * Copyright (C) 2015 The Android-x86 Open Source Project
 *
 * by Chih-Wei Huang <cwhuang@linux.org.tw>
 *
 * Licensed under the GNU General Public License Version 2 or later.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/gpl.html
 *
 */

#define LOG_TAG "libnb"

#include <dlfcn.h>
#include <cutils/log.h>
#include <cutils/properties.h>
#include "nativebridge/native_bridge.h"

namespace android {

static void *native_handle = nullptr;

static NativeBridgeCallbacks *get_callbacks()
{
    static NativeBridgeCallbacks *callbacks = nullptr;

    if (!callbacks) {
        const char *libnb = "/system/"
#ifdef __LP64__
                "lib64/arm64/"
#else
                "lib/arm/"
#endif
                "libhoudini.so";
        if (!native_handle) {
            native_handle = dlopen(libnb, RTLD_LAZY);
            if (!native_handle) {
                ALOGE("Unable to open %s", libnb);
                return nullptr;
            }
        }
        callbacks = reinterpret_cast<NativeBridgeCallbacks *>(dlsym(native_handle, "NativeBridgeItf"));
    }
    return callbacks;
}

// NativeBridgeCallbacks implementations
static bool native_bridge2_initialize(const NativeBridgeRuntimeCallbacks *art_cbs,
                                      const char *app_code_cache_dir,
                                      const char *isa)
{
    ALOGV("enter native_bridge2_initialize %s %s", app_code_cache_dir, isa);
    if (property_get_bool("persist.sys.nativebridge", 0)) {
        if (NativeBridgeCallbacks *cb = get_callbacks()) {
            return cb->initialize(art_cbs, app_code_cache_dir, isa);
        }
    } else {
        ALOGW("Native bridge is disabled");
    }
    return false;
}

static void *native_bridge2_loadLibrary(const char *libpath, int flag)
{
    ALOGV("enter native_bridge2_loadLibrary %s", libpath);
    NativeBridgeCallbacks *cb = get_callbacks();
    return cb ? cb->loadLibrary(libpath, flag) : nullptr;
}

static void *native_bridge2_getTrampoline(void *handle, const char *name,
                                          const char* shorty, uint32_t len)
{
    ALOGV("enter native_bridge2_getTrampoline %s", name);
    NativeBridgeCallbacks *cb = get_callbacks();
    return cb ? cb->getTrampoline(handle, name, shorty, len) : nullptr;
}

static bool native_bridge2_isSupported(const char *libpath)
{
    ALOGV("enter native_bridge2_isSupported %s", libpath);
    NativeBridgeCallbacks *cb = get_callbacks();
    return cb ? cb->isSupported(libpath) : false;
}

static const struct NativeBridgeRuntimeValues *native_bridge2_getAppEnv(const char *abi)
{
    ALOGV("enter native_bridge2_getAppEnv %s", abi);
    NativeBridgeCallbacks *cb = get_callbacks();
    return cb ? cb->getAppEnv(abi) : nullptr;
}

static bool native_bridge2_is_compatible_compatible_with(uint32_t version)
{
    // For testing, allow 1 and 2, but disallow 3+.
    return version <= 2;
}

static NativeBridgeSignalHandlerFn native_bridge2_get_signal_handler(int signal)
{
    ALOGV("enter native_bridge2_getAppEnv %d", signal);
    NativeBridgeCallbacks *cb = get_callbacks();
    return cb ? cb->getSignalHandler(signal) : nullptr;
}

static void __attribute__ ((destructor)) on_dlclose()
{
    if (native_handle) {
        dlclose(native_handle);
        native_handle = nullptr;
    }
}

extern "C" {

NativeBridgeCallbacks NativeBridgeItf = {
    version: 2,
    initialize: &native_bridge2_initialize,
    loadLibrary: &native_bridge2_loadLibrary,
    getTrampoline: &native_bridge2_getTrampoline,
    isSupported: &native_bridge2_isSupported,
    getAppEnv: &native_bridge2_getAppEnv,
    isCompatibleWith: &native_bridge2_is_compatible_compatible_with,
    getSignalHandler: &native_bridge2_get_signal_handler,
};

} // extern "C"
} // namespace android

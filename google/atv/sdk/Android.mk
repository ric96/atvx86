ifeq ($(PRODUCT_IS_ATV_SDK),true)
# Refer to development/build/Android.mk
source_properties_file := \
    $(HOST_OUT)/device/google/atv/sdk/images_$(TARGET_CPU_ABI)_source.properties

ALL_SDK_FILES += $(source_properties_file)

$(source_properties_file): $(TOPDIR)device/google/atv/sdk/images_$(TARGET_CPU_ABI)_source.prop_template
	@echo Generate $@
	$(hide) mkdir -p $(dir $@)
	$(hide) sed -e 's/$${PLATFORM_VERSION}/$(PLATFORM_VERSION)/' \
		 -e 's/$${PLATFORM_SDK_VERSION}/$(PLATFORM_SDK_VERSION)/' \
		 -e 's/$${PLATFORM_VERSION_CODENAME}/$(subst REL,,$(PLATFORM_VERSION_CODENAME))/' \
		 $< > $@ && sed -i -e '/^AndroidVersion.CodeName=\s*$$/d' $@

source_properties_file :=
endif # ATV SDK build

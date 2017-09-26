LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
        $(call all-logtags-files-under, src)

LOCAL_MODULE := CNSettings 

include $(BUILD_STATIC_JAVA_LIBRARY)

# Build the Settings APK
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle core-oj telephony-common ims-common
LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v13 \
    android-support-v7-recyclerview \
    android-support-v7-preference \
    android-support-v7-appcompat \
    android-support-v14-preference \
    android-support-design \
    jsr305 \
    settings-logtags \
    asus-common-res \
    CN_libGooglePlayServicesRev28 \
    asus-common-ui \
    cnasus-common-res
LOCAL_STATIC_JAVA_LIBRARIES += CN_DUTUtil
LOCAL_STATIC_JAVA_LIBRARIES += CN_zxing-core

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)

LOCAL_SRC_FILES += \
        src/com/android/settings/IDeviceAdmin.aidl \
        src/com/asus/splendidcommandagent/ISplendidCommandAgentService.aidl

ifneq (,$(filter CN CUCC CTA CMCC IQY, $(TARGET_SKU)))
LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/CN_Overlay_res/res \
    packages/sharelibs/CNAsusRes/res \
    packages/sharelibs/AsusRes/res \
    packages/sharelibs/AsusUi/res \
    packages/sharelibs/AsusSettingsResources/CN_Overlay_res \
    packages/sharelibs/AsusSettingsResources/Overlay_res \
    packages/sharelibs/AsusSettingsResources/Asus_res \
    packages/sharelibs/AospLocSettingsResources/res \
    $(LOCAL_PATH)/res \
    frameworks/support/v7/preference/res \
    frameworks/support/v14/preference/res \
    frameworks/support/v7/appcompat/res \
    frameworks/support/v7/recyclerview/res \
    frameworks/support/design/res
else
LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/CN_Overlay_res/res \
    packages/sharelibs/CNAsusRes/res \
    packages/sharelibs/AsusRes/res \
    packages/sharelibs/AsusUi/res \
    packages/sharelibs/AsusSettingsResources/Overlay_res \
    packages/sharelibs/AsusSettingsResources/Asus_res \
    packages/sharelibs/AospLocSettingsResources/res \
    $(LOCAL_PATH)/res \
    frameworks/support/v7/preference/res \
    frameworks/support/v14/preference/res \
    frameworks/support/v7/appcompat/res \
    frameworks/support/v7/recyclerview/res \
    frameworks/support/design/res
endif

LOCAL_PACKAGE_NAME := CNSettings
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

ifneq (,$(filter CN CUCC CTA CMCC IQY, $(TARGET_SKU)))
    ADDITIONAL_BUILD_PROPERTIES += ro.asus.cnsettings=1
endif

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_FLAGS := -include packages/sharelibs/AsusRes/proguard.flags \
    -include packages/sharelibs/AsusUi/proguard.flags

LOCAL_OVERRIDES_PACKAGES := Settings PhotoTable AsusSettings

LOCAL_DEX_PREOPT := false

LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --extra-packages android.support.v7.preference:android.support.v14.preference:android.support.v17.preference:android.support.v7.appcompat:android.support.v7.recyclerview:com.asus.commonres:com.asus.commonui:com.asus.cncommonres:android.support.design

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
    LOCAL_DX_FLAGS := --multi-dex
    LOCAL_JACK_FLAGS := --multi-dex native
endif

include frameworks/opt/setupwizard/library/common-full-support.mk
include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
	CN_libGooglePlayServicesRev28:libs/google-play-services.jar \
	CN_zxing-core:libs/zxing-core.jar \
	CN_DUTUtil:libs/DUTUtil.jar
include $(BUILD_MULTI_PREBUILT)
#include $(LOCAL_PATH)/jni/Android.mk
#include packages/apps/AsusSettings/jni/Android.mk
# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif

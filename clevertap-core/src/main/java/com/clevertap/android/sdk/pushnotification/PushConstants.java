package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface PushConstants {

    @StringDef({FCM_DELIVERY_TYPE, XIAOMI_DELIVERY_TYPE, HMS_DELIVERY_TYPE, BAIDU_DELIVERY_TYPE, ADM_DELIVERY_TYPE})
    @Retention(RetentionPolicy.SOURCE)
    @interface DeliveryType {

    }

    @StringDef({CT_FIREBASE_PROVIDER_CLASS, CT_XIAOMI_PROVIDER_CLASS, CT_BAIDU_PROVIDER_CLASS,
            CT_HUAWEI_PROVIDER_CLASS, CT_ADM_PROVIDER_CLASS})
    @Retention(RetentionPolicy.SOURCE)
    @interface CTPushProviderClass {

    }

    @StringDef({FIREBASE_SDK_CLASS, XIAOMI_SDK_CLASS, BAIDU_SDK_CLASS, HUAWEI_SDK_CLASS, ADM_SDK_CLASS})
    @Retention(RetentionPolicy.SOURCE)
    @interface PushMessagingClass {

    }

    @StringDef({FCM_PROPERTY_REG_ID, HPS_PROPERTY_REG_ID, XPS_PROPERTY_REG_ID, BPS_PROPERTY_REG_ID,
            ADM_PROPERTY_REG_ID})
    @Retention(RetentionPolicy.SOURCE)
    @interface RegKeyType {

    }

    @IntDef({ANDROID_PLATFORM, AMAZON_PLATFORM})
    @Retention(RetentionPolicy.SOURCE)
    @interface Platform {

    }

    @IntDef({ALL_DEVICES, XIAOMI_MIUI_DEVICES, NO_DEVICES})
    @Retention(RetentionPolicy.SOURCE)
    @interface XiaomiPush {

    }

    enum PushType {
        FCM(FCM_DELIVERY_TYPE, FCM_PROPERTY_REG_ID, CT_FIREBASE_PROVIDER_CLASS, FIREBASE_SDK_CLASS, ALL_DEVICES),
        XPS(XIAOMI_DELIVERY_TYPE, XPS_PROPERTY_REG_ID, CT_XIAOMI_PROVIDER_CLASS, XIAOMI_SDK_CLASS, ALL_DEVICES),
        HPS(HMS_DELIVERY_TYPE, HPS_PROPERTY_REG_ID, CT_HUAWEI_PROVIDER_CLASS, HUAWEI_SDK_CLASS, ALL_DEVICES),
        BPS(BAIDU_DELIVERY_TYPE, BPS_PROPERTY_REG_ID, CT_BAIDU_PROVIDER_CLASS, BAIDU_SDK_CLASS, ALL_DEVICES),
        ADM(ADM_DELIVERY_TYPE, ADM_PROPERTY_REG_ID, CT_ADM_PROVIDER_CLASS, ADM_SDK_CLASS, ALL_DEVICES);

        private final String ctProviderClassName;

        private final String messagingSDKClassName;

        private final String tokenPrefKey;

        private final String type;

        private @XiaomiPush
        int runningDevices;

        PushType(@DeliveryType String type, @RegKeyType String prefKey, @CTPushProviderClass String className,
                @PushMessagingClass String messagingSDKClassName, @XiaomiPush int runningDevices) {
            this.type = type;
            this.tokenPrefKey = prefKey;
            this.ctProviderClassName = className;
            this.messagingSDKClassName = messagingSDKClassName;
            this.runningDevices = runningDevices;
        }

        public String getCtProviderClassName() {
            return ctProviderClassName;
        }

        public String getMessagingSDKClassName() {
            return messagingSDKClassName;
        }

        public @RegKeyType
        String getTokenPrefKey() {
            return tokenPrefKey;
        }

        public String getType() {
            return type;
        }

        public void setRunningDevices(@XiaomiPush int runningDevices) {
            this.runningDevices = runningDevices;
        }

        public @XiaomiPush int getRunningDevices() {
            return runningDevices;
        }

        @NonNull
        @Override
        public @DeliveryType
        String toString() {
            return " [PushType:" + name() + "] ";
        }

        static {

        }
    }

    String LOG_TAG = "PushProvider";

    String FCM_LOG_TAG = PushType.FCM.toString();

    @NonNull
    String FCM_DELIVERY_TYPE = "fcm";
    @NonNull
    String BAIDU_DELIVERY_TYPE = "bps";
    @NonNull
    String HMS_DELIVERY_TYPE = "hps";
    @NonNull
    String XIAOMI_DELIVERY_TYPE = "xps";
    @NonNull
    String ADM_DELIVERY_TYPE = "adm";
    String CT_FIREBASE_PROVIDER_CLASS = "com.clevertap.android.sdk.pushnotification.fcm.FcmPushProvider";
    String CT_XIAOMI_PROVIDER_CLASS = "com.clevertap.android.xps.XiaomiPushProvider";
    String CT_BAIDU_PROVIDER_CLASS = "com.clevertap.android.bps.BaiduPushProvider";
    String CT_HUAWEI_PROVIDER_CLASS = "com.clevertap.android.hms.HmsPushProvider";
    String CT_ADM_PROVIDER_CLASS = "com.clevertap.android.adm.AmazonPushProvider";
    String FIREBASE_SDK_CLASS = "com.google.firebase.messaging.FirebaseMessagingService";
    String XIAOMI_SDK_CLASS = "com.xiaomi.mipush.sdk.MiPushClient";
    String BAIDU_SDK_CLASS = "com.baidu.android.pushservice.PushMessageReceiver";
    String HUAWEI_SDK_CLASS = "com.huawei.hms.push.HmsMessageService";
    String ADM_SDK_CLASS = "com.amazon.device.messaging.ADM";
    String FCM_PROPERTY_REG_ID = "fcm_token";
    String XPS_PROPERTY_REG_ID = "xps_token";
    String BPS_PROPERTY_REG_ID = "bps_token";
    String HPS_PROPERTY_REG_ID = "hps_token";
    String ADM_PROPERTY_REG_ID = "adm_token";
    /**
     * Android platform type. Only GCM transport will be allowed.
     */
    int ANDROID_PLATFORM = 1;
    /**
     * Amazon platform type. Only ADM transport will be allowed.
     */
    int AMAZON_PLATFORM = 2;

    /**
     * Turn on Xiaomi Push on all devices
     */
    int ALL_DEVICES = 1;

    /**
     * Turn on Xiaomi Push on Xiaomi devices running MIUI OS
     */
    int XIAOMI_MIUI_DEVICES = 2;

    /**
     * Turn off Xiaomi Push on all devices
     */
    int NO_DEVICES = 3;
}
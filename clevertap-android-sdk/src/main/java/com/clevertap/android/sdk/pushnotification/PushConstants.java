package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public interface PushConstants {

    String LOG_TAG = "PushProvider";

    @StringDef({FCM_DELIVERY_TYPE, XIAOMI_DELIVERY_TYPE, HMS_DELIVERY_TYPE, BAIDU_DELIVERY_TYPE, ADM_DELIVERY_TYPE})
    @Retention(RetentionPolicy.SOURCE)
    @interface DeliveryType {
    }

    @NonNull
    String FCM_DELIVERY_TYPE = "fcm";

    @NonNull
    String BAIDU_DELIVERY_TYPE = "bps";

    @NonNull
    String HMS_DELIVERY_TYPE = "hms";

    @NonNull
    String XIAOMI_DELIVERY_TYPE = "xps";

    @NonNull
    String ADM_DELIVERY_TYPE = "adm";

    @StringDef({FIREBASE_CLASS_NAME, XIAOMI_CLASS_NAME, BAIDU_CLASS_NAME, HUAWEI_CLASS_NAME, ADM_CLASS_NAME})
    @Retention(RetentionPolicy.SOURCE)
    @interface DeliveryClassName {
    }

    String FIREBASE_CLASS_NAME = "com.clevertap.android.sdk.pushnotification.fcm.FcmPushProvider";
    String XIAOMI_CLASS_NAME = "com.clevertap.android.xps.XiaomiPushProvider";
    String BAIDU_CLASS_NAME = "com.clevertap.android.bps.BaiduPushProvider";
    String HUAWEI_CLASS_NAME = "com.clevertap.android.hms.HmsPushProvider";
    String ADM_CLASS_NAME = "com.clevertap.android.adm.AmazonPushProvider";

    String FCM_PROPERTY_REG_ID = "fcm_token";
    String XPS_PROPERTY_REG_ID = "xps_token";
    String BPS_PROPERTY_REG_ID = "bps_token";
    String HPS_PROPERTY_REG_ID = "hps_token";
    String ADM_PROPERTY_REG_ID = "adm_token";

    @StringDef({FCM_PROPERTY_REG_ID, HPS_PROPERTY_REG_ID, XPS_PROPERTY_REG_ID, BPS_PROPERTY_REG_ID, ADM_PROPERTY_REG_ID})
    @Retention(RetentionPolicy.SOURCE)
    @interface RegKeyType {
    }

    enum PushType {
        FCM(FCM_DELIVERY_TYPE, FCM_PROPERTY_REG_ID, FIREBASE_CLASS_NAME),
        XPS(XIAOMI_DELIVERY_TYPE, XPS_PROPERTY_REG_ID, XIAOMI_CLASS_NAME),
        HPS(HMS_DELIVERY_TYPE, HPS_PROPERTY_REG_ID, HUAWEI_CLASS_NAME),
        BPS(BAIDU_DELIVERY_TYPE, BPS_PROPERTY_REG_ID, BAIDU_CLASS_NAME),
        ADM(ADM_DELIVERY_TYPE, ADM_PROPERTY_REG_ID, ADM_CLASS_NAME);

        private static final ArrayList<String> ALL;

        public String getType() {
            return type;
        }

        private final String type;

        public String getClassName() {
            return className;
        }

        private final String className;

        public @RegKeyType
        String getTokenPrefKey() {
            return tokenPrefKey;
        }

        private final String tokenPrefKey;

        PushType(@DeliveryType String type, @RegKeyType String prefKey, @DeliveryClassName String className) {
            this.type = type;
            this.tokenPrefKey = prefKey;
            this.className = className;
        }

        static {
            ALL = new ArrayList<>();
            for (PushType pushType : PushType.values()) {
                ALL.add(pushType.name());
            }
        }

        public static ArrayList<String> getAll() {
            return ALL;
        }

        public static PushType[] getPushTypes(ArrayList<String> types) {
            PushType[] pushTypes = new PushType[0];
            if (types != null && !types.isEmpty()) {
                pushTypes = new PushType[types.size()];
                for (int i = 0; i < types.size(); i++) {
                    pushTypes[i] = PushType.valueOf(types.get(i));
                }
            }
            return pushTypes;
        }

        @NonNull
        @Override
        public @DeliveryType
        String toString() {
            return " [PushType:" + name() + "] ";
        }
    }

    @IntDef({ANDROID_PLATFORM, AMAZON_PLATFORM})
    @Retention(RetentionPolicy.SOURCE)
    @interface Platform {
    }

    /**
     * Android platform type. Only GCM transport will be allowed.
     */
    int ANDROID_PLATFORM = 1;

    /**
     * Amazon platform type. Only ADM transport will be allowed.
     */
    int AMAZON_PLATFORM = 2;
}
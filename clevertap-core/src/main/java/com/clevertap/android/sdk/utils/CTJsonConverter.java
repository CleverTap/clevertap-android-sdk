package com.clevertap.android.sdk.utils;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.db.DBAdapter;
import com.clevertap.android.sdk.inapp.CTInAppNotification;
import com.clevertap.android.sdk.inbox.CTInboxMessage;
import com.clevertap.android.sdk.validation.ValidationResult;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY)
public class CTJsonConverter {

    public static JSONObject toJsonObject(String json, Logger logger, String accountId) {
        JSONObject cache = null;
        if (json != null) {
            try {
                cache = new JSONObject(json);
            } catch (Throwable t) {
                // no-op
                logger.verbose(accountId, "Error reading guid cache: " + t.toString());
            }
        }

        return (cache != null) ? cache : new JSONObject();
    }

    public static JSONObject displayUnitFromExtras(Bundle extras) throws JSONException {
        JSONObject r = new JSONObject();

        String pushJsonPayload = extras.getString(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY);
        Logger.v("Received Display Unit via push payload: " + pushJsonPayload);
        JSONArray displayUnits = new JSONArray();
        r.put(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY, displayUnits);
        JSONObject testPushObject = new JSONObject(pushJsonPayload);
        displayUnits.put(testPushObject);

        return r;
    }

    public static JSONObject from(DeviceInfo deviceInfo, Location locationFromUser, boolean enableNetworkInfoReporting
            , boolean deviceIsMultiUser) throws JSONException {

        final JSONObject evtData = new JSONObject();
        evtData.put("Build", deviceInfo.getBuild() + "");
        evtData.put("Version", deviceInfo.getVersionName());
        evtData.put("OS Version", deviceInfo.getOsVersion());
        evtData.put("SDK Version", deviceInfo.getSdkVersion());

        if (locationFromUser != null) {
            evtData.put("Latitude", locationFromUser.getLatitude());
            evtData.put("Longitude", locationFromUser.getLongitude());
        }

        // send up googleAdID
        if (deviceInfo.getGoogleAdID() != null) {
            String baseAdIDKey = "GoogleAdID";
            String adIDKey = deviceIsMultiUser ? Constants.MULTI_USER_PREFIX + baseAdIDKey : baseAdIDKey;
            evtData.put(adIDKey, deviceInfo.getGoogleAdID());
            evtData.put("GoogleAdIDLimit", deviceInfo.isLimitAdTrackingEnabled());
        }

        try {
            // Device data
            evtData.put("Make", deviceInfo.getManufacturer());
            evtData.put("Model", deviceInfo.getModel());
            evtData.put("Carrier", deviceInfo.getCarrier());
            evtData.put("useIP", enableNetworkInfoReporting);
            evtData.put("OS", deviceInfo.getOsName());
            evtData.put("wdt", deviceInfo.getWidth());
            evtData.put("hgt", deviceInfo.getHeight());
            evtData.put("dpi", deviceInfo.getDPI());
            evtData.put("dt", DeviceInfo.getDeviceType(deviceInfo.getContext()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                evtData.put("abckt", deviceInfo.getAppBucket());
            }
            if (deviceInfo.getLibrary() != null) {
                evtData.put("lib", deviceInfo.getLibrary());
            }
            String fcmSenderId = ManifestInfo.getInstance(deviceInfo.getContext()).getFCMSenderId();
            if (!TextUtils.isEmpty(fcmSenderId)) {//only for 4.3.0 for tracking custom sender ID users.
                evtData.put("fcmsid", true);
            }

            String cc = deviceInfo.getCountryCode();
            if (cc != null && !cc.equals("")) {
                evtData.put("cc", cc);
            }

            if (enableNetworkInfoReporting) {
                final Boolean isWifi = deviceInfo.isWifiConnected();
                if (isWifi != null) {
                    evtData.put("wifi", isWifi);
                }

                final Boolean isBluetoothEnabled = deviceInfo.isBluetoothEnabled();
                if (isBluetoothEnabled != null) {
                    evtData.put("BluetoothEnabled", isBluetoothEnabled);
                }

                final String bluetoothVersion = deviceInfo.getBluetoothVersion();
                if (bluetoothVersion != null) {
                    evtData.put("BluetoothVersion", bluetoothVersion);
                }

                final String radio = deviceInfo.getNetworkType();
                if (radio != null) {
                    evtData.put("Radio", radio);
                }
            }

        } catch (Throwable t) {
            // Ignore
        }

        return evtData;
    }

    //Validation
    public static JSONObject getErrorObject(ValidationResult vr) {
        JSONObject error = new JSONObject();
        try {
            error.put("c", vr.getErrorCode());
            error.put("d", vr.getErrorDesc());
        } catch (JSONException e) {
            // Won't reach here
        }
        return error;
    }

    public static JSONArray getRenderedTargetList(DBAdapter dbAdapter) {
        String[] pushIds = dbAdapter.fetchPushNotificationIds();
        JSONArray renderedTargets = new JSONArray();
        for (String pushId : pushIds) {
            Logger.v("RTL IDs -" + pushId);
            renderedTargets.put(pushId);
        }
        return renderedTargets;
    }

    public static JSONObject getWzrkFields(Bundle root) throws JSONException {
        final JSONObject fields = new JSONObject();
        for (String s : root.keySet()) {
            final Object o = root.get(s);
            if (o instanceof Bundle) {
                final JSONObject wzrkFields = getWzrkFields((Bundle) o);
                final Iterator<String> keys = wzrkFields.keys();
                while (keys.hasNext()) {
                    final String k = keys.next();
                    fields.put(k, wzrkFields.get(k));
                }
            } else if (s.startsWith(Constants.WZRK_PREFIX)) {
                fields.put(s, root.get(s));
            }
        }

        return fields;
    }

    public static JSONObject getWzrkFields(CTInAppNotification root) throws JSONException {
        final JSONObject fields = new JSONObject();
        JSONObject jsonObject = root.getJsonDescription();
        Iterator<String> iterator = jsonObject.keys();

        while (iterator.hasNext()) {
            String keyName = iterator.next();
            if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                fields.put(keyName, jsonObject.get(keyName));
            }
        }

        return fields;
    }

    public static JSONObject getWzrkFields(CTInboxMessage root) {
        return root.getWzrkParams();
    }

    public static <T> Object[] toArray(@NonNull JSONArray jsonArray) {
        Object[] array = new Object[jsonArray.length()];
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                array[i] = jsonArray.get(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return array;
    }

    public static JSONArray toJsonArray(@NonNull List<?> list) {
        JSONArray array = new JSONArray();
        for (Object item : list) {
            if (item != null) {
                array.put(item);
            }
        }
        return array;
    }

    public static String toJsonString(Object value) {
        String val = null;

        try {
            val = value.toString();
        } catch (Exception e) {
            // no-op
        }

        return val;
    }

    public static ArrayList<?> toList(@NonNull JSONArray array) {
        ArrayList<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                list.add(array.get(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

}

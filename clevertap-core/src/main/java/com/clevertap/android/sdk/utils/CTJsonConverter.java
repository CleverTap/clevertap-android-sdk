package com.clevertap.android.sdk.utils;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.DeviceState;
import com.clevertap.android.sdk.ILogger;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.inapp.CTInAppNotification;
import com.clevertap.android.sdk.inbox.CTInboxMessage;
import com.clevertap.android.sdk.validation.ValidationResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@RestrictTo(Scope.LIBRARY)
public class CTJsonConverter {
    private static final String TAG = "CTJsonConverter";

    @NonNull
    public static JSONObject toJsonObject(String json, ILogger logger, String logtag) {
        JSONObject cache = null;
        if (json != null) {
            try {
                cache = new JSONObject(json);
            } catch (Throwable t) {
                // no-op
                logger.verbose(logtag, "Error reading guid cache: ", t);
            }
        }

        return (cache != null) ? cache : new JSONObject();
    }

    public static JSONObject displayUnitFromExtras(Bundle extras) throws JSONException {
        JSONObject r = new JSONObject();

        String pushJsonPayload = extras.getString(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY);
        Logger.v(TAG, "Received Display Unit via push payload: " + pushJsonPayload);
        JSONArray displayUnits = new JSONArray();
        r.put(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY, displayUnits);
        JSONObject testPushObject = new JSONObject(pushJsonPayload);
        displayUnits.put(testPushObject);

        return r;
    }

    // Keys from() writes itself. A shared module version with one of these names would replace the real value
    // in af for every instance, so CoreMetaData rejects them. Keep in sync with from().
    public static final Set<String> RESERVED_AF_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "Build", "Version", "OS Version", "SDK Version", "Latitude", "Longitude",
            "GoogleAdID", Constants.MULTI_USER_PREFIX + "GoogleAdID", "GoogleAdIDLimit",
            "Make", "Model", "Carrier", "useIP", "OS", "wdt", "hgt", "dpi", "dt", "locale", "abckt", "lib",
            "proxyDomain", "spikyProxyDomain", "sslpin", "fcmsid", "cc",
            "wifi", "BluetoothEnabled", "BluetoothVersion", "Radio", "LIAMC",
            "lowPower", "thermal", "darkMode", "textSize", "reduceMotion", "pushStatus", "batt", "chg",
            "ram", "cpu", "boldText", "instSrc", "exitRsn", "exitStatus", "exitImp",
            "anrType", "anrTimeout", "anrUser", "metered", "lowData")));

    // Keys from() writes that can't change while the app runs. The request header sends them until one
    // request has delivered them, see QueueHeaderBuilder. Keep in sync with from().
    public static final List<String> LAUNCH_AF_KEYS = Collections.unmodifiableList(Arrays.asList(
            "ram", "cpu", "instSrc", "exitRsn", "exitStatus", "exitImp", "anrType", "anrTimeout", "anrUser"));

    public static JSONObject from(DeviceInfo deviceInfo, CoreMetaData coreMetaData, boolean enableNetworkInfoReporting
            , boolean deviceIsMultiUser) throws JSONException {

        final JSONObject evtData = new JSONObject();
        Location locationFromUser = coreMetaData.getLocationFromUser();

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
            evtData.put("locale", deviceInfo.getLocale());

            // Same keys as iOS. putOpt leaves out values that can't be read.
            // One snapshot, so a display update between two reads can't mix old and new values in one payload.
            final DeviceState deviceState = deviceInfo.getDeviceState();
            evtData.putOpt("lowPower", deviceState.getLowPower());
            evtData.putOpt("thermal", deviceState.getThermal());
            evtData.putOpt("darkMode", deviceState.getDarkMode());
            evtData.putOpt("textSize", deviceState.getTextSize());
            evtData.putOpt("reduceMotion", deviceState.getReduceMotion());
            evtData.putOpt("pushStatus", deviceState.getPushStatus());
            evtData.putOpt("batt", deviceState.getBatt());
            evtData.putOpt("chg", deviceState.getChg());
            evtData.putOpt("ram", deviceInfo.getRamTier());
            evtData.putOpt("cpu", deviceInfo.getCpuCores());
            evtData.putOpt("boldText", deviceState.getBoldText());
            evtData.putOpt("instSrc", deviceInfo.getInstallSource());
            evtData.putOpt("exitRsn", deviceInfo.getLastExitReason());
            evtData.putOpt("exitStatus", deviceInfo.getLastExitStatus());
            evtData.putOpt("exitImp", deviceInfo.getLastExitImportance());
            evtData.putOpt("anrType", deviceInfo.getLastAnrType());
            evtData.putOpt("anrTimeout", deviceInfo.getLastAnrTimeoutMs());
            evtData.putOpt("anrUser", deviceInfo.isLastAnrUserPerceptible());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                evtData.put("abckt", deviceInfo.getAppBucket());
            }
            if (deviceInfo.getLibrary() != null) {
                evtData.put("lib", deviceInfo.getLibrary());
            }

            String proxyDomain = ManifestInfo.getInstance(deviceInfo.getContext()).getProxyDomain();
            if (!TextUtils.isEmpty(proxyDomain)) {
                evtData.put("proxyDomain", proxyDomain);
            }

            String spikyProxyDomain = ManifestInfo.getInstance(deviceInfo.getContext()).getSpikeyProxyDomain();
            if (!TextUtils.isEmpty(spikyProxyDomain)) {
                evtData.put("spikyProxyDomain", spikyProxyDomain);
            }

            boolean sslPinning = ManifestInfo.getInstance(deviceInfo.getContext()).isSSLPinningEnabled();
            if (sslPinning) {
                evtData.put("sslpin", true);
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
                final DeviceInfo.LiveNetworkInfo networkInfo = deviceInfo.getLiveNetworkInfo();
                evtData.putOpt("wifi", networkInfo.wifi);
                evtData.putOpt("BluetoothEnabled", networkInfo.bluetoothEnabled);
                evtData.putOpt("Radio", networkInfo.radio);
                evtData.putOpt("metered", networkInfo.metered);
                evtData.putOpt("lowData", networkInfo.lowData);

                final String bluetoothVersion = deviceInfo.getBluetoothVersion();
                if (bluetoothVersion != null) {
                    evtData.put("BluetoothVersion", bluetoothVersion);
                }
            }
            //Adds an extra field to send local inApp count in queueData.
            evtData.put("LIAMC", deviceInfo.getLocalInAppCount());

            // add custom sdk versions in "af" key of header.
            Map<String, Integer> allCustomSdkVersions = coreMetaData.getAllCustomSdkVersions();
            for (Entry<String, Integer> entries : allCustomSdkVersions.entrySet()) {
                evtData.put(entries.getKey(), entries.getValue());
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

    public static JSONArray pushIdsToJSONArray(String[] pushIds) {
        JSONArray renderedTargets = new JSONArray();
        for (String pushId : pushIds) {
            Logger.v(TAG, "RTL IDs -" + pushId);
            renderedTargets.put(pushId);
        }
        return renderedTargets;
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
            Logger.v(TAG, "Error converting JSONArray to array", e);
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
                Logger.v(TAG, "Error converting JSONArray to list", e);
            }
        }
        return list;
    }

}

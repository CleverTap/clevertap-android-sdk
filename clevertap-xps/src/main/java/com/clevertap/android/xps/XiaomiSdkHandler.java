package com.clevertap.android.xps;

import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.xps.XpsConstants.XIAOMI_LOG_TAG;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.text.TextUtils;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ManifestInfo;
import com.xiaomi.channel.commonutils.android.Region;
import com.xiaomi.mipush.sdk.MiPushClient;
import java.util.List;

/**
 * Implementation of {@link IMiSdkHandler}
 */
class XiaomiSdkHandler implements IMiSdkHandler {

    private final Context context;

    private boolean isRegistered;

    private final CleverTapInstanceConfig mConfig;

    private ManifestInfo manifestInfo;

    XiaomiSdkHandler(final Context context, final CleverTapInstanceConfig config) {
        this(context, config, true);
    }

    XiaomiSdkHandler(final Context context, final CleverTapInstanceConfig config, final boolean isInit) {
        this.context = context.getApplicationContext();
        mConfig = config;
        this.manifestInfo = ManifestInfo.getInstance(context);
        if (isInit) {
            init();
        }
    }

    @Override
    public String appId() {
        return manifestInfo.getXiaomiAppID();
    }

    @Override
    public String appKey() {
        return manifestInfo.getXiaomiAppKey();
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(appId()) && !TextUtils.isEmpty(appKey());
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    @Override
    public String onNewToken() {
        String token = null;
        if (!isRegistered) {
            init();
        }
        try {
            token = MiPushClient.getRegId(context);
            mConfig.log(LOG_TAG, XIAOMI_LOG_TAG + "Xiaomi Token Success- " + token);
        } catch (Throwable t) {
            mConfig.log(LOG_TAG, XIAOMI_LOG_TAG + "Xiaomi Token Failed");
        }

        return token;
    }

    @Override
    public void unregisterPush(final Context context) {
        try {
            MiPushClient.unregisterPush(context);
            mConfig.log(LOG_TAG, XIAOMI_LOG_TAG + "Xiaomi Unregister Success");
        } catch (Throwable t) {
            mConfig.log(LOG_TAG, XIAOMI_LOG_TAG + "Xiaomi Unregister Failed");
        }
    }

    @RestrictTo(value = RestrictTo.Scope.LIBRARY)
    public void register(String appId, String appKey) throws RegistrationException {
        Logger.v("XiaomiSDKHandler: register | called with appid = "+appId + ", appkey="+appKey);

        try {
            String region = mConfig.getAccountRegion();
            region =  (region==null || region.isEmpty())? Constants.REGION_EUROPE : region;
            Logger.v("XiaomiSDKHandler: register | final region from mConfig = "+region);

            Region xiaomiRegion =  region.equalsIgnoreCase( Constants.REGION_INDIA) ? Region.India : Region.Global;
            Logger.v("XiaomiSDKHandler: register | final xiaomi region as per manifest = "+xiaomiRegion.name());

            Logger.v("XiaomiSDKHandler: register | final xiaomi setting xiaomi region via  MiPushClient.setRegion(xiaomiRegion) and calling MiPushClient.registerPush(context, appId, appKey);");

            MiPushClient.setRegion(xiaomiRegion);
            MiPushClient.registerPush(context, appId, appKey);
            isRegistered = true;
            mConfig.log(LOG_TAG, XIAOMI_LOG_TAG + "Xiaomi Registeration success for appId-" + appId + " and appKey-" + appKey);
        } catch (Throwable throwable) {
            isRegistered = false;
            mConfig.log(LOG_TAG, XIAOMI_LOG_TAG + "Xiaomi Registration failed for appId-" + appId + " appKey-" + appKey);
            throw new RegistrationException("Registration failed for appId " + appId + " and appKey " + appKey);
        }
    }

    void setManifestInfo(ManifestInfo manifestInfo) {
        this.manifestInfo = manifestInfo;
    }

    boolean shouldInit(String mainProcessName) {

        ActivityManager am = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));

        List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();

        int myPid = Process.myPid();

        for (ActivityManager.RunningAppProcessInfo info : processInfos) {
            if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                return true;
            }
        }
        return false;
    }

    private void init() {
        String packageName = context.getPackageName();
        if (CoreMetaData.isAppForeground() && shouldInit(packageName)) {
            String appId = appId();
            String appKey = appKey();
            try {
                register(appId, appKey);
            } catch (Throwable t) {
                //do nothing
            }
        }
    }
}
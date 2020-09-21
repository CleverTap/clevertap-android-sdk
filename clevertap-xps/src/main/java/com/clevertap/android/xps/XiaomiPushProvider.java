package com.clevertap.android.xps;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class XiaomiPushProvider implements CTPushProvider {
    private static final String LOG_TAG = XiaomiPushProvider.class.getSimpleName();
    private CTPushProviderListener ctPushListener;

    @Override
    public void setCTPushListener(CTPushProviderListener ctPushListener) {
        this.ctPushListener = ctPushListener;
        if (shouldInit())
            register();
    }

    @Override
    public int getPlatform() {
        return PushConstants.ANDROID_PLATFORM;
    }

    @NonNull
    @Override
    public PushConstants.PushType getPushType() {
        return PushConstants.PushType.XPS;
    }

    @Override
    public void requestToken() {
        String token = null;
        try {
            token = MiPushClient.getRegId(ctPushListener.context());
            ctPushListener.log(LOG_TAG, "Xiaomi Token Success- " + token);
        } catch (Throwable t) {
            ctPushListener.log(LOG_TAG, "Xiaomi Token Failed");
        }
        if (ctPushListener != null) {
            ctPushListener.onNewToken(token, getPushType());
        }
    }

    private void register() {
        String appKey = getAppKey();
        String appId = getAppId();
        try {
            MiPushClient.registerPush(ctPushListener.context(), appId, appKey);
            ctPushListener.log(LOG_TAG, "Xiaomi Registeration success for appId-" + appId + " and appKey-" + appKey);
        } catch (Throwable t) {
            ctPushListener.log(LOG_TAG, "getRegistrationToken : Registration failed for appId-" + appId + " appKey-" + appKey);
        }
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(getAppId()) && !TextUtils.isEmpty(getAppKey());
    }

    private String getAppId() {
        return ManifestInfo.getInstance(ctPushListener.context()).getXiaomiAppID();
    }

    private String getAppKey() {
        return ManifestInfo.getInstance(ctPushListener.context()).getXiaomiAppKey();
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public int minSDKSupportVersionCode() {
        return 30800;
    }

    private boolean shouldInit() {

        ActivityManager am = ((ActivityManager) ctPushListener.context().getSystemService(Context.ACTIVITY_SERVICE));

        List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();

        String mainProcessName = ctPushListener.context().getPackageName();

        int myPid = Process.myPid();

        for (ActivityManager.RunningAppProcessInfo info : processInfos) {
            if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                return true;
            }
        }
        return false;
    }
}
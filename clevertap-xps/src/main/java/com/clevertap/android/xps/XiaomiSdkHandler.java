package com.clevertap.android.xps;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;

import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.util.List;

import static com.clevertap.android.xps.XpsConstants.LOG_TAG;

public class XiaomiSdkHandler implements IMiSdkHandler {

    private final CTPushProviderListener ctPushListener;

    XiaomiSdkHandler(CTPushProviderListener ctPushListener) {
        this.ctPushListener = ctPushListener;
        if (shouldInit())
            register();
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

    @Override
    public String onNewToken() {
        String token = null;
        try {
            token = MiPushClient.getRegId(ctPushListener.context());
            ctPushListener.log(LOG_TAG, "Xiaomi Token Success- " + token);
        } catch (Throwable t) {
            ctPushListener.log(LOG_TAG, "Xiaomi Token Failed");
        }
        return token;
    }

    @Override
    public String appKey() {
        return ManifestInfo.getInstance(ctPushListener.context()).getXiaomiAppKey();
    }

    @Override
    public String appId() {
        return ManifestInfo.getInstance(ctPushListener.context()).getXiaomiAppID();
    }

    private void register() {
        String appId = appId();
        String appKey = appKey();
        try {
            MiPushClient.registerPush(ctPushListener.context(), appId, appKey);
            ctPushListener.log(LOG_TAG, "Xiaomi Registeration success for appId-" + appId + " and appKey-" + appKey);
        } catch (Throwable t) {
            ctPushListener.log(LOG_TAG, "Xiaomi Registration failed for appId-" + appId + " appKey-" + appKey);
        }
    }
}
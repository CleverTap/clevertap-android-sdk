package com.clevertap.android.xps;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.text.TextUtils;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.clevertap.android.sdk.BaseCTApiListener;
import com.clevertap.android.sdk.ManifestInfo;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.util.List;

import static com.clevertap.android.xps.XpsConstants.LOG_TAG;

public class XiaomiSdkHandler implements IMiSdkHandler {
    private final BaseCTApiListener ctApiListener;
    private boolean isRegistered;
    private ManifestInfo manifestInfo;

    XiaomiSdkHandler(BaseCTApiListener ctPushListener) {
        this.ctApiListener = ctPushListener;
        this.manifestInfo = ManifestInfo.getInstance(ctPushListener.context());
        init();
    }

    @VisibleForTesting
    @RestrictTo(value = RestrictTo.Scope.LIBRARY)
    public void setManifestInfo(ManifestInfo manifestInfo) {
        this.manifestInfo = manifestInfo;
    }

    private void init() {
        String packageName = ctApiListener.context().getPackageName();
        if (shouldInit(packageName)) {
            String appId = appId();
            String appKey = appKey();
            try {
                register(appId, appKey);
            } catch (Throwable t) {
                //do nothing
            }
        }
    }

    @VisibleForTesting
    @RestrictTo(value = RestrictTo.Scope.LIBRARY)
    public boolean shouldInit(String mainProcessName) {

        ActivityManager am = ((ActivityManager) ctApiListener.context().getSystemService(Context.ACTIVITY_SERVICE));

        List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();

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
        if (!isRegistered) {
            init();
        }
        try {
            token = MiPushClient.getRegId(ctApiListener.context());
            ctApiListener.config().log(LOG_TAG, "Xiaomi Token Success- " + token);
        } catch (Throwable t) {
            ctApiListener.config().log(LOG_TAG, "Xiaomi Token Failed");
        }

        return token;
    }

    @Override
    public String appKey() {
        return manifestInfo.getXiaomiAppKey();
    }

    @Override
    public String appId() {
        return manifestInfo.getXiaomiAppID();
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(appId()) && !TextUtils.isEmpty(appKey());
    }

    @RestrictTo(value = RestrictTo.Scope.LIBRARY)
    public void register(String appId, String appKey) throws RegistrationException {
        try {
            MiPushClient.registerPush(ctApiListener.context(), appId, appKey);
            isRegistered = true;
            ctApiListener.config().log(LOG_TAG, "Xiaomi Registeration success for appId-" + appId + " and appKey-" + appKey);
        } catch (Throwable throwable) {
            isRegistered = false;
            ctApiListener.config().log(LOG_TAG, "Xiaomi Registration failed for appId-" + appId + " appKey-" + appKey);
            throw new RegistrationException("Registration failed for appId " + appId + " and appKey " + appKey);
        }
    }

    public boolean isRegistered() {
        return isRegistered;
    }
}
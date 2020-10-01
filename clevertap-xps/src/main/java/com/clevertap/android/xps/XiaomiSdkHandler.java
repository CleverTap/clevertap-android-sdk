package com.clevertap.android.xps;

import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.xps.XpsConstants.XIAOMI_LOG_TAG;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.text.TextUtils;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.BaseCTApiListener;
import com.clevertap.android.sdk.ManifestInfo;
import com.xiaomi.mipush.sdk.MiPushClient;
import java.util.List;

/**
 * Implementation of {@link IMiSdkHandler}
 */
class XiaomiSdkHandler implements IMiSdkHandler {

    private final BaseCTApiListener ctApiListener;

    private boolean isRegistered;

    private ManifestInfo manifestInfo;

    XiaomiSdkHandler(BaseCTApiListener ctPushListener) {
        this.ctApiListener = ctPushListener;
        this.manifestInfo = ManifestInfo.getInstance(ctPushListener.context());
        init();
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
            token = MiPushClient.getRegId(ctApiListener.context());
            ctApiListener.config().log(LOG_TAG, XIAOMI_LOG_TAG + "Xiaomi Token Success- " + token);
        } catch (Throwable t) {
            ctApiListener.config().log(LOG_TAG, XIAOMI_LOG_TAG + "Xiaomi Token Failed");
        }

        return token;
    }

    @RestrictTo(value = RestrictTo.Scope.LIBRARY)
    public void register(String appId, String appKey) throws RegistrationException {
        try {
            MiPushClient.registerPush(ctApiListener.context(), appId, appKey);
            isRegistered = true;
            ctApiListener.config()
                    .log(LOG_TAG, XIAOMI_LOG_TAG + "Xiaomi Registeration success for appId-" + appId + " and appKey-"
                            + appKey);
        } catch (Throwable throwable) {
            isRegistered = false;
            ctApiListener.config()
                    .log(LOG_TAG,
                            XIAOMI_LOG_TAG + "Xiaomi Registration failed for appId-" + appId + " appKey-" + appKey);
            throw new RegistrationException("Registration failed for appId " + appId + " and appKey " + appKey);
        }
    }

    void setManifestInfo(ManifestInfo manifestInfo) {
        this.manifestInfo = manifestInfo;
    }

    boolean shouldInit(String mainProcessName) {

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
}
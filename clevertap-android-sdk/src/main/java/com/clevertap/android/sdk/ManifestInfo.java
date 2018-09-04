package com.clevertap.android.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class ManifestInfo {
    private static String accountId;
    private static String accountToken;
    private static String accountRegion;
    private static String gcmSenderId;
    private static boolean useADID;
    private static boolean appLaunchedDisabled;
    private static String notificationIcon;
    private static ManifestInfo instance;
    private static String excludedActivities;
    private static boolean sslPinning;

    private static String _getManifestStringValueForKey(Bundle manifest, String name) {
        try {
            Object o = manifest.get(name);
            return (o != null) ? o.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private ManifestInfo(Context context) {
        Bundle metaData = null;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            metaData = ai.metaData;
        } catch (Throwable t) {
            // no-op
        }
        if (metaData == null) {
            metaData = new Bundle();
        }
        if(accountId==null)
            accountId = _getManifestStringValueForKey(metaData, Constants.LABEL_ACCOUNT_ID);
        if(accountToken==null)
            accountToken = _getManifestStringValueForKey(metaData, Constants.LABEL_TOKEN);
        if(accountRegion==null)
            accountRegion = _getManifestStringValueForKey(metaData, Constants.LABEL_REGION);
        gcmSenderId = _getManifestStringValueForKey(metaData, Constants.LABEL_SENDER_ID);
        notificationIcon = _getManifestStringValueForKey(metaData,Constants.LABEL_NOTIFICATION_ICON);
        if (gcmSenderId != null) {
            gcmSenderId = gcmSenderId.replace("id:", "");
        }
        useADID = "1".equals(_getManifestStringValueForKey(metaData, Constants.CLEVERTAP_USE_GOOGLE_AD_ID));
        appLaunchedDisabled = "1".equals(_getManifestStringValueForKey(metaData, Constants.LABEL_DISABLE_APP_LAUNCH));
        excludedActivities = _getManifestStringValueForKey(metaData,Constants.LABEL_INAPP_EXCLUDE);
        sslPinning = "1".equals(_getManifestStringValueForKey(metaData,Constants.LABEL_SSL_PINNING));
    }

    public synchronized static ManifestInfo getInstance(Context context){
        if (instance == null) {
            instance = new ManifestInfo(context);
        }
        return instance;
    }

    String getAccountId(){
        return accountId;
    }

    String getAcountToken(){
        return accountToken;
    }

    String getAccountRegion(){
        return accountRegion;
    }

    public String getGCMSenderId(){
        return gcmSenderId;
    }

    public boolean useGoogleAdId(){
         return useADID;
    }

    public boolean isAppLaunchedDisabled(){
         return appLaunchedDisabled;
    }

    boolean isSSLPinningEnabled(){return sslPinning;}

    String getNotificationIcon() {
        return notificationIcon;
    }

    String getExcludedActivities(){return excludedActivities;}

    static void changeCredentials(String id, String token, String region){
        accountId = id;
        accountToken = token;
        accountRegion = region;
    }
}

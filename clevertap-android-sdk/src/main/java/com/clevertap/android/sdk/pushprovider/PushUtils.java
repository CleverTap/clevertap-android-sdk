package com.clevertap.android.sdk.pushprovider;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.CTExecutors;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.StorageHelper;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PushUtils {
    private PushUtils() {

    }

    public static String getFCMSenderID(Context context) {
        return ManifestInfo.getInstance(context.getApplicationContext()).getFCMSenderId();
    }

    public static void cacheToken(final Context context, final CleverTapInstanceConfig config, final String token, final PushConstants.PushType pushType) {
        if (config == null || TextUtils.isEmpty(token) || pushType == null) return;

        try {
            CTExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    if (alreadyHaveToken(context, config, token, pushType)) return;

                    final SharedPreferences prefs = StorageHelper.getPreferences(context);
                    @PushConstants.RegKeyType String key = pushType.getTokenPrefKey();
                    if (prefs == null || TextUtils.isEmpty(key)) return;

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(StorageHelper.storageKeyWithSuffix(config, key), token);
                    StorageHelper.persistImmediately(editor);
                    config.getLogger().verbose(config.getAccountId(), pushType + "Cached New Token successfully " + token);
                }
            });

        } catch (Throwable t) {
            config.getLogger()
                    .verbose(config.getAccountId(), pushType + "Unable to cache token " + token, t);
        }
    }

    private static boolean alreadyHaveToken(Context context, CleverTapInstanceConfig config, String newToken, PushConstants.PushType pushType) {
        boolean alreadyAvailable = !TextUtils.isEmpty(newToken) && pushType != null && newToken.equalsIgnoreCase(getCachedToken(context, config, pushType));
        if (pushType != null)
            config.getLogger().verbose(config.getAccountId(), pushType + "Token Already available value: " + alreadyAvailable);
        return alreadyAvailable;
    }

    public static String getCachedToken(Context context, CleverTapInstanceConfig config, PushConstants.PushType pushType) {
        if (config != null && pushType != null) {
            SharedPreferences prefs = StorageHelper.getPreferences(context);
            if (prefs != null) {
                @PushConstants.RegKeyType String key = pushType.getTokenPrefKey();
                if (!TextUtils.isEmpty(key)) {
                    String cachedToken = StorageHelper.getStringFromPrefs(context, config, key, null);
                    config.getLogger().verbose(config.getAccountId(), pushType + "getting Cached Token - " + cachedToken);
                    return cachedToken;
                }
            }
        }
        if (config != null && pushType != null) {
            config.getLogger().verbose(config.getAccountId(), pushType + " Unable to find cached Token for type ");
        }
        return null;
    }
}
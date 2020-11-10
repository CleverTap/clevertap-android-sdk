package com.clevertap.android.sdk;

import android.content.Context;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

class CTProfileHandlerImpl implements IProfileHandler {
    private Context mContext;
    private CleverTapInstanceConfig mConfig;

    CTProfileHandlerImpl(Context context, CleverTapInstanceConfig config) {
        this.mContext = context;
        this.mConfig = config;
    }

    HashSet<String> getProfileIdentitySet(Context context) {
        HashSet hashSet = new HashSet();
        String identifier = StorageHelper.getString(context, Constants.SP_KEY_PROFILE_IDENTITIES, "");

        if (!TextUtils.isEmpty(identifier)) {
            hashSet.add(Arrays.asList(identifier.split(Constants.SEPARATOR_COMMA)));
        } else {
            ManifestInfo manifestInfo = ManifestInfo.getInstance(context);
            HashSet<String> clientProfileSet = manifestInfo.getProfileIdentifierKeys();
            if (!clientProfileSet.isEmpty()) {
                hashSet.add(clientProfileSet);
                // store the prefs
                storeClientKeysToPref(clientProfileSet);
            } else {
                hashSet.add(Constants.DEFAULT_PROFILE_IDENTIFIER_KEYS);
            }
        }
        return hashSet;
    }

    private void storeClientKeysToPref(HashSet<String> clientProfileSet) {

    }

    @Override
    public CleverTapInstanceConfig config() {
        return mConfig;
    }

    @Override
    public Context context() {
        return mContext;
    }
}
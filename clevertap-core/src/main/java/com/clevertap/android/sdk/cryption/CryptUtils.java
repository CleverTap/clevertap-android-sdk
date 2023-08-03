package com.clevertap.android.sdk.cryption;


import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.CTJsonConverter;

import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Callable;

public final class CryptUtils {

    private CryptUtils() {

    }

    /**
     * This method migrates the encryption level of the stored data for the current account ID
     *
     * @param context - The Android context
     * @param config  - The {@link CleverTapInstanceConfig} object
     */
    public static void migrateEncryptionLevel(Context context, CleverTapInstanceConfig config) {

        int configEncryptionLevel = config.getEncryptionLevel();
        int storedEncryptionLevel = StorageHelper.getInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_LEVEL), 0);
        if (storedEncryptionLevel == configEncryptionLevel) {
            return;
        }
        config.getLogger().verbose(config.getAccountId(), "Migrating encryption level from " + storedEncryptionLevel + " to " + configEncryptionLevel);
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_LEVEL), configEncryptionLevel);

        // If configEncryptionLevel is greater than storedEncryptionLevel, encryption level has increased. Hence perform encryption
        // Otherwise decryption
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("migratingEncryptionLevel", new Callable<Void>() {
            @Override
            public Void call() {
                migrateEncryption(configEncryptionLevel > storedEncryptionLevel, context, config);
                return null;
            }
        });

    }

    static void migrateEncryption(boolean encrypt, Context context, CleverTapInstanceConfig config) {
        migrateCachedGuidsKeyPref(encrypt, config, context);
        migrateARPPreferenceFiles(encrypt, config, context);
    }

    /**
     * This method migrates the encryption level of the value under cachedGUIDsKey stored in the shared preference file
     * Only the value of the identifier(eg: johndoe@gmail.com) is encrypted/decrypted for this key throughout the sdk
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * @param config  - The {@link CleverTapInstanceConfig} object
     * @param context - The Android context
     */
    static void migrateCachedGuidsKeyPref(boolean encrypt, CleverTapInstanceConfig config, Context context) {
        config.getLogger().verbose(config.getAccountId(), "Migrating encryption level for cachedGUIDsKey prefs");

        Crypt instance = config.getCrypt();
        String json = StorageHelper.getStringFromPrefs(context, config, Constants.CACHED_GUIDS_KEY, null);
        JSONObject cachedGuidJsonObj = CTJsonConverter.toJsonObject(json, config.getLogger(), config.getAccountId());
        JSONObject newGuidJsonObj = new JSONObject();
        try {
            Iterator<String> i = cachedGuidJsonObj.keys();
            while (i.hasNext()) {
                String nextJSONObjKey = i.next();
                String key = nextJSONObjKey.substring(0, nextJSONObjKey.indexOf("_") + 1);
                String identifier = nextJSONObjKey.substring(nextJSONObjKey.indexOf("_") + 1);
                String crypted;
                if (encrypt)
                    crypted = instance.encrypt(identifier, Constants.CACHED_GUIDS_KEY);
                else
                    crypted = instance.decrypt(identifier, Constants.KEY_ENCRYPTION_MIGRATION);
                String cryptedKey = key + crypted;

                newGuidJsonObj.put(cryptedKey, cachedGuidJsonObj.get(nextJSONObjKey));
            }
            if (cachedGuidJsonObj.length() > 0) {
                String cachedGuid = newGuidJsonObj.toString();
                StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(config, Constants.CACHED_GUIDS_KEY), cachedGuid);
                config.getLogger().verbose(config.getAccountId(), "setCachedGUIDs after migration:[" + cachedGuid + "]");
            }
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Error migrating cached guids: " + t);
        }

    }

    /**
     * This method migrates the encryption level of the value under the key k_n. This data stored in the ARP related shared preference files
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * @param config  - The {@link CleverTapInstanceConfig} object
     * @param context - The Android context
     */
    static void migrateARPPreferenceFiles(boolean encrypt, CleverTapInstanceConfig config, Context context) {
        config.getLogger().verbose(config.getAccountId(), "Migrating encryption level for ARP related prefs");
        try {
            String dataDir = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).applicationInfo.dataDir;
            File prefsDir = new File(dataDir, "shared_prefs");

            Crypt instance = config.getCrypt();
            String path = Constants.CLEVERTAP_STORAGE_TAG + "_ARP:" + config.getAccountId();
            for (String prefName : Objects.requireNonNull(prefsDir.list())) {
                if (prefName.startsWith(path)) {
                    String prefFile = prefName.substring(0, prefName.length() - 4);
                    SharedPreferences prefs = context.getSharedPreferences(prefFile, MODE_PRIVATE);
                    String value = prefs.getString(Constants.KEY_k_n, "");
                    if (!value.equals("")) {
                        String crypted;
                        if (encrypt)
                            crypted = instance.encrypt(value, Constants.KEY_k_n);
                        else
                            crypted = instance.decrypt(value, Constants.KEY_k_n);
                        SharedPreferences.Editor editor = prefs.edit().putString(Constants.KEY_k_n, crypted);
                        StorageHelper.persist(editor);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            config.getLogger().verbose(config.getAccountId(), "Error migrating ARP Preference Files: " + e);
        }
    }
}
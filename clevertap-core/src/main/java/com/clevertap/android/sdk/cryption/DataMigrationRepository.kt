package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.CACHED_GUIDS_KEY
import com.clevertap.android.sdk.Constants.INAPP_KEY
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.utils.CTJsonConverter
import org.json.JSONObject
import java.io.File

interface IDataMigrationRepository {
    fun cachedGuidJsonObject(): JSONObject
    fun cachedGuidString(): String?
    fun saveCachedGuidJson(json: String?)
    fun removeCachedGuidJson()
    fun saveCachedGuidJsonLength(length: Int)
    fun userProfilesInAccount(): Map<String, JSONObject>
    fun saveUserProfile(deviceID: String, profile: JSONObject): Long
    fun inAppDataFiles(keysToMigrate: List<String>, migrate: (String) -> String?)
}

internal class DataMigrationRepository(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val dbAdapter: DBAdapter
) : IDataMigrationRepository {

    override fun cachedGuidString(): String? {
        return StorageHelper.getStringFromPrefs(context, config, CACHED_GUIDS_KEY, null)
    }
    override fun cachedGuidJsonObject(): JSONObject {
        val json = cachedGuidString()
        val cachedGuidJsonObj = CTJsonConverter.toJsonObject(json, config.logger, config.accountId)
        return cachedGuidJsonObj
    }

    override fun saveCachedGuidJson(json: String?) {
        StorageHelper.putString(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, CACHED_GUIDS_KEY),
            json
        )
    }

    override fun removeCachedGuidJson() {
        StorageHelper.remove(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, CACHED_GUIDS_KEY),
        )
    }

    override fun saveCachedGuidJsonLength(length: Int) {
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.CACHED_GUIDS_LENGTH_KEY),
            length
        )
    }

    override fun userProfilesInAccount(): Map<String,JSONObject> {
        return dbAdapter.fetchUserProfilesByAccountId(config.accountId)
    }

    override fun saveUserProfile(deviceID: String, profile: JSONObject): Long {
        return dbAdapter.storeUserProfile(config.accountId, deviceID, profile)
    }

    override fun inAppDataFiles(
        keysToMigrate: List<String>,
        migrate: (String) -> String?
    ) {
        File(context.applicationInfo.dataDir, "shared_prefs")
            .listFiles { _, name ->
                // Check StoreProvider.constructStorePreferenceName() to check how the name is constructed
                name.startsWith(INAPP_KEY) && name.endsWith("${config.accountId}.xml")
            }?.map { file ->
                val prefName = file.nameWithoutExtension
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            }?.forEach { sp ->
            keysToMigrate.forEach { key ->
                sp.getString(key, null)?.let { data ->
                    val encryptedData = migrate(data)
                    sp.edit().putString(key, encryptedData).apply()
                }
            }
        }
    }
}
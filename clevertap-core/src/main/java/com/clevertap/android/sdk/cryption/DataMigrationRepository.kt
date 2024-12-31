package com.clevertap.android.sdk.cryption

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants.CACHED_GUIDS_KEY
import com.clevertap.android.sdk.Constants.INAPP_KEY
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.utils.CTJsonConverter
import org.json.JSONObject
import java.io.File

interface IDataMigrationRepository {
    fun cachedGuidJsonObject(): JSONObject
    fun saveCachedGuidJsonObject(newGuidJsonObj: JSONObject)
    fun userProfilesInAccount(): Map<String, JSONObject>
    fun saveUserProfile(deviceID: String, profile: JSONObject): Long
    fun inAppDataFiles(): List<SharedPreferences>
}

internal class DataMigrationRepository(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val dbAdapter: DBAdapter
) : IDataMigrationRepository {
    override fun cachedGuidJsonObject(): JSONObject {
        val json = StorageHelper.getStringFromPrefs(context, config, CACHED_GUIDS_KEY, null)
        val cachedGuidJsonObj = CTJsonConverter.toJsonObject(json, config.logger, config.accountId)
        return cachedGuidJsonObj
    }

    override fun saveCachedGuidJsonObject(newGuidJsonObj: JSONObject) {
        StorageHelper.putString(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, CACHED_GUIDS_KEY),
            newGuidJsonObj.toString()
        )
    }

    override fun userProfilesInAccount(): Map<String,JSONObject> {
        return dbAdapter.fetchUserProfilesByAccountId(config.accountId)
    }

    override fun saveUserProfile(deviceID: String, profile: JSONObject): Long {
        return dbAdapter.storeUserProfile(config.accountId, deviceID, profile)
    }

    override fun inAppDataFiles(): List<SharedPreferences> {
        // Fetch all SharedPreferences files starting with "inApp" and ending with the accountId
        return File(context.applicationInfo.dataDir, "shared_prefs")
            .listFiles { _, name ->
                // Check StoreProvider.constructStorePreferenceName() to check how the name is constructed
                name.startsWith(INAPP_KEY) && name.endsWith("${config.accountId}.xml")
            }?.map { file ->
                val prefName = file.nameWithoutExtension
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            } ?: emptyList()
    }
}
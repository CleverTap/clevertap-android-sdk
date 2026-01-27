package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.INAPP_KEY
import com.clevertap.android.sdk.Constants.PREFS_EVALUATED_INAPP_KEY_SS
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_CS
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_SS
import com.clevertap.android.sdk.Constants.PREFS_SUPPRESSED_INAPP_KEY_CS
import com.clevertap.android.sdk.STORE_TYPE_INAPP
import com.clevertap.android.sdk.StoreProvider
import com.clevertap.android.sdk.cryption.ICryptHandler
import com.clevertap.android.sdk.login.ChangeUserCallback
import com.clevertap.android.sdk.store.preference.ICTPreference
import com.clevertap.android.sdk.toList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
/**
 * The `InAppStore` class manages the storage and retrieval of In-App messages.
 * In-App messages can be stored in either Client-side mode (CS) or Server-side mode (SS).
 *
 * <p>
 * It stores in-apps in the shared preferences named "WizRocket_inapp:<<account_id>>:<<device_id>>"
 * with keys "inapp_notifs_cs" for client-side inApps whereas "inapp_notifs_ss" for server-side inApps meta data.
 * It also stores legacy ss in-apps using key "inApp", evaluated ss inapp ids and suppressed cs inapp ids
 * </p>
 *
 * @property ctPreference The shared preference handler for storing and retrieving data.
 * @property cryptHandler The handler used for encryption and decryption of In-App messages.
 */
internal class InAppStore(
    private val ctPreference: ICTPreference,
    private val cryptHandler: ICryptHandler
) : ChangeUserCallback {

    private var clientSideInAppsCache: List<JSONObject>? = null
    private var serverSideInAppsCache: List<JSONObject>? = null
    private var clientSideDelayedInAppsCache: List<JSONObject>? = null
    private var serverSideMetaCache: List<JSONObject>? = null
    private var serverSideInActionCache: List<JSONObject>? = null

    companion object {

        const val CLIENT_SIDE_MODE = "CS"
        const val SERVER_SIDE_MODE = "SS"
        const val NO_MODE = "NO_MODE"
        const val PREFS_DELAYED_INAPP_KEY_CS = "delayed_inapp_notifs_cs"
        const val PREFS_INACTION_INAPP_META_KEY_SS = "inaction_inapp_notifs_ss"  // In-Action SS metadata
    }

    /**
     * The mode in which In-App messages are stored. Set to either [CLIENT_SIDE_MODE] or [SERVER_SIDE_MODE].
     *
     * <p>
     * If the mode changes from Server-side (SS) to Client-side (CS), all Server-side In-App messages
     * are cleaned. If the mode changes from Client-side (CS) to Server-side (SS), all Client-side In-App
     * messages are cleaned. If no flag(null) is returned (i.e. NO_MODE case), all Client-side and Server-side In-App messages
     * are cleaned.
     * </p>
     */
    var mode: String? = null
        set(value) {
            if (field == value) return
            field = value

            when (value) {
                CLIENT_SIDE_MODE -> {
                    removeServerSideInAppsMetaData()
                    removeServerSideInActionInAppsMetaData()
                }
                SERVER_SIDE_MODE -> {
                    removeClientSideInApps()
                    removeClientSideDelayedInApps()
                }
                NO_MODE -> {
                    removeServerSideInAppsMetaData()
                    removeServerSideInActionInAppsMetaData()
                    removeClientSideInApps()
                    removeClientSideDelayedInApps()
                }
            }
        }

    /**
     * Removes Client-side In-App messages.
     */
    private fun removeClientSideInApps() {
        ctPreference.remove(PREFS_INAPP_KEY_CS)
        clientSideInAppsCache = null
    }

    /**
     * Removes Server-side In-App meta data.
     */
    private fun removeServerSideInAppsMetaData() {
        ctPreference.remove(PREFS_INAPP_KEY_SS)
        serverSideMetaCache = null
    }
    private fun removeServerSideInActionInAppsMetaData() {
        ctPreference.remove(PREFS_INACTION_INAPP_META_KEY_SS)
        serverSideInActionCache = null
    }

    /**
     * Stores Client-side In-App messages in encrypted format.
     *
     * @param clientSideInApps The list of Client-side In-App messages.
     */
    fun storeClientSideInApps(clientSideInApps: List<JSONObject>) {
        clientSideInAppsCache = clientSideInApps
        val jsonArray = JSONArray(clientSideInApps)
        val encryptedString = cryptHandler.encrypt(jsonArray.toString())
        encryptedString?.apply {
            ctPreference.writeString(PREFS_INAPP_KEY_CS, this)
        }
    }

    /**
     * Stores Server-side In-App metadata.
     *
     * @param serverSideInAppsMetaData The list of Server-side In-App metadata.
     */
    fun storeServerSideInAppsMetaData(serverSideInAppsMetaData: List<JSONObject>) {
        serverSideMetaCache = serverSideInAppsMetaData
        val jsonArray = JSONArray(serverSideInAppsMetaData)
        ctPreference.writeString(PREFS_INAPP_KEY_SS, jsonArray.toString())
    }
    fun storeServerSideInActionMetaData(serverSideInActionMetaData: List<JSONObject>) {
        serverSideInActionCache = serverSideInActionMetaData
        val jsonArray = JSONArray(serverSideInActionMetaData)
        ctPreference.writeString(PREFS_INACTION_INAPP_META_KEY_SS, jsonArray.toString())
    }

    /**
     * Stores Server-side legacy In-App messages in encrypted format.
     *
     * @param serverSideInApps The list of Server-side legacy In-App messages.
     */
    fun storeServerSideInApps(serverSideInApps: List<JSONObject>) {
        serverSideInAppsCache = serverSideInApps
        val jsonArray = JSONArray(serverSideInApps)
        val encryptedString = cryptHandler.encrypt(jsonArray.toString())
        encryptedString?.apply { ctPreference.writeString(INAPP_KEY, this) }
    }

    /**
     * Stores evaluated Server-side In-App IDs.
     *
     * @param evaluatedServerSideInAppIds The array of evaluated Server-side In-App IDs.
     */
    fun storeEvaluatedServerSideInAppIds(evaluatedServerSideInAppIds: JSONArray) {
        ctPreference.writeString(PREFS_EVALUATED_INAPP_KEY_SS, evaluatedServerSideInAppIds.toString())
    }

    /**
     * Stores suppressed Client-side In-App IDs.
     *
     * @param suppressedClientSideInAppIds The array of suppressed Client-side In-App IDs.
     */
    fun storeSuppressedClientSideInAppIds(suppressedClientSideInAppIds: JSONArray) {
        ctPreference.writeString(PREFS_SUPPRESSED_INAPP_KEY_CS, suppressedClientSideInAppIds.toString())
    }

    /**
     * Reads and decrypts Client-side In-App messages.
     *
     * @return A list of Client-side In-App messages.
     */
    fun readClientSideInApps(): List<JSONObject> {
        clientSideInAppsCache?.let { return it }

        val csInAppsEncrypted = ctPreference.readString(PREFS_INAPP_KEY_CS, "")
        val result = if (csInAppsEncrypted.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val decrypted = cryptHandler.decrypt(csInAppsEncrypted)
                JSONArray(decrypted).toList<JSONObject>()
            } catch (e: Exception) {
                emptyList()
            }
        }
        clientSideInAppsCache = result
        return result
    }

    /**
     * Reads Server-side In-App metadata.
     *
     * @return A list of Server-side In-App metadata.
     */
    fun readServerSideInAppsMetaData(): List<JSONObject> {
        serverSideMetaCache?.let { return it }

        val ssInAppsMetaData = ctPreference.readString(PREFS_INAPP_KEY_SS, "")
        val result = if (ssInAppsMetaData.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                JSONArray(ssInAppsMetaData).toList<JSONObject>()
            } catch (e: JSONException) {
                emptyList()
            }
        }
        serverSideMetaCache = result
        return result
    }
    fun readServerSideInActionMetaData(): List<JSONObject> {
        serverSideInActionCache?.let { return it }

        val ssInAppsMetaData = ctPreference.readString(PREFS_INACTION_INAPP_META_KEY_SS, "")
        val result = if (ssInAppsMetaData.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                JSONArray(ssInAppsMetaData).toList<JSONObject>()
            } catch (e: JSONException) {
                emptyList()
            }
        }
        serverSideInActionCache = result
        return result
    }

    /**
     * Reads evaluated Server-side In-App IDs.
     *
     * @return An array of evaluated Server-side In-App IDs.
     */
    fun readEvaluatedServerSideInAppIds(): JSONArray {
        val evaluatedServerSideInAppIds = ctPreference.readString(PREFS_EVALUATED_INAPP_KEY_SS, "")
        if (evaluatedServerSideInAppIds.isNullOrBlank()) return JSONArray()

        return try {
            JSONArray(evaluatedServerSideInAppIds)
        } catch (e: JSONException) {
            migrateEvaluatedServerSideInAppIds(evaluatedServerSideInAppIds)
        }
    }

    fun migrateEvaluatedServerSideInAppIds(evaluatedIds: String): JSONArray {
        try {
            val oldFormatted = JSONObject(evaluatedIds)
            val raisedArray = oldFormatted.optJSONArray(Constants.RAISED)
            val profileArray = oldFormatted.optJSONArray(Constants.PROFILE)

            return JSONArray().apply {
                if (raisedArray != null) {
                    for (count in 0 until raisedArray.length())  {
                        put(raisedArray.get(count))
                    }
                }
                if (profileArray != null) {
                    for (count in 0 until profileArray.length())  {
                        put(profileArray.get(count))
                    }
                }
            }

        } catch (e: JSONException) {
            // Not legacy-object or invalid JSON: return empty
            return JSONArray()
        }
    }

    /**
     * Reads suppressed Client-side In-App IDs.
     *
     * @return A JSONArray representing suppressed Client-side In-App IDs.
     */
    fun readSuppressedClientSideInAppIds(): JSONArray {
        val suppressedClientSideInAppIds = ctPreference.readString(PREFS_SUPPRESSED_INAPP_KEY_CS, "")
        if (suppressedClientSideInAppIds.isNullOrBlank()) {
            return JSONArray()
        }

        return try {
            // Try to convert the string to a JSONArray which signifies already migrated
            JSONArray(suppressedClientSideInAppIds)
        } catch (jsonException: JSONException) {
            migrateInAppHeaderPrefsForEventType(suppressedClientSideInAppIds)
        }
    }

    /**
     * Migrates suppressed_ss and evaluated_ss after reading from the prefs.
     *
     * @param - inAppIds to be migrated
     * @return - JSONArray in the migrated format
     */
    private fun migrateInAppHeaderPrefsForEventType(inAppIds: String): JSONArray {
        try {
            // Old format data from 6.2.1 -> 7.5.1
            // {"raised":[123],"profile":[456]}
            // New format => [123, 456]
            val oldJsonObject = JSONObject(inAppIds)

            val raisedArray = oldJsonObject.optJSONArray(Constants.RAISED)
            val profileArray = oldJsonObject.optJSONArray(Constants.PROFILE)

            return JSONArray().apply {
                if (raisedArray != null) {
                    for (count in 0 until raisedArray.length())  {
                        put(raisedArray.get(count))
                    }
                }
                if (profileArray != null) {
                    for (count in 0 until profileArray.length())  {
                        put(profileArray.get(count))
                    }
                }
            }
        } catch (_: JSONException) {
            // Not a legacy-object: return empty to avoid errors.
            return JSONArray()
        }
    }

    /**
     * Reads and decrypts Server-side legacy In-App messages.
     *
     * @return A list of Server-side legacy In-App messages.
     */
    fun readServerSideInApps(): List<JSONObject> {
        serverSideInAppsCache?.let { return it }

        val ssInAppsEncrypted = ctPreference.readString(INAPP_KEY, "")
        val result = if (ssInAppsEncrypted.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val decrypted = cryptHandler.decrypt(ssInAppsEncrypted)
                JSONArray(decrypted).toList<JSONObject>()
            } catch (e: Exception) {
                emptyList()
            }
        }
        serverSideInAppsCache = result
        return result
    }

    /**
     * Stores Client-side Delayed In-App messages in encrypted format.
     *
     * @param delayedInApps The list of delayed Client-side In-App messages.
     */
    fun storeClientSideDelayedInApps(delayedInApps: List<JSONObject>) {
        clientSideDelayedInAppsCache = delayedInApps
        val jsonArray = JSONArray(delayedInApps)
        val encryptedString = cryptHandler.encrypt(jsonArray.toString())
        encryptedString?.apply {
            ctPreference.writeString(PREFS_DELAYED_INAPP_KEY_CS, this)
        }
    }

    /**
     * Reads and decrypts Client-side Delayed In-App messages.
     *
     * @return A list of delayed Client-side In-App messages.
     */
    fun readClientSideDelayedInApps(): List<JSONObject> {
        clientSideDelayedInAppsCache?.let { return it }

        val encryptedData = ctPreference.readString(PREFS_DELAYED_INAPP_KEY_CS, "")
        val result = if (encryptedData.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val decrypted = cryptHandler.decrypt(encryptedData)
                JSONArray(decrypted).toList<JSONObject>()
            } catch (e: Exception) {
                emptyList()
            }
        }
        clientSideDelayedInAppsCache = result
        return result
    }


    fun removeClientSideDelayedInApps() {
        ctPreference.remove(PREFS_DELAYED_INAPP_KEY_CS)
        clientSideDelayedInAppsCache = null
    }

    /**
     * Callback method triggered when the user changes, updating the preferences associated with the new user.
     *
     * When called, it constructs a new
     * preference name using the `StoreProvider` and updates the `ctPreference` instance to use the new preference name.
     *
     * @param deviceId The new unique device identifier for the changed user.
     * @param accountId The new unique account identifier for the changed user.
     */
    override fun onChangeUser(deviceId: String, accountId: String) {
        val newPrefName =
            StoreProvider.getInstance().constructStorePreferenceName(STORE_TYPE_INAPP, deviceId, accountId)
        ctPreference.changePreferenceName(newPrefName)
    }
}
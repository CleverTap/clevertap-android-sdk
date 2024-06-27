package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.INAPP_KEY
import com.clevertap.android.sdk.Constants.PREFS_EVALUATED_INAPP_KEY_SS
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_CS
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_SS
import com.clevertap.android.sdk.Constants.PREFS_SUPPRESSED_INAPP_KEY_CS
import com.clevertap.android.sdk.STORE_TYPE_INAPP
import com.clevertap.android.sdk.StoreProvider
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.login.ChangeUserCallback
import com.clevertap.android.sdk.store.preference.ICTPreference
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
class InAppStore(
    private val ctPreference: ICTPreference,
    private val cryptHandler: CryptHandler
) : ChangeUserCallback {

    private var clientSideInApps: JSONArray? = null
    private var serverSideInApps: JSONArray? = null

    companion object {

        const val CLIENT_SIDE_MODE = "CS"
        const val SERVER_SIDE_MODE = "SS"
        const val NO_MODE = "NO_MODE"
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
                CLIENT_SIDE_MODE -> removeServerSideInAppsMetaData()
                SERVER_SIDE_MODE -> removeClientSideInApps()
                NO_MODE -> {
                    removeServerSideInAppsMetaData()
                    removeClientSideInApps()
                }
            }
        }

    /**
     * Removes Client-side In-App messages.
     */
    private fun removeClientSideInApps() {
        ctPreference.remove(PREFS_INAPP_KEY_CS)
        clientSideInApps = null
    }

    /**
     * Removes Server-side In-App meta data.
     */
    private fun removeServerSideInAppsMetaData() {
        ctPreference.remove(PREFS_INAPP_KEY_SS)
    }

    /**
     * Stores Client-side In-App messages in encrypted format.
     *
     * @param clientSideInApps The array of Client-side In-App messages.
     */
    fun storeClientSideInApps(clientSideInApps: JSONArray) {
        this.clientSideInApps = clientSideInApps
        val encryptedString =
            cryptHandler.encrypt(clientSideInApps.toString())
        encryptedString?.apply {
            ctPreference.writeString(PREFS_INAPP_KEY_CS, this)
        }
    }

    /**
     * Stores Server-side In-App metadata.
     *
     * @param serverSideInAppsMetaData The array of Server-side In-App metadata.
     */
    fun storeServerSideInAppsMetaData(serverSideInAppsMetaData: JSONArray) {
        ctPreference.writeString(PREFS_INAPP_KEY_SS, serverSideInAppsMetaData.toString())
    }

    /**
     * Stores Server-side legacy In-App messages in encrypted format.
     *
     * @param serverSideInApps The array of Server-side legacy In-App messages.
     */
    fun storeServerSideInApps(serverSideInApps: JSONArray) {
        this.serverSideInApps = serverSideInApps
        val encryptedString =
            cryptHandler.encrypt(serverSideInApps.toString())
        encryptedString?.apply { ctPreference.writeString(INAPP_KEY, this) }
    }

    /**
     * Stores evaluated Server-side In-App IDs.
     *
     * @param evaluatedServerSideInAppIds  The JSoNObject representing the map of EventType - evaluated Server-side In-App IDs.
     */
    fun storeEvaluatedServerSideInAppIds(evaluatedServerSideInAppIds: JSONObject) {
        ctPreference.writeString(PREFS_EVALUATED_INAPP_KEY_SS, evaluatedServerSideInAppIds.toString())
    }

    /**
     * Stores suppressed Client-side In-App IDs.
     *
     * @param suppressedClientSideInAppIds The JSoNObject representing the map of EventType - suppressed Client-side In-App IDs.
     */
    fun storeSuppressedClientSideInAppIds(suppressedClientSideInAppIds: JSONObject) {
        ctPreference.writeString(PREFS_SUPPRESSED_INAPP_KEY_CS, suppressedClientSideInAppIds.toString())
    }

    /**
     * Reads and decrypts Client-side In-App messages.
     *
     * @return An array of Client-side In-App messages.
     */
    fun readClientSideInApps(): JSONArray {
        if (clientSideInApps != null)
            return clientSideInApps as JSONArray

        val csInAppsEncrypted = ctPreference.readString(PREFS_INAPP_KEY_CS, "")
        clientSideInApps = if (csInAppsEncrypted.isNullOrBlank()) {
            JSONArray()
        } else {
            JSONArray(cryptHandler.decrypt(csInAppsEncrypted))
        }
        return clientSideInApps as JSONArray
    }

    /**
     * Reads Server-side In-App metadata.
     *
     * @return An array of Server-side In-App metadata.
     */
    fun readServerSideInAppsMetaData(): JSONArray {
        val ssInAppsMetaData = ctPreference.readString(PREFS_INAPP_KEY_SS, "")

        if (ssInAppsMetaData.isNullOrBlank()) return JSONArray()

        return JSONArray(ssInAppsMetaData)
    }

    /**
     * Reads evaluated Server-side In-App IDs.
     *
     * @return A JSoNObject representing the map of EventType - evaluated Server-side In-App IDs
     */
    fun readEvaluatedServerSideInAppIds(): JSONObject {
        val evaluatedServerSideInAppIds = ctPreference.readString(PREFS_EVALUATED_INAPP_KEY_SS, "")
        if (evaluatedServerSideInAppIds.isNullOrBlank()) return JSONObject()

        return try {
            // Try to convert the string to a JSONObject
            JSONObject(evaluatedServerSideInAppIds)
        } catch (jsonException: JSONException) {
            // If it fails, convert the string to a JSONArray
            val jsonArray = JSONArray(evaluatedServerSideInAppIds)
            // Wrap the JSONArray in a JSONObject
            JSONObject().put(Constants.RAISED, jsonArray)
        }
    }

    /**
     * Reads suppressed Client-side In-App IDs.
     *
     * @return A JSoNObject representing the map of EventType - suppressed Client-side In-App IDs.
     */
    fun readSuppressedClientSideInAppIds(): JSONObject {
        val suppressedClientSideInAppIds = ctPreference.readString(PREFS_SUPPRESSED_INAPP_KEY_CS, "")
        if (suppressedClientSideInAppIds.isNullOrBlank()) return JSONObject()

        return try {
            // Try to convert the string to a JSONObject
            JSONObject(suppressedClientSideInAppIds)
        } catch (jsonException: JSONException) {
            // If it fails, convert the string to a JSONArray
            val jsonArray = JSONArray(suppressedClientSideInAppIds)
            // Wrap the JSONArray in a JSONObject
            JSONObject().put(Constants.RAISED, jsonArray)
        }
    }

    /**
     * Reads and decrypts Server-side legacy In-App messages.
     *
     * @return An array of Server-side legacy In-App messages.
     */
    fun readServerSideInApps(): JSONArray {
        if (serverSideInApps != null)
            return serverSideInApps as JSONArray

        val ssEncryptedInApps = ctPreference.readString(INAPP_KEY, "")
        serverSideInApps = if (ssEncryptedInApps.isNullOrBlank()) {
            JSONArray()
        } else {
            JSONArray(cryptHandler.decrypt(ssEncryptedInApps))
        }

        return serverSideInApps as JSONArray
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
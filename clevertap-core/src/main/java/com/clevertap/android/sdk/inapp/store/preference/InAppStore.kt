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
internal class InAppStore(
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
     * @return An array of Client-side In-App messages.
     */
    fun readClientSideInApps(): JSONArray {
        if (clientSideInApps != null)
            return clientSideInApps as JSONArray

        val csInAppsEncrypted = ctPreference.readString(PREFS_INAPP_KEY_CS, "")
        clientSideInApps = if (csInAppsEncrypted.isNullOrBlank()) {
            JSONArray()
        } else {
            try {
                JSONArray(cryptHandler.decrypt(csInAppsEncrypted))
            } catch (e: Exception) {
                JSONArray()
            }
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
            val raisedArray = oldFormatted.getJSONArray(Constants.RAISED)
            val profileArray = oldFormatted.getJSONArray(Constants.PROFILE)

            return JSONArray().apply {
                for (count in 0 until raisedArray.length())  {
                    put(raisedArray.get(count))
                }
                for (count in 0 until profileArray.length())  {
                    put(profileArray.get(count))
                }
            }

        } catch (e: JSONException) {
            // We cannot migrate, this is no-op branch, we will never get here.
            return JSONArray()
        }
    }

    /**
     * Reads suppressed Client-side In-App IDs.
     *
     * @return A JSoNObject representing the map of EventType - suppressed Client-side In-App IDs.
     */
    fun readSuppressedClientSideInAppIds(): JSONArray {
        val suppressedClientSideInAppIds = ctPreference.readString(PREFS_SUPPRESSED_INAPP_KEY_CS, "")
        if (suppressedClientSideInAppIds.isNullOrBlank()) {
            return JSONArray()
        }

        return try {
            // Try to convert the string to a JSONObject which signifies already migrated
            JSONArray(suppressedClientSideInAppIds)
        } catch (jsonException: JSONException) {
            migrateInAppHeaderPrefsForEventType(suppressedClientSideInAppIds)
        }
    }

    /**
     * Migrates suppressed_ss and evaluated_ss after reading from the prefs.
     * The older format was a JSONArray. This JSoNArray represented the list of all inapps suppressed/evaluated
     * The migrated format is a JSONObject. This JSoNObject has the key as EvenType and the
     * value as the corresponding list of inapps suppressed/evaluated
     *
     * @param - inAppIds to be migrated
     * @return - JSoNObject in the migrated format
     */
    private fun migrateInAppHeaderPrefsForEventType(inAppIds: String): JSONArray {
        try {
            // Old format data from 6.2.1 -> 7.5.1
            // {"raised":[1733462104],"profile":[]}
            val oldJsonObject = JSONObject(inAppIds)

            val raisedArray = oldJsonObject.getJSONArray(Constants.RAISED)
            val profileArray = oldJsonObject.getJSONArray(Constants.PROFILE)

            return JSONArray().apply {
                for (count in 0 until raisedArray.length())  {
                    put(raisedArray.get(count))
                }
                for (count in 0 until profileArray.length())  {
                    put(profileArray.get(count))
                }
            }
        } catch (_: JSONException) {
            // Added for code-completion, we never get here.
            return JSONArray()
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
            try {
                JSONArray(cryptHandler.decrypt(ssEncryptedInApps))
            } catch (e: Exception) {
                JSONArray()
            }
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
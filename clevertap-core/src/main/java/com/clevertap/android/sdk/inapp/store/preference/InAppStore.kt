package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_INAPP_CS
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_INAPP_SS
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_CS
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_SS
import com.clevertap.android.sdk.STORE_TYPE_INAPP
import com.clevertap.android.sdk.StoreProvider
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.login.ChangeUserCallback
import com.clevertap.android.sdk.store.preference.ICTPreference
import org.json.JSONArray

/**
 * The `InAppStore` class manages the storage and retrieval of In-App messages.
 * In-App messages can be stored in either Client-side mode (CS) or Server-side mode (SS).
 *
 * <p>
 * It stores in-apps in the shared preferences named "WizRocket_inapp:<<account_id>>:<<device_id>>"
 * with keys "inapp_notifs_cs" for client-side inApps whereas "inapp_notifs_ss" for server-side inApps.
 * </p>
 *
 * @property context The Android application context.
 * @property cryptHandler The handler used for encryption and decryption of In-App messages.
 * @property accountId The unique account identifier.
 * @property deviceId The unique device identifier.
 */
class InAppStore(
    private val ctPreference: ICTPreference,
    private val cryptHandler: CryptHandler
) : ChangeUserCallback {

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
                CLIENT_SIDE_MODE -> removeServerSideInApps()
                SERVER_SIDE_MODE -> removeClientSideInApps()
                NO_MODE -> {
                    removeServerSideInApps()
                    removeClientSideInApps()
                }
            }
        }

    /**
     * Removes Client-side In-App messages.
     */
    private fun removeClientSideInApps() {
        ctPreference.remove(PREFS_INAPP_KEY_CS)
    }

    /**
     * Removes Server-side In-App messages.
     */
    private fun removeServerSideInApps() {
        ctPreference.remove(PREFS_INAPP_KEY_SS)
    }

    /**
     * Stores Client-side In-App messages in encrypted format.
     *
     * @param clientSideInApps The array of Client-side In-App messages.
     */
    fun storeClientSideInApps(clientSideInApps: JSONArray) {
        val encryptedString =
            cryptHandler.encrypt(clientSideInApps.toString(), KEY_ENCRYPTION_INAPP_CS)
        encryptedString?.apply { ctPreference.writeString(PREFS_INAPP_KEY_CS, this) }
    }

    /**
     * Stores Server-side In-App messages in encrypted format.
     *
     * @param serverSideInApps The array of Server-side In-App messages.
     */
    fun storeServerSideInApps(serverSideInApps: JSONArray) {
        val encryptedString =
            cryptHandler.encrypt(serverSideInApps.toString(), KEY_ENCRYPTION_INAPP_SS)
        encryptedString?.apply { ctPreference.writeString(PREFS_INAPP_KEY_SS, this) }
    }

    /**
     * Reads and decrypts Client-side In-App messages.
     *
     * @return An array of Client-side In-App messages.
     */
    fun readClientSideInApps(): JSONArray {
        val csInAppsEncrypted = ctPreference.readString(PREFS_INAPP_KEY_CS, "")
        if (csInAppsEncrypted.isNullOrBlank()) return JSONArray()

        return JSONArray(cryptHandler.decrypt(csInAppsEncrypted, KEY_ENCRYPTION_INAPP_CS))
    }

    /**
     * Reads and decrypts Server-side In-App messages.
     *
     * @return An array of Server-side In-App messages.
     */
    fun readServerSideInApps(): JSONArray {
        val csInAppsEncrypted = ctPreference.readString(PREFS_INAPP_KEY_SS, "")
        if (csInAppsEncrypted.isNullOrBlank()) return JSONArray()

        return JSONArray(cryptHandler.decrypt(csInAppsEncrypted, KEY_ENCRYPTION_INAPP_SS))
    }

    override fun onChangeUser(deviceId: String, accountId: String) {
        val newPrefName =
            StoreProvider.getInstance().constructStorePreferenceName(STORE_TYPE_INAPP, deviceId, accountId)
        ctPreference.changePreferenceName(newPrefName)
    }
}
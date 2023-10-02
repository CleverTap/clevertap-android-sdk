package com.clevertap.android.sdk.inapp

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.Constants.INAPP_KEY
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_INAPP_CS
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_INAPP_SS
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_CS
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_SS
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.cryption.CryptHandler
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
    private val context: Context,
    private val cryptHandler: CryptHandler,
    accountId: String,
    deviceId: String
) {

    companion object {
        const val CLIENT_SIDE_MODE = "CS"
        const val SERVER_SIDE_MODE = "SS"
    }

    val prefName =
        "$INAPP_KEY:$accountId:$deviceId" //TODO Reuse of old INAPP_KEY with combo of accountId & deviceId??

    /**
     * The mode in which In-App messages are stored. Set to either [CLIENT_SIDE_MODE] or [SERVER_SIDE_MODE].
     *
     * <p>
     * If the mode changes from Server-side (SS) to Client-side (CS), all Server-side In-App messages
     * are cleaned. If the mode changes from Client-side (CS) to Server-side (SS), all Client-side In-App
     * messages are cleaned. If no flag is returned (null), all Client-side and Server-side In-App messages
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
                else -> {
                    removeServerSideInApps()
                    removeClientSideInApps()
                }
            }
        }

    /**
     * Removes Client-side In-App messages.
     */
    private fun removeClientSideInApps() {
        sharedPrefs().edit().remove(PREFS_INAPP_KEY_CS).apply()
    }

    /**
     * Removes Server-side In-App messages.
     */
    private fun removeServerSideInApps() {
        sharedPrefs().edit().remove(PREFS_INAPP_KEY_SS).apply()
    }

    /**
     * Stores Client-side In-App messages in encrypted format.
     *
     * @param clientSideInApps The array of Client-side In-App messages.
     */
    fun storeClientSideInApps(clientSideInApps: JSONArray) {
        val encryptedString =
            cryptHandler.encrypt(clientSideInApps.toString(), KEY_ENCRYPTION_INAPP_CS)
        sharedPrefs().edit().putString(PREFS_INAPP_KEY_CS, encryptedString).apply()
    }

    /**
     * Stores Server-side In-App messages in encrypted format.
     *
     * @param serverSideInApps The array of Server-side In-App messages.
     */
    fun storeServerSideInApps(serverSideInApps: JSONArray) {
        val encryptedString =
            cryptHandler.encrypt(serverSideInApps.toString(), KEY_ENCRYPTION_INAPP_SS)
        sharedPrefs().edit().putString(PREFS_INAPP_KEY_SS, encryptedString).apply()
    }

    /**
     * Reads and decrypts Client-side In-App messages.
     *
     * @return An array of Client-side In-App messages.
     */
    fun readClientSideInApps(): JSONArray {
        val encryptedString =
            sharedPrefs().getString(PREFS_INAPP_KEY_CS, null) ?: return JSONArray()
        return JSONArray(cryptHandler.decrypt(encryptedString, KEY_ENCRYPTION_INAPP_CS))
    }

    /**
     * Reads and decrypts Server-side In-App messages.
     *
     * @return An array of Server-side In-App messages.
     */
    fun readServerSideInApps(): JSONArray {
        val encryptedString =
            sharedPrefs().getString(PREFS_INAPP_KEY_SS, null) ?: return JSONArray()

        return JSONArray(cryptHandler.decrypt(encryptedString, KEY_ENCRYPTION_INAPP_SS))
    }

    /**
     * Retrieves the shared preferences for In-App messages.
     *
     * @return The shared preferences for In-App messages, or `null` if unavailable.
     */
    fun sharedPrefs(): SharedPreferences {
        return StorageHelper.getPreferences(context, prefName)
    }
}
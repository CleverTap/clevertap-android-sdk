package com.clevertap.android.sdk.network

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.StorageHelper
import androidx.core.content.edit
import com.clevertap.android.sdk.DeviceInfo
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ArpRepo(
    private val accountId: String,
    private val logger: Logger,
    private val deviceInfo: DeviceInfo
) {

    private val newNamespaceARPKey: String?
        get() {
            val accountId = accountId ?: return null

            logger.verbose(
                accountId,
                "New ARP Key = ARP:" + accountId + ":" + deviceInfo.deviceID
            )
            return "ARP:" + accountId + ":" + deviceInfo.deviceID
        }

    private val namespaceARPKey: String?
        //Session
        get() {
            val accountId = accountId ?: return null

            logger.verbose(accountId, "Old ARP Key = ARP:$accountId")
            return "ARP:$accountId"
        }

    /**
     * The ARP is additional request parameters, which must be sent once
     * received after any HTTP call. This is sort of a proxy for cookies.
     *
     * @return A JSON object containing the ARP key/values. Can be null.
     */
    fun getARP(context: Context): JSONObject? {
            try {
                val nameSpaceKey = newNamespaceARPKey ?: return null

                //checking whether new namespace is empty or not
                //if not empty, using prefs of new namespace to send ARP
                //if empty, checking for old prefs
                val newPrefsFile = StorageHelper.getPreferences(context, nameSpaceKey)
                val prefs =
                    if (newPrefsFile.all.isNotEmpty()) {
                        //prefs point to new namespace
                        newPrefsFile
                    } else {
                        //prefs point to new namespace migrated from old namespace
                        migrateARPToNewNameSpace(context, nameSpaceKey, namespaceARPKey)
                    }

                val all = prefs.all
                removeUnsetEntries(all)
                val ret = JSONObject(all)
                logger.verbose(
                    accountId,
                    "Fetched ARP for namespace key: $nameSpaceKey values: $all"
                )
                return ret
            } catch (e: Exception) {
                logger.verbose(accountId, "Failed to construct ARP object", e)
                return null
            }
        }

    /** Drops entries whose value was set to -1, which marks them as removed. */
    private fun removeUnsetEntries(all: MutableMap<String, *>) {
        val iter = all.entries.iterator()

        while (iter.hasNext()) {
            val kv = iter.next()
            val o = kv.value!!
            if (o is Number && o.toInt() == -1) {
                iter.remove()
            }
        }
    }

    /**
     * Saves ARP directly to new namespace
     */
    fun handleARPUpdate(context: Context, arp: JSONObject) {
        if (arp.length() == 0) {
            return
        }

        val nameSpaceKey = newNamespaceARPKey ?: return

        val prefs = StorageHelper.getPreferences(context, nameSpaceKey)
        val editor = prefs.edit()

        val keys = arp.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            try {
                val o = arp.get(key)
                putArpValue(editor = editor, key = key, value = o)
            } catch (e: Exception) {
                // Ignore
            }
        }
        logger.verbose(accountId,
                "Stored ARP for namespace key: $nameSpaceKey values: $arp")
        StorageHelper.persist(editor)
    }

    private fun migrateARPToNewNameSpace(
        context: Context,
        newKey: String,
        oldKey: String?
    ): SharedPreferences {
        val oldPrefs = StorageHelper.getPreferences(context, oldKey)
        val newPrefs = StorageHelper.getPreferences(context, newKey)
        val editor = newPrefs.edit()
        val all = oldPrefs.all

        for ((key, value) in all) {
            val o = value!!
            putArpValue(editor = editor, key = key, value = o)
        }
        logger.verbose(accountId, "Completed ARP update for namespace key: $newKey")
        StorageHelper.persist(editor)
        oldPrefs.edit { clear() }
        return newPrefs
    }

    /**
     * Writes one ARP value: numbers as Int, strings shorter than 100 chars, booleans. Anything
     * else is rejected with a log.
     */
    private fun putArpValue(editor: SharedPreferences.Editor, key: String, value: Any) {
        if (value is Number) {
            val update = value.toInt()
            editor.putInt(key, update)
        } else if (value is String) {
            if (value.length < 100) {
                editor.putString(key, value)
            } else {
                logger.verbose(
                    accountId,
                    "ARP update for key $key rejected (string value too long)"
                )
            }
        } else if (value is Boolean) {
            editor.putBoolean(key, value)
        } else {
            logger.verbose(
                accountId,
                "ARP update for key $key rejected (invalid data type)"
            )
        }
    }
}

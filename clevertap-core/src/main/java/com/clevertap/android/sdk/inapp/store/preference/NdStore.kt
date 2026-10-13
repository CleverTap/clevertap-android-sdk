package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants.PREFS_EVALUATED_ND_KEY_SS
import com.clevertap.android.sdk.Constants.PREFS_ND_KEY_SS
import com.clevertap.android.sdk.Constants.PREFS_SUPPRESSED_ND_KEY
import com.clevertap.android.sdk.store.preference.ICTPreference
import com.clevertap.android.sdk.toList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Stores Native Display (ND) Server-Side state, mirroring the SS half of [InAppStore].
 *
 * ND is **SS + legacy only** — there is no client-side (CS) mode, so unlike [InAppStore] there is no
 * encrypted content cache and no delivery-mode switching. This store holds:
 *  - the advanced-rule metadata bundle (`adUnit_notifs_ss`, rules only — used for local eval),
 *  - the pending `adUnit_eval` vote list, and
 *  - the pending `adUnit_suppressed` CG-ack list.
 *
 * Prefs file: `WizRocket_adUnit:<deviceId>:<accountId>`.
 *
 * Unlike [InAppStore], this store is **not** a [com.clevertap.android.sdk.login.ChangeUserCallback]:
 * its user-switch lifecycle is owned by [NdStoreProvider], which builds a fresh instance (pointed at the
 * new user's prefs, with an empty [metaCache]) on the next access after the device id changes. Do not
 * re-add a change-user callback here — the provider is the single owner.
 */
internal class NdStore(
    private val ctPreference: ICTPreference,
) {

    private var metaCache: List<JSONObject>? = null

    /**
     * Stores the ND advanced-rule metadata bundle (`adUnit_notifs_ss`). Plaintext — rules only, no content.
     */
    fun storeServerSideNdMetaData(metaData: List<JSONObject>) {
        metaCache = metaData
        ctPreference.writeString(PREFS_ND_KEY_SS, JSONArray(metaData).toString())
    }

    /**
     * Reads the ND advanced-rule metadata bundle.
     */
    fun readServerSideNdMetaData(): List<JSONObject> {
        metaCache?.let { return it }

        val stored = ctPreference.readString(PREFS_ND_KEY_SS, "")
        val result = if (stored.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                JSONArray(stored).toList<JSONObject>()
            } catch (e: JSONException) {
                emptyList()
            }
        }
        metaCache = result
        return result
    }

    /**
     * Removes the ND metadata bundle (used by dead-target GC / clear paths).
     */
    fun removeServerSideNdMetaData() {
        ctPreference.remove(PREFS_ND_KEY_SS)
        metaCache = null
    }

    /**
     * Stores the pending `adUnit_eval` ids (eligible advanced ND campaigns not yet reported).
     */
    fun storeEvaluatedServerSideNdIds(evaluatedIds: JSONArray) {
        ctPreference.writeString(PREFS_EVALUATED_ND_KEY_SS, evaluatedIds.toString())
    }

    /**
     * Reads the pending `adUnit_eval` ids.
     */
    fun readEvaluatedServerSideNdIds(): JSONArray {
        val stored = ctPreference.readString(PREFS_EVALUATED_ND_KEY_SS, "")
        if (stored.isNullOrBlank()) return JSONArray()
        return try {
            JSONArray(stored)
        } catch (e: JSONException) {
            JSONArray()
        }
    }

    /**
     * Stores the pending `adUnit_suppressed` CG acks (App-Launched path only).
     */
    fun storeSuppressedNdIds(suppressedIds: JSONArray) {
        ctPreference.writeString(PREFS_SUPPRESSED_ND_KEY, suppressedIds.toString())
    }

    /**
     * Reads the pending `adUnit_suppressed` CG acks.
     */
    fun readSuppressedNdIds(): JSONArray {
        val stored = ctPreference.readString(PREFS_SUPPRESSED_ND_KEY, "")
        if (stored.isNullOrBlank()) return JSONArray()
        return try {
            JSONArray(stored)
        } catch (e: JSONException) {
            JSONArray()
        }
    }
}

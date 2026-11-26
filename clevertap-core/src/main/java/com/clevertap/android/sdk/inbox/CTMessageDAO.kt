package com.clevertap.android.sdk.inbox

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Message Data Access Object class interfacing with Database
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class CTMessageDAO() {

    var campaignId: String? = null

    var date: Long = 0

    var expires: Long = 0

    private var _id: String? = null

    /**
     * Message id. Always set for a DAO that came from [initWithJSON] or the database; reading it
     * before it is set throws, where the Java getter returned null.
     */
    var id: String
        get() = checkNotNull(_id) { "CTMessageDAO id read before it was set" }
        set(value) {
            _id = value
        }

    var jsonData: JSONObject? = null

    private var read = false

    private val tagsList: MutableList<String> = ArrayList()

    var userId: String? = null

    var wzrkParams: JSONObject? = null

    internal var source: InboxMessageSource? = null

    /**
     * Local-state tag for the cross-device delete sweep — never parsed from
     * server JSON, never written to [toJSON]. Defaults to
     * [InboxIndexState.PENDING_INDEXING]; the controller flips rows to
     * [InboxIndexState.INDEXED] via the DAO's `markIndexed`
     * once the fetch backend has confirmed them.
     */
    var indexState: String? = InboxIndexState.PENDING_INDEXING

    internal open fun containsVideoOrAudio(): Boolean {
        Logger.d("CTMessageDAO:containsVideoOrAudio() called")
        val content = CTInboxMessage(toJSON()).inboxMessageContents[0]
        return content.mediaIsVideo() || content.mediaIsAudio()
    }

    /**
     * Tags joined with ",". Setting splits the value on "," and appends the parts to the
     * existing tags.
     */
    var tags: String
        get() = tagsList.joinToString(",")
        set(tags) {
            // java.lang.String.split semantics (trailing empty strings dropped), as before.
            tagsList.addAll(Pattern.compile(",").split(tags))
        }

    /** 1 when read, 0 otherwise (the database column's value). */
    var isRead: Int
        get() = if (read) { 1 } else { 0 }
        set(value) {
            read = value == 1
        }

    fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        return try {
            jsonObject.apply {
                put("id", _id)
                put("msg", jsonData)
                put("isRead", read)
                put("date", date)
                put("wzrk_ttl", expires)
                put("tags", JSONArray(tagsList))
                put("wzrk_id", campaignId)
                put("wzrkParams", wzrkParams)
            }
        } catch (e: JSONException) {
            Logger.v("Unable to convert CTMessageDao to JSON - ${e.localizedMessage}")
            jsonObject
        }
    }

    companion object {

        internal fun initWithJSON(
            inboxMessage: JSONObject,
            userId: String?,
            source: InboxMessageSource?
        ): CTMessageDAO? {
            return try {
                val id = if (inboxMessage.has("_id")) { inboxMessage.getString("_id") } else { null }
                val date: Long = if (inboxMessage.has("date")) {
                    inboxMessage.getInt("date").toLong()
                } else {
                    System.currentTimeMillis() / 1000
                }
                val expires: Long = if (inboxMessage.has("wzrk_ttl")) {
                    inboxMessage.getInt("wzrk_ttl").toLong()
                } else {
                    (System.currentTimeMillis() + 24 * 60 * Constants.ONE_MIN_IN_MILLIS) / 1000
                }
                val cellObject = if (inboxMessage.has("msg")) { inboxMessage.getJSONObject("msg") } else { null }
                val tagsList = ArrayList<String>()
                //Part of "msg" object
                val tagsArray = if (cellObject?.has("tags") == true) { cellObject.getJSONArray("tags") } else { null }
                if (tagsArray != null) {
                    for (i in 0 until tagsArray.length()) {
                        tagsList.add(tagsArray.getString(i))
                    }
                }
                val campaignId = if (inboxMessage.has("wzrk_id")) { inboxMessage.getString("wzrk_id") } else { "0_0" }
                if (campaignId.equals("0_0", ignoreCase = true)) {
                    inboxMessage.put("wzrk_id", campaignId) //For test inbox Notification Viewed
                }
                val wzrkParams = wzrkFieldsOf(inboxMessage)
                val read = inboxMessage.optBoolean(Constants.INBOX_V2_ISREAD_KEY, false)
                id ?: return null
                CTMessageDAO().also {
                    it._id = id
                    it.jsonData = cellObject
                    it.read = read
                    it.date = date
                    it.expires = expires
                    it.userId = userId
                    it.tagsList.addAll(tagsList)
                    it.campaignId = campaignId
                    it.wzrkParams = wzrkParams
                    it.source = source
                }
            } catch (e: JSONException) {
                Logger.d("Unable to parse Notification inbox message to CTMessageDao - ${e.localizedMessage}")
                null
            }
        }

        private fun wzrkFieldsOf(root: JSONObject): JSONObject {
            val fields = JSONObject()
            for (keyName in root.keys()) {
                if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                    fields.put(keyName, root.get(keyName))
                }
            }
            return fields
        }
    }
}

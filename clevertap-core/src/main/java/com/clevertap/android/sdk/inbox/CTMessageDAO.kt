package com.clevertap.android.sdk.inbox

import android.text.TextUtils
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
@RestrictTo(Scope.LIBRARY)
open class CTMessageDAO() {

    var campaignId: String? = null

    var date: Long = 0

    var expires: Long = 0

    private var _id: String? = null

    /**
     * Message id. Always set for a DAO that came from [initWithJSON] or the database; reading it
     * before it is set throws, where the Java getter returned null.
     */
    var id: String
        get() = _id!!
        set(value) {
            _id = value
        }

    var jsonData: JSONObject? = null

    private var read = false

    private var tagsList: MutableList<String> = ArrayList()

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

    private constructor(
        id: String?, jsonData: JSONObject?, read: Boolean, date: Long, expires: Long, userId: String?,
        tags: MutableList<String>, campaignId: String?, wzrkParams: JSONObject?, source: InboxMessageSource?
    ) : this() {
        this._id = id
        this.jsonData = jsonData
        this.read = read
        this.date = date
        this.expires = expires
        this.userId = userId
        this.tagsList = tags
        this.campaignId = campaignId
        this.wzrkParams = wzrkParams
        this.source = source
    }

    internal open fun containsVideoOrAudio(): Boolean {
        Logger.d("CTMessageDAO:containsVideoOrAudio() called")
        val content = CTInboxMessage(this.toJSON()).inboxMessageContents[0]
        return content.mediaIsVideo() || content.mediaIsAudio()
    }

    /**
     * Tags joined with ",". Setting splits the value on "," and appends the parts to the
     * existing tags.
     */
    var tags: String
        get() = TextUtils.join(",", tagsList)
        set(tags) {
            // java.lang.String.split semantics (trailing empty strings dropped), as before.
            val tagsArray = Pattern.compile(",").split(tags)
            this.tagsList.addAll(tagsArray.asList())
        }

    fun isRead(): Int {
        return if (read) {
            1
        } else {
            0
        }
    }

    fun setRead(read: Int) {
        this.read = read == 1
    }

    /** Kotlin property syntax, equivalent to [isRead] / [setRead] (1 when read, 0 otherwise). */
    var isRead: Int
        @JvmSynthetic @JvmName("ktIsRead") inline get() = isRead()
        @JvmSynthetic @JvmName("ktSetRead") inline set(value) = setRead(value)

    fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        return try {
            jsonObject.put("id", this._id)
            jsonObject.put("msg", this.jsonData)
            jsonObject.put("isRead", this.read)
            jsonObject.put("date", this.date)
            jsonObject.put("wzrk_ttl", this.expires)
            val jsonArray = JSONArray()
            for (i in this.tagsList.indices) {
                jsonArray.put(tagsList[i])
            }
            jsonObject.put("tags", jsonArray)
            jsonObject.put("wzrk_id", campaignId)
            jsonObject.put("wzrkParams", wzrkParams)
            jsonObject
        } catch (e: JSONException) {
            Logger.v("Unable to convert CTMessageDao to JSON - " + e.localizedMessage)
            jsonObject
        }
    }

    companion object {

        @JvmStatic
        internal fun initWithJSON(inboxMessage: JSONObject, userId: String?, source: InboxMessageSource?): CTMessageDAO? {
            return try {
                val id = if (inboxMessage.has("_id")) inboxMessage.getString("_id") else null
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
                val cellObject = if (inboxMessage.has("msg")) inboxMessage.getJSONObject("msg") else null
                val tagsList: MutableList<String> = ArrayList()
                if (cellObject != null) { //Part of "msg" object
                    val tagsArray = if (cellObject.has("tags")) cellObject.getJSONArray("tags") else null
                    if (tagsArray != null) {
                        for (i in 0 until tagsArray.length()) {
                            tagsList.add(tagsArray.getString(i))
                        }
                    }
                }
                val campaignId = if (inboxMessage.has("wzrk_id")) inboxMessage.getString("wzrk_id") else "0_0"
                if (campaignId.equals("0_0", ignoreCase = true)) {
                    inboxMessage.put("wzrk_id", campaignId) //For test inbox Notification Viewed
                }
                val wzrkParams = getWzrkFields(inboxMessage)
                val read = inboxMessage.optBoolean(Constants.INBOX_V2_ISREAD_KEY, false)
                if (id == null) {
                    null
                } else {
                    CTMessageDAO(
                        id, cellObject, read, date, expires, userId, tagsList, campaignId,
                        wzrkParams, source
                    )
                }
            } catch (e: JSONException) {
                Logger.d("Unable to parse Notification inbox message to CTMessageDao - " + e.localizedMessage)
                null
            }
        }

        @Throws(JSONException::class)
        private fun getWzrkFields(root: JSONObject): JSONObject {
            val fields = JSONObject()
            val iterator = root.keys()

            while (iterator.hasNext()) {
                val keyName = iterator.next()
                if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                    fields.put(keyName, root.get(keyName))
                }
            }

            return fields
        }
    }
}

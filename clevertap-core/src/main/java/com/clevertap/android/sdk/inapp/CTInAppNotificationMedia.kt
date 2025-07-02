package com.clevertap.android.sdk.inapp

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.Constants
import org.json.JSONObject
import java.util.UUID

internal class CTInAppNotificationMedia : Parcelable {

    var mediaUrl: String
    val contentType: String
    val contentDescription: String
    val cacheKey: String?
    val orientation: Int

    constructor(
        mediaUrl: String,
        contentType: String,
        contentDescription: String,
        cacheKey: String?,
        orientation: Int
    ) {
        this.mediaUrl = mediaUrl
        this.contentType = contentType
        this.contentDescription = contentDescription
        this.cacheKey = cacheKey
        this.orientation = orientation
    }

    private constructor(parcel: Parcel) {
        mediaUrl = parcel.readString() ?: ""
        contentType = parcel.readString() ?: ""
        contentDescription = parcel.readString() ?: ""
        cacheKey = parcel.readString()
        orientation = parcel.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(mediaUrl)
        dest.writeString(contentType)
        dest.writeString(contentDescription)
        dest.writeString(cacheKey)
        dest.writeInt(orientation)
    }

    fun isAudio(): Boolean {
        return mediaUrl.isNotBlank() && contentType.startsWith("audio")
    }

    fun isGIF(): Boolean {
        return mediaUrl.isNotBlank() && contentType == "image/gif"
    }

    fun isImage(): Boolean {
        return mediaUrl.isNotBlank() && contentType.startsWith("image") && (contentType != "image/gif")
    }

    fun isVideo(): Boolean {
        return mediaUrl.isNotBlank() && contentType.startsWith("video")
    }

    fun isMediaStreamable(): Boolean {
        return isVideo() || isAudio()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CTInAppNotificationMedia

        if (orientation != other.orientation) return false
        if (mediaUrl != other.mediaUrl) return false
        if (contentType != other.contentType) return false
        if (contentDescription != other.contentDescription) return false
        if (cacheKey != other.cacheKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = orientation
        result = 31 * result + mediaUrl.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + contentDescription.hashCode()
        result = 31 * result + (cacheKey?.hashCode() ?: 0)
        return result
    }

    companion object {

        @JvmField
        val CREATOR = object : Parcelable.Creator<CTInAppNotificationMedia> {
            override fun createFromParcel(parcel: Parcel): CTInAppNotificationMedia {
                return CTInAppNotificationMedia(parcel)
            }

            override fun newArray(size: Int): Array<CTInAppNotificationMedia?> {
                return arrayOfNulls(size)
            }
        }

        @JvmStatic
        fun create(json: JSONObject, orientation: Int): CTInAppNotificationMedia? {
            val contentType = json.optString(Constants.KEY_CONTENT_TYPE)
            if (contentType.isBlank()) {
                return null
            }
            val mediaUrl = json.optString(Constants.KEY_URL)

            var cacheKey: String? = null
            if (mediaUrl.isNotBlank()) {
                if (contentType.startsWith("image")) {
                    cacheKey = UUID.randomUUID().toString() + json.optString(Constants.KEY_KEY)
                }
            }
            val contentDescription = json.optString(Constants.KEY_ALT_TEXT)
            return CTInAppNotificationMedia(
                mediaUrl,
                contentType,
                contentDescription,
                cacheKey,
                orientation
            )
        }
    }
}

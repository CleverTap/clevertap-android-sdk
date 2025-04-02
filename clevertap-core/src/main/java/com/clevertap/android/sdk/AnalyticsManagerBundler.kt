package com.clevertap.android.sdk

import android.os.Bundle
import org.json.JSONException
import org.json.JSONObject

object AnalyticsManagerBundler {

    @Throws(JSONException::class)
    @JvmStatic
    fun wzrkBundleToJson(root: Bundle): JSONObject {
        val fields = JSONObject()
        for (s in root.keySet()) {
            val o = root[s]
            if (o is Bundle) {
                val wzrkFields = wzrkBundleToJson(o)
                val keys = wzrkFields.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    fields.put(k, wzrkFields[k])
                }
            } else if (s.startsWith(Constants.WZRK_PREFIX)) {
                fields.put(s, root[s])
            }
        }

        return fields
    }

    @JvmStatic
    fun notificationViewedJson(root: Bundle): JSONObject {
        val event = JSONObject()
        try {
            val notif = wzrkBundleToJson(root)
            event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)
            event.put("evtData", notif)
        } catch (ignored: Throwable) {
            //no-op
        }
        return event
    }

    @JvmStatic
    fun notificationClickedJson(root: Bundle): JSONObject {
        val event = JSONObject()
        try {
            val notif = wzrkBundleToJson(root)
            event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME)
            event.put("evtData", notif)
        } catch (ignored: Throwable) {
            //no-op
        }
        return event
    }
}
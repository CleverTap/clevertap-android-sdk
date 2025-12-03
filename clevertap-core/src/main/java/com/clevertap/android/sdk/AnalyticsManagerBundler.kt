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
    fun notificationViewedJson(root: JSONObject): JSONObject {
        val event = JSONObject()
        try {
            event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)
            event.put("evtData", root)
        } catch (ignored: Throwable) {
            //no-op
        }
        return event
    }

    @JvmStatic
    fun notificationClickedJson(root: JSONObject): JSONObject {
        val event = JSONObject()
        try {
            event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME)
            event.put("evtData", root)
        } catch (ignored: Throwable) {
            //no-op
        }
        return event
    }
}
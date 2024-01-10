package com.clevertap.android.sdk.db

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class QueueData(var table: Table) {

    var data: JSONArray? = null // the db objects
    var lastId: String? = null // the id of the last object returned from the db, used to remove sent objects

    val isEmpty: Boolean
        get() {
            val data = data
            return lastId == null || data == null || data.length() <= 0
        }

    /**
     * Calling this function will set items from dbObject as [data].
     * If dbObject is null, nothing will be changed
     * else
     *  - it will try taking the first key and set it at as [lastId]
     *  - it will try taking the value of first key, cast it as jsonArray and set it as [data]
     *
     * @param dbObject JSON, must be of format `{string : jsonArray }`
     * */
    fun setDataFromDbObject(dbObject: JSONObject?) {
        if (dbObject == null) {
            return
        }
        val keys = dbObject.keys()
        if (keys.hasNext()) {
            val key = keys.next()
            lastId = key
            try {
                data = dbObject.getJSONArray(key)
            } catch (e: JSONException) {
                lastId = null
                data = null
            }
        }
    }

    override fun toString(): String {
        val numItems = data?.length() ?: 0
        return if (isEmpty) {
            "table: $table | numItems: $numItems"
        } else {
            "table: $table | lastId: $lastId | numItems: $numItems | items: ${data.toString()}"
        }
    }
}

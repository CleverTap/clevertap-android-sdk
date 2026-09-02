package com.clevertap.android.pushtemplates.styles

import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.Utils
import org.json.JSONArray

/**
 * Pure parsing of the `pt_progress_segments` / `pt_progress_points` JSON and hex colors, extracted
 * from [ProgressStyle] so it is unit-testable without building a Notification. All methods are
 * tolerant: malformed / missing input yields sensible empties/nulls and never throws.
 */
internal object ProgressPayloadParser {

    data class SegmentData(val length: Int, val color: Int?)
    data class PointData(val position: Int, val color: Int?)

    /** Each element: `{ "length": Int (default 1), "color": "#RRGGBB" (optional) }`. */
    fun parseSegments(json: String?): List<SegmentData> {
        val out = ArrayList<SegmentData>()
        if (json.isNullOrEmpty()) return out
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(SegmentData(o.optInt("length", 1), Utils.getColourOrNull(o.optString("color"))))
            }
        } catch (t: Throwable) {
            PTLog.verbose("pt_progress: failed to parse segments", t)
        }
        return out
    }

    /** Each element: `{ "position": Int (default 0), "color": "#RRGGBB" (optional) }`. */
    fun parsePoints(json: String?): List<PointData> {
        val out = ArrayList<PointData>()
        if (json.isNullOrEmpty()) return out
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(PointData(o.optInt("position", 0), Utils.getColourOrNull(o.optString("color"))))
            }
        } catch (t: Throwable) {
            PTLog.verbose("pt_progress: failed to parse points", t)
        }
        return out
    }
}

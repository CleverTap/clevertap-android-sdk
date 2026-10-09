package com.clevertap.demo

import android.util.Log
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit

const val ND_TAG = "ND"

/**
 * Prints what the SDK parsed for every slide of this unit: the BE's `metadata` block
 * (wzrk_element_id, wzrk_index, wzrk_c2a) merged with the wzrk_action / wzrk_data the
 * SDK derives from the slide's Android url.
 *
 * "metaData EMPTY" means the payload carried no `metadata` for that slide, so an
 * element-level event on it records unit-level wzrk_* only.
 */
fun CleverTapDisplayUnit.logSlides() {
    val slides = contents ?: arrayListOf()
    Log.i(ND_TAG, "unit=$unitID type=$type slides=${slides.size}")
    slides.forEachIndexed { index, content ->
        Log.i(ND_TAG, "  slide $index title=${content.title} actionUrl=${content.actionUrl}")
        val metaData = getMetaDataForContent(index)
        if (metaData.isEmpty()) {
            Log.i(ND_TAG, "  slide $index metaData EMPTY - no `metadata` object on this content item")
        } else {
            metaData.toSortedMap().forEach { (key, value) ->
                Log.i(ND_TAG, "  slide $index $key=$value")
            }
        }
    }
}

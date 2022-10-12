package com.clevertap.android.sdk

import android.app.Activity

class CTStringResources(private val context: Activity, vararg sRID: Int) {

    private var sArray: Array<String>

    init {
        sArray = Array(sRID.size) { context.getString(sRID[it]) }
    }

    operator fun component1(): String? = sArray.getOrNull(0)
    operator fun component2(): String? = sArray.getOrNull(1)
    operator fun component3(): String? = sArray.getOrNull(2)
    operator fun component4(): String? = sArray.getOrNull(3)
    operator fun component5(): String? = sArray.getOrNull(4)
}
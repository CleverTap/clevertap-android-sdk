package com.clevertap.android.sdk.events

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class EventDetail(
    count: Int,
    firstTime: Int,
    lastTime: Int,
    name: String?
) {

    private val countValue: Int = count

    private val firstTimeValue: Int = firstTime

    private val lastTimeValue: Int = lastTime

    private val nameValue: String? = name

    open fun getCount(): Int = countValue

    open fun getFirstTime(): Int = firstTimeValue

    open fun getLastTime(): Int = lastTimeValue

    open fun getName(): String? = nameValue

    /** Kotlin property syntax, equivalent to [getCount]. */
    val count: Int
        @JvmSynthetic @JvmName("ktGetCount") get() = getCount()

    /** Kotlin property syntax, equivalent to [getFirstTime]. */
    val firstTime: Int
        @JvmSynthetic @JvmName("ktGetFirstTime") get() = getFirstTime()

    /** Kotlin property syntax, equivalent to [getLastTime]. */
    val lastTime: Int
        @JvmSynthetic @JvmName("ktGetLastTime") get() = getLastTime()

    /** Kotlin property syntax, equivalent to [getName]. */
    val name: String?
        @JvmSynthetic @JvmName("ktGetName") get() = getName()
}

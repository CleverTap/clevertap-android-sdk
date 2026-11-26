package com.clevertap.android.sdk.events

/**
 * Count and first/last occurrence times of one event, as returned by the deprecated
 * [com.clevertap.android.sdk.CleverTapAPI.getDetails]. Times are Unix epoch seconds.
 *
 * @see com.clevertap.android.sdk.CleverTapAPI.getUserEventLog
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
public open class EventDetail(
    count: Int,
    firstTime: Int,
    lastTime: Int,
    name: String?
) {

    private val countValue: Int = count

    private val firstTimeValue: Int = firstTime

    private val lastTimeValue: Int = lastTime

    private val nameValue: String? = name

    /**
     * Returns how many times the event was recorded.
     *
     * @return the event count
     */
    public open fun getCount(): Int = countValue

    /**
     * Returns when the event was first recorded.
     *
     * @return the first occurrence, in Unix epoch seconds
     */
    public open fun getFirstTime(): Int = firstTimeValue

    /**
     * Returns when the event was last recorded.
     *
     * @return the last occurrence, in Unix epoch seconds
     */
    public open fun getLastTime(): Int = lastTimeValue

    /**
     * Returns the event name.
     *
     * @return the event name
     */
    public open fun getName(): String? = nameValue

    /** Kotlin property syntax, equivalent to [getCount]. */
    public val count: Int
        @JvmSynthetic @JvmName("ktGetCount") inline get() = getCount()

    /** Kotlin property syntax, equivalent to [getFirstTime]. */
    public val firstTime: Int
        @JvmSynthetic @JvmName("ktGetFirstTime") inline get() = getFirstTime()

    /** Kotlin property syntax, equivalent to [getLastTime]. */
    public val lastTime: Int
        @JvmSynthetic @JvmName("ktGetLastTime") inline get() = getLastTime()

    /** Kotlin property syntax, equivalent to [getName]. */
    public val name: String?
        @JvmSynthetic @JvmName("ktGetName") inline get() = getName()
}

package com.clevertap.android.sdk.events

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class EventDetail(
    open val count: Int,
    open val firstTime: Int,
    open val lastTime: Int,
    open val name: String?
)

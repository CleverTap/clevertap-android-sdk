package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.SessionManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventMediator

/**
 * Analytics and event tracking
 * Manages event queues, analytics, and user sessions
 */
data class AnalyticsFeature(
    val analyticsManager: AnalyticsManager,
    val baseEventQueueManager: BaseEventQueueManager,
    val eventMediator: EventMediator,
    val sessionManager: SessionManager
)

package com.clevertap.android.sdk.events

import com.clevertap.android.sdk.profile.merge.ProfileChange

sealed interface FlattenedEventData {
    data class ProfileChanges(val changes: Map<String, ProfileChange>) : FlattenedEventData
    data class EventProperties(val properties: Map<String, Any>) : FlattenedEventData
    data object NoData : FlattenedEventData
}
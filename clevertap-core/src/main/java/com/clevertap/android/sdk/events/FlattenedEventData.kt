package com.clevertap.android.sdk.events

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.profile.traversal.ProfileChange

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed interface FlattenedEventData {
    data class ProfileChanges(val changes: Map<String, ProfileChange>) : FlattenedEventData
    data class EventProperties(val properties: Map<String, Any>) : FlattenedEventData
    data object NoData : FlattenedEventData
}
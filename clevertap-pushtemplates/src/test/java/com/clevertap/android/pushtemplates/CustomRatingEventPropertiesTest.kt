package com.clevertap.android.pushtemplates

import android.os.Bundle
import com.clevertap.android.sdk.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers the Rating Submitted properties the pt_custom_rating template sends (FR-EVT-01): the
 * campaign attribution the Classic template already carried, plus the typed rating properties that
 * journeys and segments are built on.
 */
@RunWith(RobolectricTestRunner::class)
class CustomRatingEventPropertiesTest {

    private fun campaignExtras() = Bundle().apply {
        putString(PTConstants.PT_ID, "pt_custom_rating")
        putString("wzrk_id", "1234_20260825")
        putString(Constants.KEY_C2A, PTConstants.PT_RATING_C2A_KEY + "4")
        putString(PTConstants.PT_TITLE, "How did we do?")
    }

    @Test
    fun `typed rating properties are sent alongside the campaign attribution`() {
        val props = Utils.getCustomRatingEventProperties(campaignExtras(), 4, 5, "icon")

        assertEquals(4, props[PTConstants.PT_RATING_EVENT_VALUE])
        assertEquals(5, props[PTConstants.PT_RATING_EVENT_SCALE])
        assertEquals("icon", props[PTConstants.PT_RATING_EVENT_STYLE])
        assertEquals(PTConstants.PT_RATING_C2A_KEY + "4", props[Constants.KEY_C2A])
        assertEquals("1234_20260825", props["wzrk_id"])
        assertEquals("pt_custom_rating", props[PTConstants.PT_ID])
    }

    @Test
    fun `payload keys that are not campaign attribution stay out of the event`() {
        val props = Utils.getCustomRatingEventProperties(campaignExtras(), 1, 2, "text")

        assertFalse(props.containsKey(PTConstants.PT_TITLE))
    }

    @Test
    fun `rating value is the raw position at scale two, with no sentiment mapping`() {
        // FR-EVT-03: no hidden normalisation - a 2-position scale reports 1 or 2.
        val props = Utils.getCustomRatingEventProperties(campaignExtras(), 2, 2, "text")

        assertEquals(2, props[PTConstants.PT_RATING_EVENT_VALUE])
        assertEquals(2, props[PTConstants.PT_RATING_EVENT_SCALE])
    }

    @Test
    fun `an unknown style is omitted rather than sent as a null property`() {
        val props = Utils.getCustomRatingEventProperties(campaignExtras(), 3, 5, null)

        assertNull(props[PTConstants.PT_RATING_EVENT_STYLE])
        assertFalse(props.containsKey(PTConstants.PT_RATING_EVENT_STYLE))
    }
}

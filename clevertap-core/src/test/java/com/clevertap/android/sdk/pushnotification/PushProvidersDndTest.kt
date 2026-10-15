package com.clevertap.android.sdk.pushnotification

import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Coverage for the pure DND-window check used by push amplification. The window is expressed as
 * HH:mm times; a stop time earlier than the start time denotes an overnight window.
 */
@RunWith(RobolectricTestRunner::class)
class PushProvidersDndTest : BaseTestCase() {

    private val parser = SimpleDateFormat("HH:mm", Locale.US)
    private fun t(hhmm: String) = parser.parse(hhmm)!!

    private fun buildSut(): PushProviders = PushProviders(
        appCtx, cleverTapInstanceConfig, mockk<BaseDatabaseManager>(relaxed = true),
        mockk<ValidationResultStack>(relaxed = true), mockk<AnalyticsManager>(relaxed = true),
        mockk<CTWorkManager>(relaxed = true), mockk<Clock>(relaxed = true)
    )

    @Test
    fun `same-day window - inside is true`() {
        assertTrue(buildSut().isTimeBetweenDNDTime(t("09:00"), t("17:00"), t("12:00")))
    }

    @Test
    fun `same-day window - before start is false`() {
        assertFalse(buildSut().isTimeBetweenDNDTime(t("09:00"), t("17:00"), t("08:00")))
    }

    @Test
    fun `same-day window - after stop is false`() {
        assertFalse(buildSut().isTimeBetweenDNDTime(t("09:00"), t("17:00"), t("18:00")))
    }

    @Test
    fun `overnight window - late evening is inside`() {
        // DND 22:00 -> 07:00 (stop < start => spans midnight)
        assertTrue(buildSut().isTimeBetweenDNDTime(t("22:00"), t("07:00"), t("23:30")))
    }

    @Test
    fun `overnight window - early morning is inside`() {
        assertTrue(buildSut().isTimeBetweenDNDTime(t("22:00"), t("07:00"), t("03:00")))
    }

    @Test
    fun `overnight window - midday is outside`() {
        assertFalse(buildSut().isTimeBetweenDNDTime(t("22:00"), t("07:00"), t("12:00")))
    }
}

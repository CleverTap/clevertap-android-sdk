package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController
import com.clevertap.android.sdk.displayunits.model.MockCleverTapDisplayUnit
import com.clevertap.android.sdk.utils.configMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DisplayUnitResponseTest {

    private val config = configMock().also { every { it.isAnalyticsOnly } returns false }
    private val callbackManager = mockk<BaseCallbackManager>(relaxed = true)
    private val controllerManager = mockk<ControllerManager>()
    private val context = mockk<Context>(relaxed = true)

    // A real cache so the merge/replace outcome can be asserted end to end.
    private val cache = CTDisplayUnitController()

    private lateinit var response: DisplayUnitResponse

    @Before
    fun setUp() {
        every { controllerManager.orCreateDisplayUnitCache } returns cache
        response = DisplayUnitResponse(config, callbackManager, controllerManager)
    }

    private fun unit(id: String): JSONObject =
        MockCleverTapDisplayUnit().getAUnit().put(Constants.NOTIFICATION_ID_TAG, id)

    private fun adUnitResponse(vararg units: JSONObject): JSONObject =
        JSONObject().put(
            Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY,
            JSONArray().apply { units.forEach { put(it) } }
        )

    private fun process(source: CTResponseSource, body: JSONObject) {
        response.responseSource = source
        response.processResponse(body, "", context)
    }

    private fun cachedIds(): Set<String> =
        cache.allDisplayUnits.orEmpty().map { it.unitID }.toSet()

    @Test
    fun `a1 response replaces the cache`() {
        process(CTResponseSource.A1, adUnitResponse(unit("u1"), unit("u2")))
        assertEquals(setOf("u1", "u2"), cachedIds())

        process(CTResponseSource.A1, adUnitResponse(unit("u3")))
        assertEquals(setOf("u3"), cachedIds()) // REPLACE: u1/u2 gone
    }

    @Test
    fun `content fetch merges by unitID and does not wipe a1 units`() {
        process(CTResponseSource.A1, adUnitResponse(unit("u1"), unit("u2")))
        assertEquals(2, cachedIds().size)

        // Partial personalized subset from /content: updates u2, adds u3, must keep u1.
        process(CTResponseSource.CONTENT_FETCH, adUnitResponse(unit("u2"), unit("u3")))
        assertEquals(setOf("u1", "u2", "u3"), cachedIds())
    }

    @Test
    fun `content fetch callback delivers the full merged set, never a smaller one`() {
        process(CTResponseSource.A1, adUnitResponse(unit("u1")))
        process(CTResponseSource.CONTENT_FETCH, adUnitResponse(unit("u2")))

        // Second (content) callback carries both units, not just the personalized one.
        verify { callbackManager.notifyDisplayUnitsLoaded(match { it.size == 2 }) }
    }

    @Test
    fun `empty content fetch adUnit_notifs does not touch the cache`() {
        process(CTResponseSource.A1, adUnitResponse(unit("u1"), unit("u2")))
        assertEquals(2, cachedIds().size)

        process(CTResponseSource.CONTENT_FETCH, adUnitResponse()) // empty array
        assertEquals(setOf("u1", "u2"), cachedIds()) // unchanged, not wiped
    }
}

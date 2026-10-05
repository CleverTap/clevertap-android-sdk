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

    // A real cache so the upsert outcome can be asserted end to end.
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

    private fun process(body: JSONObject) {
        response.processResponse(body, "", context)
    }

    private fun cachedIds(): Set<String> =
        cache.allDisplayUnits.orEmpty().map { it.unitID }.toSet()

    @Test
    fun `responses upsert by unitID and keep earlier units`() {
        process(adUnitResponse(unit("u1"), unit("u2")))
        assertEquals(setOf("u1", "u2"), cachedIds())

        process(adUnitResponse(unit("u3")))
        assertEquals(setOf("u1", "u2", "u3"), cachedIds()) // upsert, not replace
    }

    @Test
    fun `a later subset updates and adds without wiping earlier units`() {
        process(adUnitResponse(unit("u1"), unit("u2")))
        assertEquals(2, cachedIds().size)

        // e.g. a /content personalized subset: updates u2, adds u3, keeps u1.
        process(adUnitResponse(unit("u2"), unit("u3")))
        assertEquals(setOf("u1", "u2", "u3"), cachedIds())
    }

    @Test
    fun `each response notifies listeners with only its own units`() {
        process(adUnitResponse(unit("u1")))
        verify { callbackManager.notifyDisplayUnitsLoaded(match { it.size == 1 && it[0].unitID == "u1" }) }

        process(adUnitResponse(unit("u2")))
        // A later response fires its own callback with just its unit, not the accumulated set.
        verify { callbackManager.notifyDisplayUnitsLoaded(match { it.size == 1 && it[0].unitID == "u2" }) }
    }

    @Test
    fun `empty adUnit_notifs does not touch the cache`() {
        process(adUnitResponse(unit("u1"), unit("u2")))
        assertEquals(2, cachedIds().size)

        process(adUnitResponse()) // empty array
        assertEquals(setOf("u1", "u2"), cachedIds()) // unchanged
    }
}

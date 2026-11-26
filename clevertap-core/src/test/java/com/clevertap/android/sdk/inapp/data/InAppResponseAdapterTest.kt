package com.clevertap.android.sdk.inapp.data

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class InAppResponseAdapterTest {

    private val templatesManager = mockk<TemplatesManager>(relaxed = true)

    private fun adapter(response: JSONObject) = InAppResponseAdapter(response, templatesManager)

    private fun response(vararg items: JSONObject): JSONObject =
        JSONObject().put(
            Constants.CONTENT_FETCH_JSON_RESPONSE_KEY,
            JSONArray().apply { items.forEach { put(it) } }
        )

    @Test
    fun `contentFetchItems parses the content_fetch array into typed items`() {
        val body = response(
            JSONObject()
                .put(Constants.CONTENT_FETCH_ITEM_EVENT_NAME, "App Launched")
                .put(Constants.CONTENT_FETCH_ITEM_RESPONSE_KEY, "inapp_notifs_applaunched")
                .put(Constants.CONTENT_FETCH_ITEM_TARGET_ID, 1787567474L),
            JSONObject()
                .put(Constants.CONTENT_FETCH_ITEM_EVENT_NAME, "Product Viewed")
                .put(Constants.CONTENT_FETCH_ITEM_TARGET_ID, "42")
        )

        val items = adapter(body).contentFetchItems

        assertEquals(2, items.size)
        assertEquals("App Launched", items[0].eventName)
        assertEquals("inapp_notifs_applaunched", items[0].responseKey)
        assertEquals("1787567474", items[0].targetId) // numeric tgtId normalized to String
        assertEquals("Product Viewed", items[1].eventName)
        assertEquals("42", items[1].targetId)
    }

    @Test
    fun `contentFetchItems skips non-object entries in the array`() {
        val body = response(
            JSONObject().put(Constants.CONTENT_FETCH_ITEM_TARGET_ID, "1")
        ).also {
            it.getJSONArray(Constants.CONTENT_FETCH_JSON_RESPONSE_KEY).put("not-an-object")
        }

        val items = adapter(body).contentFetchItems

        assertEquals(1, items.size)
        assertEquals("1", items[0].targetId)
    }

    @Test
    fun `contentFetchItems is empty when the content_fetch key is absent`() {
        assertTrue(adapter(JSONObject()).contentFetchItems.isEmpty())
    }

    @Test
    fun `contentFetchItems is empty when the content_fetch array is empty`() {
        assertTrue(adapter(response()).contentFetchItems.isEmpty())
    }

    private fun media(url: String, contentType: String): JSONObject =
        JSONObject().put(Constants.KEY_URL, url).put(Constants.KEY_CONTENT_TYPE, contentType)

    @Test
    fun `preloadAssetsMeta collects client-side portrait and landscape image and gif urls`() {
        val inApps = JSONArray()
            .put(
                JSONObject()
                    .put(Constants.KEY_MEDIA, media("https://p.png", "image/png"))
                    .put(Constants.KEY_MEDIA_LANDSCAPE, media("https://l.gif", "image/gif"))
            )
            .put(
                JSONObject()
                    .put(Constants.KEY_MEDIA, media("https://v.mp4", "video/mp4"))
                    .put(Constants.KEY_MEDIA_LANDSCAPE, media("", "image/png"))
            )
            .put(JSONObject().put(Constants.KEY_MEDIA_LANDSCAPE, media("https://l2.jpg", "image/jpeg")))
        val body = JSONObject().put(Constants.INAPP_NOTIFS_KEY_CS, inApps)

        val meta = adapter(body).preloadAssetsMeta

        assertEquals(
            listOf(
                "https://p.png" to CtCacheType.IMAGE,
                "https://l2.jpg" to CtCacheType.IMAGE,
                "https://l.gif" to CtCacheType.GIF
            ),
            meta
        )
    }

    @Test
    fun `preloadAssetsMeta ignores media of non client-side in-apps`() {
        val inApps = JSONArray().put(JSONObject().put(Constants.KEY_MEDIA, media("https://p.png", "image/png")))
        val body = JSONObject().put(Constants.INAPP_JSON_RESPONSE_KEY, inApps)

        assertTrue(adapter(body).preloadAssetsMeta.isEmpty())
    }
}

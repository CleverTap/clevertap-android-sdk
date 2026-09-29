package com.clevertap.android.sdk.inapp

import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.utils.configMock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InAppActionParserTest {

    private val config = configMock()

    @Test
    fun `parse returns action unchanged for non open-url action`() {
        val action = CTInAppAction.createCloseAction()
        val data = Bundle().apply { putString("k", "v") }

        val parsed = InAppActionParser.parse(action, "cta", data, config)

        assertEquals(action, parsed.action)
        assertEquals("cta", parsed.callToAction)
        assertEquals(data, parsed.additionalData)
    }

    @Test
    fun `parse surfaces url parameters as additionalData`() {
        val url = Uri.parse("https://clevertap.com").buildUpon()
            .appendQueryParameter("param1", "value")
            .appendQueryParameter("param2", "value 2")
            .appendQueryParameter("param3", "5")
            .build().toString()

        val parsed = InAppActionParser.parse(CTInAppAction.createOpenUrlAction(url), null, null, config)

        assertEquals(InAppActionType.OPEN_URL, parsed.action.type)
        assertEquals(url, parsed.action.actionUrl)
        val data = parsed.additionalData!!
        assertEquals("value", data.getString("param1"))
        assertEquals("value 2", data.getString("param2"))
        assertEquals("5", data.getString("param3"))
    }

    @Test
    fun `parse merges url parameters with provided additionalData preferring additionalData`() {
        val url = Uri.parse("https://clevertap.com").buildUpon()
            .appendQueryParameter("param1", "value")
            .appendQueryParameter("param2", "value 2")
            .appendQueryParameter("param3", "5")
            .build().toString()
        val data = Bundle().apply {
            putString("param1", "dataValue")
            putString("param2", "data value 2")
        }

        val parsed = InAppActionParser.parse(CTInAppAction.createOpenUrlAction(url), null, data, config)

        val out = parsed.additionalData!!
        assertEquals("dataValue", out.getString("param1"))
        assertEquals("data value 2", out.getString("param2"))
        assertEquals("5", out.getString("param3"))
    }

    @Test
    fun `parse uses c2a url param when no callToAction argument is provided`() {
        val url = Uri.parse("https://clevertap.com").buildUpon()
            .appendQueryParameter(Constants.KEY_C2A, "c2aParam")
            .build().toString()

        val parsed = InAppActionParser.parse(CTInAppAction.createOpenUrlAction(url), null, null, config)

        assertEquals("c2aParam", parsed.callToAction)
        // c2a is a control param and should not remain in the tracked data
        assertNull(parsed.additionalData!!.getString(Constants.KEY_C2A))
    }

    @Test
    fun `parse prefers callToAction argument over c2a url param`() {
        val url = Uri.parse("https://clevertap.com").buildUpon()
            .appendQueryParameter(Constants.KEY_C2A, "c2aParam")
            .build().toString()

        val parsed =
            InAppActionParser.parse(CTInAppAction.createOpenUrlAction(url), "argument", null, config)

        assertEquals("argument", parsed.callToAction)
    }

    @Test
    fun `parse extracts deeplink from c2a __dl__ param and tracks only original url params`() {
        val dl = "https://deeplink.com?param1=asd&param2=value2"
        val url = Uri.parse("https://clevertap.com").buildUpon()
            .appendQueryParameter(Constants.KEY_C2A, "c2aParam__dl__$dl")
            .appendQueryParameter("param1", "value")
            .build().toString()

        val parsed = InAppActionParser.parse(CTInAppAction.createOpenUrlAction(url), null, null, config)

        // the open-url action should target the url after __dl__
        assertEquals(dl, parsed.action.actionUrl)
        assertEquals("c2aParam", parsed.callToAction)
        val data = parsed.additionalData!!
        assertEquals(1, data.size())
        assertEquals("value", data.getString("param1"))
    }
}

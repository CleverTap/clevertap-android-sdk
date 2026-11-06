package com.clevertap.android.sdk.pushnotification.preview

import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PushPreviewKeyTest : BaseTestCase() {

    private fun payload(vararg fields: Pair<String, String>): PushPreviewPayload =
        PushPreviewPayload.fromJson(JSONObject().apply { fields.forEach { (k, v) -> put(k, v) } })!!

    private fun inApp(json: String) = payload(
        Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY to "custom-html",
        Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY to json,
    )

    private val simpleInApp = inApp("""{"a":1}""")

    private fun s3(url: String) = payload(
        Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY to "advanced-builder",
        Constants.INAPP_PREVIEW_S3_URL_KEY to url,
    )

    @Test
    fun `key is 64 lowercase hex characters`() {
        val key = PushPreviewKey.of(simpleInApp)

        assertEquals(64, key.length)
        assertTrue(key.all { it in '0'..'9' || it in 'a'..'f' }, "not lowercase hex: $key")
    }

    @Test
    fun `known answer - sha256 of the NUL joined canonical fields`() {
        // material = "custom-html" NUL {"a":1} NUL "" NUL "" NUL "" NUL, computed with an independent sha256.
        assertEquals(
            "aafea0c08330cea7b3eb0678acac656040b2dfdd3d6bded76b6166072c4818bc",
            PushPreviewKey.of(simpleInApp)
        )
    }

    @Test
    fun `same key when a wrapper reorders keys and adds whitespace`() {
        val arrived = inApp("""{"type":"half-interstitial","isJsEnabled":false,"d":{"html":"<p>Hi</p>"}}""")
        val tapped = inApp("""{ "d": { "html": "<p>Hi</p>" },   "isJsEnabled": false, "type": "half-interstitial" }""")

        assertEquals(PushPreviewKey.of(arrived), PushPreviewKey.of(tapped))
    }

    @Test
    fun `same key when a whole number is re-serialised as a double`() {
        assertEquals(PushPreviewKey.of(inApp("""{"w":100}""")), PushPreviewKey.of(inApp("""{"w":100.0}""")))
        assertEquals(PushPreviewKey.of(inApp("""{"w":100}""")), PushPreviewKey.of(inApp("""{"w":1e2}""")))
    }

    @Test
    fun `different key when one character of the content changes`() {
        assertNotEquals(PushPreviewKey.of(inApp("""{"a":1}""")), PushPreviewKey.of(inApp("""{"a":2}""")))
        assertNotEquals(PushPreviewKey.of(inApp("""{"a":1}""")), PushPreviewKey.of(inApp("""{"a":"1"}""")))
    }

    @Test
    fun `s3 url - query string is ignored, path is kept`() {
        val base = "https://bucket.s3.region.amazonaws.com/inapp-preview-campaign/1699358338/8bd4c58d"
        val signedA = "$base?X-Amz-Expires=240&X-Amz-Signature=aaa"
        val signedB = "$base?X-Amz-Expires=240&X-Amz-Signature=bbb&X-Amz-Security-Token=x%2Fy"
        val otherObject =
            "https://bucket.s3.region.amazonaws.com/inapp-preview-campaign/1699358338/ffffffff?X-Amz-Signature=aaa"

        assertEquals(PushPreviewKey.of(s3(signedA)), PushPreviewKey.of(s3(signedB)))
        assertNotEquals(PushPreviewKey.of(s3(signedA)), PushPreviewKey.of(s3(otherObject)))
    }

    @Test
    fun `s3 url that does not parse is hashed as the raw string`() {
        val broken = "https://host/inapp-preview-campaign/{x}"
        assertEquals(PushPreviewKey.of(s3(broken)), PushPreviewKey.of(s3(broken)))
        assertNotEquals(PushPreviewKey.of(s3(broken)), PushPreviewKey.of(s3("$broken-2")))
    }

    @Test
    fun `invalid json is hashed as the raw string`() {
        assertEquals(PushPreviewKey.of(inApp("not json")), PushPreviewKey.of(inApp("not json")))
        assertNotEquals(PushPreviewKey.of(inApp("not json")), PushPreviewKey.of(inApp("not json ")))
    }

    @Test
    fun `missing fields hash as empty and the field order matters`() {
        val typeOnly = payload(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY to "custom-html")
        val inboxOnly = payload(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY to "custom-html")

        assertNotEquals(PushPreviewKey.of(typeOnly), PushPreviewKey.of(inboxOnly))
        assertNotEquals(PushPreviewKey.of(typeOnly), PushPreviewKey.of(inApp("""{}""")))
    }

    @Test
    fun `inbox and display unit previews key on their own payloads`() {
        val inboxA = payload(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY to """{"msg":[{"id":1}]}""")
        val inboxB = payload(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY to """{"msg":[{"id":2}]}""")
        val unit = payload(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY to """{"msg":[{"id":1}]}""")

        assertNotEquals(PushPreviewKey.of(inboxA), PushPreviewKey.of(inboxB))
        assertNotEquals(PushPreviewKey.of(inboxA), PushPreviewKey.of(unit))
    }

    // ---------- CanonicalJson ----------

    @Test
    fun `canonical json sorts keys at every level and keeps array order`() {
        assertEquals(
            """{"a":[3,1,{"y":false,"z":null}],"b":{"c":"x","d":true}}""",
            CanonicalJson.of("""{ "b": {"d": true, "c": "x"}, "a": [3, 1, {"z": null, "y": false}] }""")
        )
    }

    @Test
    fun `canonical json normalises numbers`() {
        assertEquals("""{"n":100}""", CanonicalJson.of("""{"n":100.0}"""))
        assertEquals("""{"n":-7}""", CanonicalJson.of("""{"n":-7.0}"""))
        assertEquals("""{"n":1.5}""", CanonicalJson.of("""{"n":1.5}"""))
        assertEquals("""{"n":0}""", CanonicalJson.of("""{"n":0}"""))
    }

    @Test
    fun `canonical json escapes strings`() {
        assertEquals("""{"s":"a\"b\n"}""", CanonicalJson.of("""{"s":"a\"b\n"}"""))
    }

    @Test
    fun `canonical json accepts a top level array`() {
        assertEquals("""[{"a":1},2]""", CanonicalJson.of(""" [ {"a": 1}, 2 ] """))
    }

    @Test
    fun `canonical json returns null for non json`() {
        assertNull(CanonicalJson.of("plain text"))
        assertNull(CanonicalJson.of("{broken"))
        assertNull(CanonicalJson.of(""))
    }

    // An attacker's intent can carry anything. Hostile JSON must be refused before parsing,
    // because org.json recurses and a StackOverflowError would crash the app.

    @Test
    fun `canonical json refuses deeply nested arrays without touching the parser`() {
        val hostile = "[".repeat(20_000) + "]".repeat(20_000)

        assertNull(CanonicalJson.of(hostile))
    }

    @Test
    fun `canonical json refuses deeply nested objects`() {
        val hostile = """{"a":""".repeat(5_000) + "1" + "}".repeat(5_000)

        assertNull(CanonicalJson.of(hostile))
    }

    @Test
    fun `canonical json refuses oversized input`() {
        val hostile = """{"a":"${"x".repeat(70_000)}"}"""

        assertNull(CanonicalJson.of(hostile))
    }

    @Test
    fun `brackets inside strings do not count as nesting`() {
        val text = """{"a":"${"[".repeat(500)}","b":"\"[{"}"""

        assertEquals("""{"a":"${"[".repeat(500)}","b":"\"[{"}""", CanonicalJson.of(text))
    }

    @Test
    fun `key for hostile input is still computed from the raw string without crashing`() {
        val hostile = "[".repeat(20_000) + "]".repeat(20_000)

        val key = PushPreviewKey.of(inApp(hostile))

        assertEquals(64, key.length)
        assertEquals(key, PushPreviewKey.of(inApp(hostile)))
    }
}

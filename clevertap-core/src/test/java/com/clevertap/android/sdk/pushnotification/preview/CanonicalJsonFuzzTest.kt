package com.clevertap.android.sdk.pushnotification.preview

import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Randomised checks over the strict reader and the URL policy. A fixed seed keeps every
 * run identical, so a failure here reproduces on the next run.
 *
 * Three properties matter for the key:
 *  - any input, however broken, returns a result and never throws;
 *  - a canonical result is a fixed point: canonicalising it again gives the same text;
 *  - the same standard JSON written with different whitespace, key order or escaping
 *    gives the same canonical text, which is what makes two sightings match.
 */
@RunWith(RobolectricTestRunner::class)
class CanonicalJsonFuzzTest : BaseTestCase() {

    private val random = Random(20261106L)
    private val rounds = 1500

    // ---------- generators ----------

    private fun randomString(): String {
        val length = random.nextInt(12)
        val alphabet = """ab "\/${'\n'}${'\t'}{}[]:,'#\u00e9\u20ac${'\u0001'}"""
        return buildString { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
    }

    private fun randomScalar(): Any? = when (random.nextInt(6)) {
        0 -> randomString()
        1 -> random.nextInt(2000) - 1000
        2 -> random.nextDouble() * 1000 - 500
        3 -> random.nextBoolean()
        4 -> JSONObject.NULL
        else -> 100.0 * random.nextInt(50) // whole doubles, must print without ".0"
    }

    private fun randomValue(depth: Int): Any? = when {
        depth >= 5 -> randomScalar()
        random.nextInt(3) == 0 -> JSONObject().apply {
            repeat(random.nextInt(4)) { put(randomString(), randomValue(depth + 1)) }
        }
        random.nextInt(3) == 0 -> JSONArray().apply {
            repeat(random.nextInt(4)) { put(randomValue(depth + 1)) }
        }
        else -> randomScalar()
    }

    private fun randomContainer(): String = when (random.nextInt(2)) {
        0 -> JSONObject().apply { repeat(random.nextInt(5)) { put(randomString(), randomValue(1)) } }.toString()
        else -> JSONArray().apply { repeat(random.nextInt(5)) { put(randomValue(1)) } }.toString()
    }

    private fun randomGarbage(): String {
        val alphabet = """{}[]"'\:,/ #${'\n'}${'\t'}0123456789.-+eEtrufalsn${'\u0000'}é"""
        val length = random.nextInt(60)
        return buildString { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
    }

    /** Same content as org.json wrote it, but indented, which also escapes "/" as "\/". */
    private fun reformat(json: String): String =
        if (json.startsWith("{")) JSONObject(json).toString(2) else JSONArray(json).toString(2)

    /** Same object with its members inserted in a different order. */
    private fun shuffleTopLevel(json: String): String {
        if (!json.startsWith("{")) return json
        val source = JSONObject(json)
        val keys = source.keys().asSequence().map { it.toString() }.toMutableList().apply { shuffle(random) }
        return JSONObject().apply { keys.forEach { put(it, source.get(it)) } }.toString()
    }

    // ---------- properties ----------

    @Test
    fun `standard json always canonicalises and is a fixed point`() {
        repeat(rounds) {
            val json = randomContainer()
            val canonical = CanonicalJson.of(json)

            assertNotNull(canonical, "must accept org.json output: $json")
            assertEquals(canonical, CanonicalJson.of(canonical), "must be a fixed point: $json")
            assertEquals(canonical, CanonicalJson.of(reformat(json)), "whitespace and \\/ must not matter: $json")
            assertEquals(canonical, CanonicalJson.of(shuffleTopLevel(json)), "key order must not matter: $json")
        }
    }

    @Test
    fun `canonical output is itself parseable by org_json with the same content`() {
        repeat(rounds) {
            val json = randomContainer()
            val canonical = CanonicalJson.of(json)!!
            // Round trip through org.json and back through the reader lands on the same text.
            assertEquals(canonical, CanonicalJson.of(reformat(canonical)), json)
        }
    }

    @Test
    fun `garbage never throws and any accepted result is a fixed point`() {
        repeat(rounds * 4) {
            val text = randomGarbage()
            val result = CanonicalJson.of(text)
            if (result != null) {
                assertEquals(result, CanonicalJson.of(result), "fixed point for garbage: $text")
            }
        }
    }

    @Test
    fun `nesting is accepted up to the limit and refused beyond it, in any mix`() {
        repeat(200) {
            val depth = 1 + random.nextInt(60)
            val open = StringBuilder()
            val close = StringBuilder()
            repeat(depth) {
                if (random.nextBoolean()) {
                    open.append("{\"k\":")
                    close.insert(0, "}")
                } else {
                    open.append("[")
                    close.insert(0, "]")
                }
            }
            val text = open.toString() + "1" + close.toString()
            val result = CanonicalJson.of(text)
            if (depth <= 32) {
                assertNotNull(result, "depth $depth must be accepted")
            } else {
                assertNull(result, "depth $depth must be refused")
            }
        }
    }

    @Test
    fun `preview key never throws and is stable for any bundle content`() {
        repeat(rounds) {
            val text = if (random.nextBoolean()) randomContainer() else randomGarbage()
            val payload = PushPreviewPayload.fromJson(
                JSONObject().put(com.clevertap.android.sdk.Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, text)
            )!!

            val key = PushPreviewKey.of(payload)

            assertEquals(64, key.length, text)
            assertEquals(key, PushPreviewKey.of(payload), text)
        }
    }
}

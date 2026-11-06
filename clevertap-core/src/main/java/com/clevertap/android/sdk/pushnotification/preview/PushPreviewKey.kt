package com.clevertap.android.sdk.pushnotification.preview

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/**
 * Turns a [PushPreviewPayload] into a stable 64 character hex key.
 *
 * The key is a lookup identifier, not a secret and not a proof. It only has to
 * be the same every time the same preview is seen on the same device, and
 * different for different previews. SHA-256 gives both.
 *
 * Example: two sightings of one push, where a wrapper library has re-ordered
 * the JSON in between, still give the same key because the JSON is
 * canonicalised (sorted keys, no whitespace) before hashing.
 */
internal object PushPreviewKey {

    private const val SEPARATOR = '\u0000'

    private val JSON_FIELDS = setOf(
        Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY,
        Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY,
        Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY,
    )

    fun of(payload: PushPreviewPayload): String {
        val material = buildString {
            for (key in PushPreviewPayload.KEYS) {
                append(canonical(key, payload[key]))
                append(SEPARATOR)
            }
        }
        return sha256Hex(material)
    }

    private fun canonical(key: String, value: String?): String = when {
        value == null -> ""
        key == Constants.INAPP_PREVIEW_S3_URL_KEY -> pathOnly(value)
        key in JSON_FIELDS -> CanonicalJson.of(value) ?: value
        else -> value
    }

    /** The presigned query string may be re-encoded in transit; the path holds the per-send UUID. */
    private fun pathOnly(url: String): String = try {
        URI(url).rawPath ?: url
    } catch (ignored: URISyntaxException) {
        url
    }

    /** Lowercase hex. Formatter treats a negative Byte as its unsigned 8 bit value, so no masking is needed. */
    private fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte) }
}

/**
 * Deterministic JSON text: sorted keys, no whitespace, whole-number doubles as integers.
 * Returns null when the text is not a JSON object or array, or when it is too big or
 * too deeply nested to be a real preview.
 *
 * Example: `{ "b": 1.0, "a": "x" }` and `{"a":"x","b":1}` both become `{"a":"x","b":1}`.
 *
 * The input may come from a tapped intent, which anyone can build. org.json parses
 * recursively and so does [write], so a value like `[[[[...` ten thousand levels deep
 * would overflow the stack. A StackOverflowError is not an Exception, so nothing above
 * would catch it and the app would crash. The size and depth checks below run before
 * any parsing and cost one pass over the string. A real preview is under 4 KB (the FCM
 * limit) and a handful of levels deep, so honest input is never affected.
 */
internal object CanonicalJson {

    private const val WHOLE_NUMBER_LIMIT = 1e15

    /** Well above the FCM payload cap, well below anything that could hurt. */
    private const val MAX_LENGTH = 64 * 1024

    /** Real previews nest four or five levels; the parser's recursion is the only cost. */
    private const val MAX_DEPTH = 32

    fun of(text: String): String? {
        if (text.length > MAX_LENGTH || nestingDepth(text) > MAX_DEPTH) {
            return null
        }
        val trimmed = text.trim()
        return try {
            when {
                trimmed.startsWith("{") -> write(JSONObject(trimmed))
                trimmed.startsWith("[") -> write(JSONArray(trimmed))
                else -> null
            }
        } catch (ignored: JSONException) {
            null
        }
    }

    /**
     * Deepest bracket nesting, ignoring brackets inside string literals. Stops counting
     * early once past [MAX_DEPTH] because the exact number no longer matters.
     *
     * Example: `{"a":[1,{"b":2}]}` has depth 3; `"[["` has depth 0 because it is a string.
     */
    private fun nestingDepth(text: String): Int {
        val scanner = DepthScanner()
        for (ch in text) {
            if (scanner.accept(ch) > MAX_DEPTH) break
        }
        return scanner.deepest
    }

    /** Tracks bracket depth while skipping JSON string literals and their escapes. */
    private class DepthScanner {
        var deepest = 0
            private set
        private var depth = 0
        private var inString = false
        private var escaped = false

        /** Feeds one character and returns the deepest nesting seen so far. */
        fun accept(ch: Char): Int {
            if (inString) insideString(ch) else outsideString(ch)
            return deepest
        }

        private fun insideString(ch: Char) = when {
            escaped -> escaped = false
            ch == '\\' -> escaped = true
            ch == '"' -> inString = false
            else -> Unit
        }

        private fun outsideString(ch: Char) = when (ch) {
            '"' -> inString = true
            '{', '[' -> deepest = maxOf(deepest, ++depth)
            '}', ']' -> depth--
            else -> Unit
        }
    }

    private fun write(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().sorted()
            .joinToString(",", "{", "}") { key -> JSONObject.quote(key) + ":" + write(value.opt(key)) }
        is JSONArray -> (0 until value.length())
            .joinToString(",", "[", "]") { index -> write(value.opt(index)) }
        is String -> JSONObject.quote(value)
        is Boolean -> value.toString()
        is Number -> number(value)
        else -> JSONObject.quote(value.toString())
    }

    private fun number(value: Number): String {
        val d = value.toDouble()
        val isWhole = d.isFinite() && d == floor(d) && abs(d) < WHOLE_NUMBER_LIMIT
        return if (isWhole) d.toLong().toString() else JSONObject.numberToString(value)
    }
}

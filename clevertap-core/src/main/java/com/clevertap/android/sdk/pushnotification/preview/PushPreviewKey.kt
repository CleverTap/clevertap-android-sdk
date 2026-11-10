package com.clevertap.android.sdk.pushnotification.preview

import com.clevertap.android.sdk.Constants
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.util.Locale
import java.util.TreeMap
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
 * Returns null when the text is not a plain JSON object or array, or is too big or
 * too deeply nested to be a real preview.
 *
 * Example: `{ "b": 1.0, "a": "x" }` and `{"a":"x","b":1}` both become `{"a":"x","b":1}`.
 *
 * Why this has its own reader instead of org.json: the input may come from a tapped
 * intent, which is not under the SDK's control. org.json parses recursively, so `[[[[...` ten
 * thousand levels deep overflows the stack, and StackOverflowError is not an Exception.
 * org.json also accepts single-quoted strings and comments, so no pre-scan written for
 * standard JSON can reliably predict how deep it will recurse. This reader accepts
 * standard JSON only (RFC 8259) and carries the depth into every nested call, refusing
 * at [MAX_DEPTH]. It can therefore never recurse deeper than that, whatever the input.
 *
 * Refusal is not an error path: the caller hashes the raw text instead. Real previews
 * from the backend are standard JSON under 4 KB (the FCM cap) and a few levels deep,
 * so they are never refused.
 */
internal object CanonicalJson {

    /** Well above the FCM payload cap, well below anything that could hurt. */
    private const val MAX_LENGTH = 64 * 1024

    /** Real previews nest four or five levels. */
    private const val MAX_DEPTH = 32

    private const val WHOLE_NUMBER_LIMIT = 1e15

    private const val FORM_FEED = '\u000C'

    fun of(text: String): String? {
        if (text.length > MAX_LENGTH) return null
        val lexer = Lexer(text)
        val out = StringBuilder(text.length)
        lexer.skipWhitespace()
        val isContainer = lexer.peek() == '{' || lexer.peek() == '['
        val ok = isContainer && Reader(lexer).readValue(out, depth = 0) && lexer.isAtEnd
        return if (ok) out.toString() else null
    }

    /** Grammar and canonical output. Depth is checked on entry to every object and array. */
    private class Reader(private val lexer: Lexer) {

        fun readValue(out: StringBuilder, depth: Int): Boolean {
            lexer.skipWhitespace()
            return when (lexer.peek()) {
                '{' -> readObject(out, depth + 1)
                '[' -> readArray(out, depth + 1)
                '"' -> appendString(out)
                't' -> appendLiteral("true", out)
                'f' -> appendLiteral("false", out)
                'n' -> appendLiteral("null", out)
                else -> appendNumber(out)
            }
        }

        private fun readObject(out: StringBuilder, depth: Int): Boolean {
            if (depth > MAX_DEPTH) return false
            lexer.consume('{')
            val members = TreeMap<String, String>()
            var ok = true
            var more = !lexer.consumeAfterWhitespace('}')
            while (ok && more) {
                ok = readMember(members, depth)
                more = ok && lexer.consumeAfterWhitespace(',')
                if (ok && !more) ok = lexer.consume('}')
            }
            if (ok) writeObject(members, out)
            return ok
        }

        private fun readMember(members: TreeMap<String, String>, depth: Int): Boolean {
            lexer.skipWhitespace()
            val key = lexer.readString()
            val value = StringBuilder()
            val ok = key != null && lexer.consumeAfterWhitespace(':') && readValue(value, depth)
            if (ok && key != null) members[key] = value.toString() // a repeated key keeps its last value
            return ok
        }

        private fun writeObject(members: TreeMap<String, String>, out: StringBuilder) {
            out.append('{')
            var first = true
            for ((key, value) in members) {
                if (!first) out.append(',')
                first = false
                out.append(quote(key)).append(':').append(value)
            }
            out.append('}')
        }

        private fun readArray(out: StringBuilder, depth: Int): Boolean {
            if (depth > MAX_DEPTH) return false
            lexer.consume('[')
            out.append('[')
            var ok = true
            var more = !lexer.consumeAfterWhitespace(']')
            while (ok && more) {
                ok = readValue(out, depth)
                more = ok && lexer.consumeAfterWhitespace(',')
                if (more) out.append(',') else if (ok) ok = lexer.consume(']')
            }
            out.append(']')
            return ok
        }

        private fun appendString(out: StringBuilder): Boolean {
            val s = lexer.readString()
            if (s != null) out.append(quote(s))
            return s != null
        }

        private fun appendLiteral(literal: String, out: StringBuilder): Boolean {
            val ok = lexer.readLiteral(literal)
            if (ok) out.append(literal)
            return ok
        }

        private fun appendNumber(out: StringBuilder): Boolean {
            val token = lexer.readNumberToken()
            if (token != null) out.append(normaliseNumber(token))
            return token != null
        }
    }

    /** Tokens of standard JSON. Every method leaves the position after what it consumed, or where it failed. */
    private class Lexer(private val text: String) {

        private var pos = 0

        val isAtEnd: Boolean
            get() {
                skipWhitespace()
                return pos == text.length
            }

        fun peek(): Char = if (pos < text.length) text[pos] else END

        fun skipWhitespace() {
            while (pos < text.length && text[pos] in WHITESPACE) pos++
        }

        fun consume(expected: Char): Boolean {
            val found = peek() == expected
            if (found) pos++
            return found
        }

        fun consumeAfterWhitespace(expected: Char): Boolean {
            skipWhitespace()
            return consume(expected)
        }

        /** A double-quoted string with standard escapes only. Returns the decoded text, or null. */
        fun readString(): String? {
            if (!consume('"')) return null
            val sb = StringBuilder()
            var ok = true
            var closed = false
            while (ok && !closed && pos < text.length) {
                val ch = text[pos++]
                when {
                    ch == '"' -> closed = true
                    ch == '\\' -> ok = readEscape(sb)
                    ch < ' ' -> ok = false // control characters must be escaped
                    else -> sb.append(ch)
                }
            }
            return if (ok && closed) sb.toString() else null
        }

        private fun readEscape(sb: StringBuilder): Boolean {
            val ch = if (pos < text.length) text[pos++] else END
            val decoded = when (ch) {
                '"', '\\', '/' -> ch
                'b' -> '\b'
                'f' -> FORM_FEED
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> readUnicodeEscape()
                else -> null
            }
            if (decoded != null) sb.append(decoded)
            return decoded != null
        }

        /** Exactly four hex digits. toIntOrNull alone would also accept a sign, as in "\u-001". */
        private fun readUnicodeEscape(): Char? {
            val end = pos + HEX_DIGITS
            val digits = if (end <= text.length) text.substring(pos, end) else ""
            val code = if (digits.all(::isHexDigit)) digits.toIntOrNull(HEX_RADIX) else null
            if (code != null) pos = end
            return code?.toChar()
        }

        fun readLiteral(literal: String): Boolean {
            val found = text.startsWith(literal, pos)
            if (found) pos += literal.length
            return found
        }

        /** Strict JSON number grammar: -?(0|[1-9][0-9]*)(.[0-9]+)?([eE][+-]?[0-9]+)? */
        fun readNumberToken(): String? {
            val start = pos
            consume('-')
            var ok = readDigits(integerPart = true)
            if (ok && consume('.')) ok = readDigits(integerPart = false)
            if (ok && (peek() == 'e' || peek() == 'E')) {
                pos++
                if (peek() == '+' || peek() == '-') pos++
                ok = readDigits(integerPart = false)
            }
            return if (ok) text.substring(start, pos) else null
        }

        /** JSON forbids a leading zero in the integer part: "0" and "0.5" are fine, "01" is not. */
        private fun readDigits(integerPart: Boolean): Boolean {
            val start = pos
            while (pos < text.length && text[pos] in '0'..'9') pos++
            val count = pos - start
            val leadingZero = integerPart && count > 1 && text[start] == '0'
            return count > 0 && !leadingZero
        }

        private companion object {
            const val END = '\u0000'
            const val HEX_DIGITS = 4
            const val HEX_RADIX = 16
            val WHITESPACE = charArrayOf(' ', '\t', '\n', '\r')
        }
    }

    private fun isHexDigit(ch: Char): Boolean = ch in '0'..'9' || ch in 'a'..'f' || ch in 'A'..'F'

    /** 100, 100.0 and 1e2 all print as 100; other finite numbers print as Kotlin's Double text. */
    private fun normaliseNumber(token: String): String {
        val d = token.toDoubleOrNull() ?: return token
        val isWhole = d.isFinite() && d == floor(d) && abs(d) < WHOLE_NUMBER_LIMIT
        return when {
            isWhole -> d.toLong().toString()
            d.isFinite() -> d.toString()
            else -> token
        }
    }

    private fun quote(s: String): String {
        val sb = StringBuilder(s.length + 2).append('"')
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                FORM_FEED -> sb.append("\\f")
                else -> if (ch < ' ') sb.append("\\u%04x".format(Locale.ROOT, ch.code)) else sb.append(ch)
            }
        }
        return sb.append('"').toString()
    }
}

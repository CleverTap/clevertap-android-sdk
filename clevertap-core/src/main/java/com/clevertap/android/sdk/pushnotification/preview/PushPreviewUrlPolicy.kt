package com.clevertap.android.sdk.pushnotification.preview

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/**
 * Option F of SDK-6282: decides whether an in-app preview may be fetched from
 * a wzrk_inapp_s3_url. Validates the URL strictly even though the trusted store
 * already vouches for it.
 *
 * Allowed example:
 *   https://sk1-...-gd8sly0omq1m.s3.ap-northeast-2.amazonaws.com/inapp-preview-campaign/1699358338/8bd4...
 * Rejected examples and why:
 *   http://...                                  scheme
 *   https://sk1-...amazonaws.com.other.example/...   host not allowed (exact match only)
 *   https://sk1-...amazonaws.com@other.example/...   userinfo
 *   https://sk1-...amazonaws.com/other/x        path prefix
 */
internal class PushPreviewUrlPolicy(
    private val allowedHosts: Set<String> = DEFAULT_ALLOWED_HOSTS,
) {

    /**
     * Null when allowed, otherwise a short reason for the log. Never echoes the URL.
     * Checks run in order: raw text, then structure, then host, then path, and the
     * first problem wins.
     */
    fun reasonToReject(url: String): String? =
        rawTextProblem(url) ?: parseOrNull(url).let { uri ->
            if (uri == null) "unparsable" else structureProblem(uri) ?: hostProblem(uri) ?: pathProblem(uri)
        }

    private fun rawTextProblem(url: String): String? = when {
        url.length > MAX_LENGTH -> "too long"
        url.any { it <= ' ' || it > '~' || it == '\\' } -> "illegal character"
        else -> null
    }

    private fun parseOrNull(url: String): URI? = try {
        URI(url)
    } catch (ignored: URISyntaxException) {
        null
    }

    private fun structureProblem(uri: URI): String? = when {
        !uri.isAbsolute || uri.isOpaque -> "not an absolute hierarchical url"
        !uri.scheme.equals(SCHEME, ignoreCase = true) -> "scheme"
        uri.rawUserInfo != null -> "userinfo"
        uri.port != -1 && uri.port != HTTPS_PORT -> "port"
        uri.rawFragment != null -> "fragment"
        else -> null
    }

    private fun hostProblem(uri: URI): String? {
        val host = uri.host?.lowercase(Locale.ROOT)
        return when {
            host == null -> "no host"
            !HOST_CHARS.matches(host) -> "host characters"
            host !in allowedHosts -> "host not allowed: $host"
            else -> null
        }
    }

    private fun pathProblem(uri: URI): String? {
        val path = uri.rawPath
        return when {
            path == null -> "no path"
            !path.startsWith(REQUIRED_PATH_PREFIX) -> "path prefix"
            path.contains("..") || path.contains("//") || path.contains('%') -> "path encoding"
            else -> null
        }
    }

    companion object {
        private const val SCHEME = "https"
        private const val HTTPS_PORT = 443
        private const val MAX_LENGTH = 4096
        private const val REQUIRED_PATH_PREFIX = "/inapp-preview-campaign/"
        private val HOST_CHARS = Regex("[a-z0-9.-]+")

        /**
         * Exact host names only. Never a wildcard on amazonaws.com, anyone can
         * create a bucket there. Backend owns this list; add one line per host.
         */
        val DEFAULT_ALLOWED_HOSTS: Set<String> = setOf(
            "sk1-eventstore-inapppreviews3bucket-gd8sly0omq1m.s3.ap-northeast-2.amazonaws.com",
        )
    }
}

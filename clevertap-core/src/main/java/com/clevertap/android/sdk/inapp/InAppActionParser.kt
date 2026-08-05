package com.clevertap.android.sdk.inapp

import android.os.Bundle
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.utils.UriHelper
import java.net.URLDecoder

/**
 * Stateless parser for in-app actions triggered from an HTML/WebView surface.
 *
 * For [InAppActionType.OPEN_URL] actions the url can carry:
 *  - tracking parameters, which are surfaced as `additionalData`
 *  - a `wzrk_c2a` ([Constants.KEY_C2A]) parameter that supplies the call-to-action, and which may
 *    itself embed a deep link using the [Constants.URL_PARAM_DL_SEPARATOR] (`__dl__`) separator.
 *
 * This logic is shared by the fragment-hosted in-apps
 * ([com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFragment]) and the non-fragment
 * [CTInAppHtmlBannerOverlay] path so that both resolve actions identically.
 */
internal object InAppActionParser {

    data class ParsedAction(
        val action: CTInAppAction,
        val callToAction: String?,
        val additionalData: Bundle?
    )

    fun parse(
        action: CTInAppAction,
        callToAction: String?,
        additionalData: Bundle?,
        config: CleverTapInstanceConfig
    ): ParsedAction {
        if (action.type != InAppActionType.OPEN_URL) {
            return ParsedAction(action, callToAction, additionalData)
        }

        var resolvedAction = action
        var resolvedCallToAction = callToAction

        // All URL parameters should be tracked as additional data
        val urlActionData = UriHelper.getAllKeyValuePairs(action.actionUrl, false)

        // callToAction is handled as a parameter
        var callToActionUrlParam = urlActionData.getString(Constants.KEY_C2A)
        // no need to keep it in the data bundle
        urlActionData.remove(Constants.KEY_C2A)

        // add all additional params, overriding the url params if there is a collision
        if (additionalData != null) {
            urlActionData.putAll(additionalData)
        }

        if (callToActionUrlParam != null) {
            // check if there is a deeplink within the callToAction param
            val parts = callToActionUrlParam.split(Constants.URL_PARAM_DL_SEPARATOR)
            if (parts.size == 2) {
                // Decode it here as it is not decoded by UriHelper
                try {
                    // Extract the actual callToAction value
                    callToActionUrlParam = URLDecoder.decode(parts[0], "UTF-8")
                } catch (e: Exception) {
                    config.logger.debug("Error parsing c2a param", e)
                }
                // use the url from the callToAction param
                resolvedAction = CTInAppAction.createOpenUrlAction(parts[1])
            }
        }
        // Use the url param value only if no other value is passed
        resolvedCallToAction = resolvedCallToAction ?: callToActionUrlParam

        return ParsedAction(resolvedAction, resolvedCallToAction, urlActionData)
    }
}

package com.clevertap.android.sdk.inapp

/**
 * The in-app notification templates. [toString] returns the template identifier used in the in-app
 * payload, for example `interstitial`.
 *
 * @see CTInAppNotification.inAppType
 */
public enum class CTInAppType(private val type: String) {
    CTInAppTypeHTML("html"),
    CTInAppTypeCoverHTML("coverHtml"),
    CTInAppTypeInterstitialHTML("interstitialHtml"),
    CTInAppTypeHeaderHTML("headerHtml"),
    CTInAppTypeFooterHTML("footerHtml"),
    CTInAppTypeHalfInterstitialHTML("halfInterstitialHtml"),
    CTInAppTypeCover("cover"),
    CTInAppTypeInterstitial("interstitial"),
    CTInAppTypeHalfInterstitial("half-interstitial"),
    CTInAppTypeHeader("header-template"),
    CTInAppTypeFooter("footer-template"),
    CTInAppTypeAlert("alert-template"),
    CTInAppTypeCoverImageOnly("cover-image"),
    CTInAppTypeInterstitialImageOnly("interstitial-image"),
    CTInAppTypeHalfInterstitialImageOnly("half-interstitial-image"),
    CTInAppTypeCustomCodeTemplate("custom-code"),
    CTInAppTypePIP("pip"),
    UNKNOWN("");

    override fun toString(): String {
        return type
    }

    public companion object {
        /**
         * Looks up a template by its payload identifier (the value of [toString]).
         *
         * @param type the payload identifier, for example `cover-image`
         * @return the matching template, or [UNKNOWN] if [type] is `null` or unrecognised
         */
        @JvmStatic
        public fun fromString(type: String?): CTInAppType {
            return when (type) {
                "html" -> CTInAppTypeHTML
                "coverHtml" -> CTInAppTypeCoverHTML
                "interstitialHtml" -> CTInAppTypeInterstitialHTML
                "headerHtml" -> CTInAppTypeHeaderHTML
                "footerHtml" -> CTInAppTypeFooterHTML
                "halfInterstitialHtml" -> CTInAppTypeHalfInterstitialHTML
                "half-interstitial" -> CTInAppTypeHalfInterstitial
                "interstitial" -> CTInAppTypeInterstitial
                "cover" -> CTInAppTypeCover
                "header-template" -> CTInAppTypeHeader
                "footer-template" -> CTInAppTypeFooter
                "alert-template" -> CTInAppTypeAlert
                "cover-image" -> CTInAppTypeCoverImageOnly
                "interstitial-image" -> CTInAppTypeInterstitialImageOnly
                "half-interstitial-image" -> CTInAppTypeHalfInterstitialImageOnly
                "custom-code" -> CTInAppTypeCustomCodeTemplate
                "pip" -> CTInAppTypePIP
                else -> UNKNOWN
            }
        }
    }
}

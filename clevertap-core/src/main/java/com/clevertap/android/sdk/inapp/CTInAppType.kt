package com.clevertap.android.sdk.inapp

internal enum class CTInAppType(private val type: String) {
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
    UNKNOWN("");

    override fun toString(): String {
        return type
    }

    companion object {
        @Suppress("unused")
        fun fromString(type: String?): CTInAppType {
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
                else -> UNKNOWN
            }
        }
    }
}

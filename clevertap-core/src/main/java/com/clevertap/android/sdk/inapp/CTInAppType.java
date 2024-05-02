package com.clevertap.android.sdk.inapp;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

@RestrictTo(Scope.LIBRARY)
public enum CTInAppType {

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
    CTInAppTypeCustomCodeTemplate("custom-code");


    private final String inAppType;

    CTInAppType(String type) {
        this.inAppType = type;
    }

    @NonNull
    @Override
    public String toString() {
        return inAppType;
    }

    @SuppressWarnings({"unused"})
    public static CTInAppType fromString(String type) {
        switch (type) {
            case "html": {
                return CTInAppTypeHTML;
            }
            case "coverHtml": {
                return CTInAppTypeCoverHTML;
            }
            case "interstitialHtml": {
                return CTInAppTypeInterstitialHTML;
            }
            case "headerHtml": {
                return CTInAppTypeHeaderHTML;
            }
            case "footerHtml": {
                return CTInAppTypeFooterHTML;
            }
            case "halfInterstitialHtml": {
                return CTInAppTypeHalfInterstitialHTML;
            }
            case "half-interstitial": {
                return CTInAppTypeHalfInterstitial;
            }
            case "interstitial": {
                return CTInAppTypeInterstitial;
            }
            case "cover": {
                return CTInAppTypeCover;
            }
            case "header-template": {
                return CTInAppTypeHeader;
            }
            case "footer-template": {
                return CTInAppTypeFooter;
            }
            case "alert-template": {
                return CTInAppTypeAlert;
            }
            case "cover-image": {
                return CTInAppTypeCoverImageOnly;
            }
            case "interstitial-image": {
                return CTInAppTypeInterstitialImageOnly;
            }
            case "half-interstitial-image": {
                return CTInAppTypeHalfInterstitialImageOnly;
            }
            case "custom-code": {
                return CTInAppTypeCustomCodeTemplate;
            }
            default:
                return null;
        }
    }

}

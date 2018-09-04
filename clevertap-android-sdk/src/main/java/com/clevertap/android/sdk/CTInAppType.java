package com.clevertap.android.sdk;

enum CTInAppType {

    CTInAppTypeHTML("html"),
    CTInAppTypeCoverHTML("coverHtml"),
    CTInAppTypeInterstitialHTML("interstitialHtmlL"),
    CTInAppTypeHeaderHTML("headerHtml"),
    CTInAppTypeFooterHTML("footerHtml"),
    CTInAppTypeHalfInterstitialHTML("halfInterstitialHtml");


    private final String inAppType;
    CTInAppType(String type) {
        this.inAppType = type;
    }


    @SuppressWarnings({"unused"})
    static CTInAppType fromString(String type) {
        switch(type){
            case "html" : {
                return CTInAppTypeHTML;
            }
            case "coverHtml" : {
                return CTInAppTypeCoverHTML;
            }
            case "interstitialHtmlL" : {
                return CTInAppTypeInterstitialHTML;
            }
            case "headerHtml" : {
                return CTInAppTypeHeaderHTML;
            }
            case "footerHtml" : {
                return CTInAppTypeFooterHTML;
            }
            case "halfInterstitialHtml" : {
                return CTInAppTypeHalfInterstitialHTML;
            }
            default: return null;
        }
    }

    @Override
    public String toString() {
            return inAppType;
    }

}

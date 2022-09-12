package com.clevertap.android.sdk.inapp;

import java.util.Objects;

public class CTLocalInAppSettings {


    public static final class Builder {
        private String inAppAlertType;
        private String titleText;
        private String bodyText;
        private String positiveConfirmationBtnText;
        private String negativeConfirmationBtnText;
        private String positiveConfirmationBtnColor;

        public Builder() {
        }

        public CTLocalInAppSettings build(){
            return new CTLocalInAppSettings(this);
        }

        public CTLocalInAppSettings.Builder setInAppAlertType(String inAppAlertType) {
            this.inAppAlertType = inAppAlertType;
            return this;
        }

        public CTLocalInAppSettings.Builder setTitleText(String titleText){
            this.titleText = titleText;
            return this;
        }

        public CTLocalInAppSettings.Builder setBodyText(String bodyText){
            this.bodyText = bodyText;
            return this;
        }

        public CTLocalInAppSettings.Builder setPositiveConfirmationBtnText(String positiveConfirmationBtnText){
            this.positiveConfirmationBtnText = positiveConfirmationBtnText;
            return this;
        }

        public CTLocalInAppSettings.Builder setNegativeConfirmationBtnText(String negativeConfirmationBtnText){
            this.negativeConfirmationBtnText = negativeConfirmationBtnText;
            return this;
        }

        public CTLocalInAppSettings.Builder setPositiveConfirmationBtnColor(String positiveConfirmationBtnColor){
            this.positiveConfirmationBtnColor = positiveConfirmationBtnColor;
            return this;
        }
    }

    private final String inAppAlertType;
    private final String titleText;
    private final String bodyText;
    private final String positiveConfirmationBtnText;
    private final String negativeConfirmationBtnText;
    private final String positiveConfirmationBtnColor;

    private CTLocalInAppSettings(Builder builder) {
        inAppAlertType = builder.inAppAlertType;
        titleText = builder.titleText;
        bodyText = builder.bodyText;
        positiveConfirmationBtnText = builder.positiveConfirmationBtnText;
        negativeConfirmationBtnText = builder.negativeConfirmationBtnText;
        positiveConfirmationBtnColor = builder.positiveConfirmationBtnColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CTLocalInAppSettings that = (CTLocalInAppSettings) o;
        return Objects.equals(inAppAlertType, that.inAppAlertType) && Objects.equals(titleText, that.titleText)
                && Objects.equals(bodyText, that.bodyText) &&
                Objects.equals(positiveConfirmationBtnText, that.positiveConfirmationBtnText)
                && Objects.equals(negativeConfirmationBtnText, that.negativeConfirmationBtnText)
                && Objects.equals(positiveConfirmationBtnColor, that.positiveConfirmationBtnColor);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public String getInAppAlertType() {
        return inAppAlertType;
    }

    public String getTitleText() {
        return titleText;
    }

    public String getBodyText() {
        return bodyText;
    }

    public String getPositiveConfirmationBtnText() {
        return positiveConfirmationBtnText;
    }

    public String getNegativeConfirmationBtnText() {
        return negativeConfirmationBtnText;
    }

    public String getPositiveConfirmationBtnColor() {
        return positiveConfirmationBtnColor;
    }
}

package com.clevertap.android.sdk.inapp;


import androidx.annotation.NonNull;

import java.util.Objects;

public class CTHalfInterstitialLocalInAppBuilder {

    private final String titleText;
    private final String titleTextColor;
    private final String bodyText;
    private final String bodyTextColor;
    private final String backgroundColor;
    private final boolean followDeviceOrientation;
    private final String positiveBtnText;
    private final String negativeBtnText;
    private final String btnTextColor;
    private final String btnBackgroundColor;
    private final String btnBorderColor;// Optional
    private final String btnBorderRadius;// Optional
    private boolean doesSupportTablet;

    CTHalfInterstitialLocalInAppBuilder(Builder.Builder1 builder) {
        this.titleText = builder.titleText;
        this.titleTextColor = builder.titleTextColor;
        this.bodyText = builder.bodyText;
        this.bodyTextColor = builder.bodyTextColor;
        this.backgroundColor = builder.backgroundColor;
        this.followDeviceOrientation = builder.followDeviceOrientation;
        this.positiveBtnText = builder.positiveBtnText;
        this.negativeBtnText = builder.negativeBtnText;
        this.btnTextColor = builder.btnTextColor;
        this.btnBackgroundColor = builder.btnBackgroundColor;
        this.btnBorderColor = builder.btnBorderColor;
        this.btnBorderRadius = builder.btnBorderRadius;
    }
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        public Builder1 titleText(String titleText) {
            return new Builder1(titleText);
        }
        public static final class Builder1 {
            final String titleText;
            String titleTextColor;
            String bodyText;
            String bodyTextColor;
            String backgroundColor;
            boolean followDeviceOrientation;
            String positiveBtnText;
            String negativeBtnText;
            String btnTextColor;
            String btnBackgroundColor;
            String btnBorderColor;
            String btnBorderRadius;

            private Builder1(String titleText) {
                this.titleText = titleText;
            }
            public Builder2 titleTextColor(String titleTextColor) {
                this.titleTextColor = titleTextColor;
                return new Builder2(Builder1.this);
            }
        }
        public static final class Builder2 {
            final Builder1 builder;

            private Builder2(Builder1 builder) {
                this.builder = builder;
            }
            public Builder3 bodyText(String bodyText) {
                this.builder.bodyText = bodyText;
                return new Builder3(this.builder);
            }
        }
        public static final class Builder3 {
            final Builder1 builder;

            private Builder3(Builder1 builder) {
                this.builder = builder;
            }
            public Builder4 bodyTextColor(String bodyTextColor) {
                this.builder.bodyTextColor = bodyTextColor;
                return new Builder4(this.builder);
            }
        }
        public static final class Builder4 {
            final Builder1 builder;

            private Builder4(Builder1 builder) {
                this.builder = builder;
            }
            public Builder5 backgroundColor(String backgroundColor) {
                this.builder.backgroundColor = backgroundColor;
                return new Builder5(this.builder);
            }
        }
        public static final class Builder5 {
            final Builder1 builder;

            private Builder5(Builder1 builder) {
                this.builder = builder;
            }
            public Builder6 followDeviceOrientation(boolean followDeviceOrientation) {
                this.builder.followDeviceOrientation = followDeviceOrientation;
                return new Builder6(this.builder);
            }
        }
        public static final class Builder6 {
            final Builder1 builder;

            private Builder6(Builder1 builder) {
                this.builder = builder;
            }
            public Builder7 positiveBtnText(String positiveBtnText) {
                this.builder.positiveBtnText = positiveBtnText;
                return new Builder7(this.builder);
            }
        }
        public static final class Builder7 {
            final Builder1 builder;

            private Builder7(Builder1 builder) {
                this.builder = builder;
            }
            public Builder8 negativeBtnText(String negativeBtnText) {
                this.builder.negativeBtnText = negativeBtnText;
                return new Builder8(this.builder);
            }
        }
        public static final class Builder8 {
            final Builder1 builder;

            private Builder8(Builder1 builder) {
                this.builder = builder;
            }
            public Builder9 btnTextColor(String btnTextColor) {
                this.builder.btnTextColor = btnTextColor;
                return new Builder9(this.builder);
            }
        }
        public static final class Builder9 {
            final Builder1 builder;

            private Builder9(Builder1 builder) {
                this.builder = builder;
            }
            public Builder10 btnBackgroundColor(String btnBackgroundColor) {
                this.builder.btnBackgroundColor = btnBackgroundColor;
                return new Builder10(this.builder);
            }
        }
        public static final class Builder10 {
            final Builder1 builder;

            private Builder10(Builder1 builder) {
                this.builder = builder;
            }
            public Builder10 btnBorderColor(String btnBorderColor){
                this.builder.btnBorderColor = btnBorderColor;
                return this;
            }
            public Builder10 btnBorderRadius(String btnBorderRadius){
                this.builder.btnBorderRadius = btnBorderRadius;
                return this;
            }
            public CTHalfInterstitialLocalInAppBuilder build() {
                return new CTHalfInterstitialLocalInAppBuilder(this.builder);
            }
        }
    }

    public String titleText() {
        return this.titleText;
    }
    public String titleTextColor() {
        return this.titleTextColor;
    }
    public String bodyText() {
        return this.bodyText;
    }
    public String bodyTextColor() {
        return this.bodyTextColor;
    }
    public String backgroundColor() {
        return this.backgroundColor;
    }
    public boolean followDeviceOrientation() {
        return this.followDeviceOrientation;
    }
    public String positiveBtnText() {
        return this.positiveBtnText;
    }
    public String negativeBtnText() {
        return this.negativeBtnText;
    }
    public String btnTextColor() {
        return this.btnTextColor;
    }
    public String btnBackgroundColor() {
        return this.btnBackgroundColor;
    }
    public String btnBorderColor() {
        return this.btnBorderColor;
    }
    public String btnBorderRadius() {
        return this.btnBorderRadius;
    }

    @NonNull
    @Override
    public String toString() {
        return "CTHalfInterstitialLocalInAppBuilder(titleText=" + this.titleText + ", titleTextColor=" +
                this.titleTextColor + ", bodyText=" + this.bodyText + ", bodyTextColor=" + this.bodyTextColor +
                ", backgroundColor=" + this.backgroundColor + ", followDeviceOrientation=" +
                this.followDeviceOrientation + ", positiveBtnText=" + this.positiveBtnText +
                ", negativeBtnText=" + this.negativeBtnText + ", btnTextColor=" +
                this.btnTextColor + ", btnBackgroundColor=" + this.btnBackgroundColor +
                ", btnBorderColor=" + this.btnBorderColor + ", btnBorderRadius=" + this.btnBorderRadius + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CTHalfInterstitialLocalInAppBuilder that = (CTHalfInterstitialLocalInAppBuilder) o;
        return followDeviceOrientation == that.followDeviceOrientation &&
                Objects.equals(titleText, that.titleText) && Objects.equals(titleTextColor, that.titleTextColor)
                && Objects.equals(bodyText, that.bodyText) && Objects.equals(bodyTextColor, that.bodyTextColor)
                && Objects.equals(backgroundColor, that.backgroundColor) &&
                Objects.equals(positiveBtnText, that.positiveBtnText) &&
                Objects.equals(negativeBtnText, that.negativeBtnText) &&
                Objects.equals(btnTextColor, that.btnTextColor) &&
                Objects.equals(btnBackgroundColor, that.btnBackgroundColor) &&
                Objects.equals(btnBorderColor, that.btnBorderColor) &&
                Objects.equals(btnBorderRadius, that.btnBorderRadius);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titleText, titleTextColor, bodyText, bodyTextColor, backgroundColor,
                followDeviceOrientation, positiveBtnText, negativeBtnText, btnTextColor,
                btnBackgroundColor, btnBorderColor, btnBorderRadius);
    }
}

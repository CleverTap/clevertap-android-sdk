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
    private final String image;// Optional

    CTHalfInterstitialLocalInAppBuilder(Builder.LocalInAppBuilder localInAppBuilder) {
        this.titleText = localInAppBuilder.titleText;
        this.titleTextColor = localInAppBuilder.titleTextColor;
        this.bodyText = localInAppBuilder.bodyText;
        this.bodyTextColor = localInAppBuilder.bodyTextColor;
        this.backgroundColor = localInAppBuilder.backgroundColor;
        this.followDeviceOrientation = localInAppBuilder.followDeviceOrientation;
        this.positiveBtnText = localInAppBuilder.positiveBtnText;
        this.negativeBtnText = localInAppBuilder.negativeBtnText;
        this.btnTextColor = localInAppBuilder.btnTextColor;
        this.btnBackgroundColor = localInAppBuilder.btnBackgroundColor;
        this.btnBorderColor = localInAppBuilder.btnBorderColor;
        this.btnBorderRadius = localInAppBuilder.btnBorderRadius;
        this.image = localInAppBuilder.image;
    }
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        public LocalInAppBuilder titleText(String titleText) {
            return new LocalInAppBuilder(titleText);
        }
        public static final class LocalInAppBuilder {
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
            String image;

            private LocalInAppBuilder(String titleText) {
                this.titleText = titleText;
            }
            public TitleTextColorBuilder titleTextColor(String titleTextColor) {
                this.titleTextColor = titleTextColor;
                return new TitleTextColorBuilder(LocalInAppBuilder.this);
            }
        }
        public static final class TitleTextColorBuilder {
            final LocalInAppBuilder builder;

            private TitleTextColorBuilder(LocalInAppBuilder builder) {
                this.builder = builder;
            }
            public BodyTextBuilder bodyText(String bodyText) {
                this.builder.bodyText = bodyText;
                return new BodyTextBuilder(this.builder);
            }
        }
        public static final class BodyTextBuilder {
            final LocalInAppBuilder builder;

            private BodyTextBuilder(LocalInAppBuilder builder) {
                this.builder = builder;
            }
            public BodyTextColorBuilder bodyTextColor(String bodyTextColor) {
                this.builder.bodyTextColor = bodyTextColor;
                return new BodyTextColorBuilder(this.builder);
            }
        }
        public static final class BodyTextColorBuilder {
            final LocalInAppBuilder builder;

            private BodyTextColorBuilder(LocalInAppBuilder builder) {
                this.builder = builder;
            }
            public BackgroundColorBuilder backgroundColor(String backgroundColor) {
                this.builder.backgroundColor = backgroundColor;
                return new BackgroundColorBuilder(this.builder);
            }
        }
        public static final class BackgroundColorBuilder {
            final LocalInAppBuilder builder;

            private BackgroundColorBuilder(LocalInAppBuilder builder) {
                this.builder = builder;
            }
            public FollowDeviceOrientationBuilder followDeviceOrientation(boolean followDeviceOrientation) {
                this.builder.followDeviceOrientation = followDeviceOrientation;
                return new FollowDeviceOrientationBuilder(this.builder);
            }
        }
        public static final class FollowDeviceOrientationBuilder {
            final LocalInAppBuilder builder;

            private FollowDeviceOrientationBuilder(LocalInAppBuilder builder) {
                this.builder = builder;
            }
            public PositiveBtnTextBuilder positiveBtnText(String positiveBtnText) {
                this.builder.positiveBtnText = positiveBtnText;
                return new PositiveBtnTextBuilder(this.builder);
            }
        }
        public static final class PositiveBtnTextBuilder {
            final LocalInAppBuilder builder;

            private PositiveBtnTextBuilder(LocalInAppBuilder builder) {
                this.builder = builder;
            }
            public NegativeBtnTextBuilder negativeBtnText(String negativeBtnText) {
                this.builder.negativeBtnText = negativeBtnText;
                return new NegativeBtnTextBuilder(this.builder);
            }
        }
        public static final class NegativeBtnTextBuilder {
            final LocalInAppBuilder builder;

            private NegativeBtnTextBuilder(LocalInAppBuilder builder) {
                this.builder = builder;
            }
            public BtnTextColorBuilder btnTextColor(String btnTextColor) {
                this.builder.btnTextColor = btnTextColor;
                return new BtnTextColorBuilder(this.builder);
            }
        }
        public static final class BtnTextColorBuilder {
            final LocalInAppBuilder builder;

            private BtnTextColorBuilder(LocalInAppBuilder builder) {
                this.builder = builder;
            }
            public LocalInAppOptionalBuilder btnBackgroundColor(String btnBackgroundColor) {
                this.builder.btnBackgroundColor = btnBackgroundColor;
                return new LocalInAppOptionalBuilder(this.builder);
            }
        }
        public static final class LocalInAppOptionalBuilder {
            final LocalInAppBuilder builder;

            private LocalInAppOptionalBuilder(LocalInAppBuilder builder) {
                this.builder = builder;
            }
            public LocalInAppOptionalBuilder image(String image){
                this.builder.image = image;
                return this;
            }
            public LocalInAppOptionalBuilder btnBorderColor(String btnBorderColor){
                this.builder.btnBorderColor = btnBorderColor;
                return this;
            }
            public LocalInAppOptionalBuilder btnBorderRadius(String btnBorderRadius){
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
    public String image() {
        return this.image;
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
                ", image=" + this.image + ", btnBorderColor=" + this.btnBorderColor +
                ", btnBorderRadius=" + this.btnBorderRadius + ")";
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
                Objects.equals(image, that.image) &&
                Objects.equals(btnBorderColor, that.btnBorderColor) &&
                Objects.equals(btnBorderRadius, that.btnBorderRadius);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titleText, titleTextColor, bodyText, bodyTextColor, backgroundColor,
                followDeviceOrientation, positiveBtnText, negativeBtnText, btnTextColor,
                btnBackgroundColor, image, btnBorderColor, btnBorderRadius);
    }
}

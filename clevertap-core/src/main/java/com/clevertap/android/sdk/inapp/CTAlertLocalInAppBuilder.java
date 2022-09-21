package com.clevertap.android.sdk.inapp;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CTAlertLocalInAppBuilder {

    private final String titleText;
    private final String bodyText;
    private final boolean followDeviceOrientation;
    private final String positiveBtnText;
    private final String negativeBtnText;

    CTAlertLocalInAppBuilder(Builder.Builder1 builder) {
        this.titleText = builder.titleText;
        this.bodyText = builder.bodyText;
        this.followDeviceOrientation = builder.followDeviceOrientation;
        this.positiveBtnText = builder.positiveBtnText;
        this.negativeBtnText = builder.negativeBtnText;
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
            String bodyText;
            boolean followDeviceOrientation;
            String positiveBtnText;
            String negativeBtnText;

            private Builder1(String titleText) {
                this.titleText = titleText;
            }
            public Builder2 bodyText(String bodyText) {
                this.bodyText = bodyText;
                return new Builder2(Builder1.this);
            }
        }
        public static final class Builder2 {
            final Builder1 builder;

            private Builder2(Builder1 builder) {
                this.builder = builder;
            }
            public Builder3 followDeviceOrientation(boolean followDeviceOrientation) {
                this.builder.followDeviceOrientation = followDeviceOrientation;
                return new Builder3(this.builder);
            }
        }
        public static final class Builder3 {
            final Builder1 builder;

            private Builder3(Builder1 builder) {
                this.builder = builder;
            }
            public Builder4 positiveBtnText(String positiveBtnText) {
                this.builder.positiveBtnText = positiveBtnText;
                return new Builder4(this.builder);
            }
        }
        public static final class Builder4 {
            final Builder1 builder;

            private Builder4(Builder1 builder) {
                this.builder = builder;
            }
            public Builder5 negativeBtnText(String negativeBtnText) {
                this.builder.negativeBtnText = negativeBtnText;
                return new Builder5(this.builder);
            }
        }
        public static final class Builder5 {
            final Builder1 builder;

            private Builder5(Builder1 builder) {
                this.builder = builder;
            }
            public CTAlertLocalInAppBuilder build() {
                return new CTAlertLocalInAppBuilder(this.builder);
            }
        }
    }

    public String titleText() {
        return this.titleText;
    }
    public String bodyText() {
        return this.bodyText;
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

    @NonNull
    @Override
    public String toString() {
        return "CTAlertLocalInAppBuilder(titleText=" + this.titleText + ", bodyText=" + this.bodyText +
                ", followDeviceOrientation=" + this.followDeviceOrientation + ", positiveBtnText=" +
                this.positiveBtnText + ", negativeBtnText=" + this.negativeBtnText + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CTAlertLocalInAppBuilder that = (CTAlertLocalInAppBuilder) o;
        return followDeviceOrientation == that.followDeviceOrientation &&
                Objects.equals(titleText, that.titleText) && Objects.equals(bodyText, that.bodyText) &&
                Objects.equals(positiveBtnText, that.positiveBtnText) &&
                Objects.equals(negativeBtnText, that.negativeBtnText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titleText, bodyText, followDeviceOrientation, positiveBtnText, negativeBtnText);
    }
}

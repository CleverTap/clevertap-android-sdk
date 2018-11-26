package com.clevertap.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;

public class CTInboxStyleConfig implements Parcelable {

    private String titleColor;
    private String bodyColor;
    private String ctaColor;
    private String layoutColor;

    public CTInboxStyleConfig(){}

    private CTInboxStyleConfig(Parcel in) {
        titleColor = in.readString();
        bodyColor = in.readString();
        ctaColor = in.readString();
        layoutColor = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(titleColor);
        dest.writeString(bodyColor);
        dest.writeString(ctaColor);
        dest.writeString(layoutColor);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CTInboxStyleConfig> CREATOR = new Parcelable.Creator<CTInboxStyleConfig>() {
        @Override
        public CTInboxStyleConfig createFromParcel(Parcel in) {
            return new CTInboxStyleConfig(in);
        }

        @Override
        public CTInboxStyleConfig[] newArray(int size) {
            return new CTInboxStyleConfig[size];
        }
    };

    public String getTitleColor() {
        return titleColor;
    }

    public String getBodyColor() {
        return bodyColor;
    }

    public String getCtaColor() {
        return ctaColor;
    }

    public String getLayoutColor() {
        return layoutColor;
    }

    public void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    public void setBodyColor(String bodyColor) {
        this.bodyColor = bodyColor;
    }

    public void setCtaColor(String ctaColor) {
        this.ctaColor = ctaColor;
    }

    public void setLayoutColor(String layoutColor) {
        this.layoutColor = layoutColor;
    }
}

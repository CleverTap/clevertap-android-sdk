package com.clevertap.android.sdk;

@SuppressWarnings("WeakerAccess")
public class UTMDetail {
    private String source;
    private String medium;
    private String campaign;

    @SuppressWarnings("unused")
    public String getCampaign() {
        return campaign;
    }

    public void setCampaign(String campaign) {
        this.campaign = campaign;
    }

    @SuppressWarnings("unused")
    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    @SuppressWarnings("unused")
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}

package com.clevertap.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings({"unused", "WeakerAccess"})
public class CTInboxStyleConfig implements Parcelable {

    private final static int MAX_TABS = 2;

    private String titleColor;
    private String bodyColor;
    private String ctaColor;
    private String layoutColor;
    private String navBarColor;
    private String navBarTitle;
    private String navBarTitleColor;
    private String inboxBackgroundColor;
    private boolean usingTabs;
    private String backButtonColor;
    private String selectedTabColor;
    private String unselectedTabColor;
    private String selectedTabIndicatorColor;
    private String tabBackgroundColor;

    private String[] tabs;

    public CTInboxStyleConfig(){
        this.navBarColor = "#FFFFFF";
        this.navBarTitle = "App Inbox";
        this.navBarTitleColor = "#333333";
        this.inboxBackgroundColor = "#D3D4DA";
        this.backButtonColor = "#333333";
        this.selectedTabColor = "#1C84FE";
        this.unselectedTabColor = "#808080";
        this.selectedTabIndicatorColor = "#1C84FE";
        this.tabBackgroundColor = "#FFFFFF";
        this.tabs = new String[0];
    }

    CTInboxStyleConfig(CTInboxStyleConfig config){
        this.navBarColor = config.navBarColor;
        this.navBarTitle = config.navBarTitle;
        this.navBarTitleColor = config.navBarTitleColor;
        this.inboxBackgroundColor = config.inboxBackgroundColor;
        this.backButtonColor = config.backButtonColor;
        this.selectedTabColor = config.selectedTabColor;
        this.unselectedTabColor = config.unselectedTabColor;
        this.selectedTabIndicatorColor = config.selectedTabIndicatorColor;
        this.tabBackgroundColor = config.tabBackgroundColor;
        this.tabs = (config.tabs == null) ? new String[0] : Arrays.copyOf(config.tabs, config.tabs.length);
    }

    protected CTInboxStyleConfig(Parcel in) {
        titleColor = in.readString();
        bodyColor = in.readString();
        ctaColor = in.readString();
        layoutColor = in.readString();
        navBarColor = in.readString();
        navBarTitle = in.readString();
        navBarTitleColor = in.readString();
        inboxBackgroundColor = in.readString();
        tabs = in.createStringArray();
        usingTabs = in.readByte() != 0x00;
        backButtonColor = in.readString();
        selectedTabColor = in.readString();
        unselectedTabColor = in.readString();
        selectedTabIndicatorColor = in.readString();
        tabBackgroundColor = in.readString();
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
        dest.writeString(navBarColor);
        dest.writeString(navBarTitle);
        dest.writeString(navBarTitleColor);
        dest.writeString(inboxBackgroundColor);
        dest.writeStringArray(tabs);
        dest.writeByte((byte) (usingTabs ? 0x01 : 0x00));
        dest.writeString(backButtonColor);
        dest.writeString(selectedTabColor);
        dest.writeString(unselectedTabColor);
        dest.writeString(selectedTabIndicatorColor);
        dest.writeString(tabBackgroundColor);
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

    String getTitleColor() {
        return titleColor;
    }

    void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    String getBodyColor() {
        return bodyColor;
    }

    void setBodyColor(String bodyColor) {
        this.bodyColor = bodyColor;
    }

    String getCtaColor() {
        return ctaColor;
    }

    void setCtaColor(String ctaColor) {
        this.ctaColor = ctaColor;
    }

    String getLayoutColor() {
        return layoutColor;
    }

    void setLayoutColor(String layoutColor) {
        this.layoutColor = layoutColor;
    }

    public String getNavBarColor() {
        return navBarColor;
    }

    public void setNavBarColor(String navBarColor) {
        this.navBarColor = navBarColor;
    }

    public String getNavBarTitle() {
        return navBarTitle;
    }

    public void setNavBarTitle(String navBarTitle) {
        this.navBarTitle = navBarTitle;
    }

    public String getNavBarTitleColor() {
        return navBarTitleColor;
    }

    public void setNavBarTitleColor(String navBarTitleColor) {
        this.navBarTitleColor = navBarTitleColor;
    }

    public String getInboxBackgroundColor() {
        return inboxBackgroundColor;
    }

    public void setInboxBackgroundColor(String inboxBackgroundColor) {
        this.inboxBackgroundColor = inboxBackgroundColor;
    }
    public void setTabs(ArrayList<String>tabs) {
        if (tabs == null || tabs.size() <= 0) return;

        ArrayList<String>toAdd;
        if (tabs.size() > MAX_TABS) {
            toAdd = new ArrayList<>(tabs.subList(0, MAX_TABS));
        } else {
            toAdd = tabs;
        }
        this.tabs = toAdd.toArray(new String[0]);
    }

    public ArrayList<String> getTabs() {
        return this.tabs == null ? new ArrayList<String>() : new ArrayList<>(Arrays.asList(this.tabs));
    }

    boolean isUsingTabs() {
        return (tabs != null && tabs.length > 0);
    }

    public String getBackButtonColor() {
        return backButtonColor;
    }

    public void setBackButtonColor(String backButtonColor) {
        this.backButtonColor = backButtonColor;
    }

    public String getSelectedTabColor() {
        return selectedTabColor;
    }

    public void setSelectedTabColor(String selectedTabColor) {
        this.selectedTabColor = selectedTabColor;
    }

    public String getUnselectedTabColor() {
        return unselectedTabColor;
    }

    public void setUnselectedTabColor(String unselectedTabColor) {
        this.unselectedTabColor = unselectedTabColor;
    }

    public String getSelectedTabIndicatorColor() {
        return selectedTabIndicatorColor;
    }

    public void setSelectedTabIndicatorColor(String selectedTabIndicatorColor) {
        this.selectedTabIndicatorColor = selectedTabIndicatorColor;
    }

    public String getTabBackgroundColor() {
        return tabBackgroundColor;
    }

    public void setTabBackgroundColor(String tabBackgroundColor) {
        this.tabBackgroundColor = tabBackgroundColor;
    }
}

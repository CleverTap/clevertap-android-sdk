package com.clevertap.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;
import com.clevertap.android.sdk.inbox.CTInboxActivity;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class has all the parameters required to configure the styling of your {@link CTInboxActivity}
 * All the setter methods are public and the object of this class is made immutable by {@link CleverTapAPI}
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CTInboxStyleConfig implements Parcelable {

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CTInboxStyleConfig> CREATOR
            = new Parcelable.Creator<CTInboxStyleConfig>() {
        @Override
        public CTInboxStyleConfig createFromParcel(Parcel in) {
            return new CTInboxStyleConfig(in);
        }

        @Override
        public CTInboxStyleConfig[] newArray(int size) {
            return new CTInboxStyleConfig[size];
        }
    };

    private final static int MAX_TABS = 2;

    private String backButtonColor;

    private String firstTabTitle;

    private String inboxBackgroundColor;

    private String navBarColor;

    private String navBarTitle;

    private String navBarTitleColor;

    private String noMessageViewText;

    private String noMessageViewTextColor;

    private String selectedTabColor;

    private String selectedTabIndicatorColor;

    private String tabBackgroundColor;

    private String[] tabs;

    private String unselectedTabColor;

    public CTInboxStyleConfig() {
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
        this.noMessageViewText = "No Message(s) to show";
        this.noMessageViewTextColor = "#000000";
        this.firstTabTitle = "ALL";
    }

    CTInboxStyleConfig(CTInboxStyleConfig config) {
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
        this.noMessageViewText = config.noMessageViewText;
        this.noMessageViewTextColor = config.noMessageViewTextColor;
        this.firstTabTitle = config.firstTabTitle;
    }

    protected CTInboxStyleConfig(Parcel in) {
        navBarColor = in.readString();
        navBarTitle = in.readString();
        navBarTitleColor = in.readString();
        inboxBackgroundColor = in.readString();
        tabs = in.createStringArray();
        backButtonColor = in.readString();
        selectedTabColor = in.readString();
        unselectedTabColor = in.readString();
        selectedTabIndicatorColor = in.readString();
        tabBackgroundColor = in.readString();
        noMessageViewText = in.readString();
        noMessageViewTextColor = in.readString();
        firstTabTitle = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getBackButtonColor() {
        return backButtonColor;
    }

    /**
     * Sets the color of the back button on the top navigation toolbar
     *
     * @param backButtonColor String - hexcode of the color
     */
    public void setBackButtonColor(String backButtonColor) {
        this.backButtonColor = backButtonColor;
    }

    public String getFirstTabTitle() {
        return firstTabTitle;
    }

    /**
     * Sets the title of the first tab of the App Inbox
     *
     * @param title String - title of the first tab
     */
    public void setFirstTabTitle(String title) {
        firstTabTitle = title;
    }

    public String getInboxBackgroundColor() {
        return inboxBackgroundColor;
    }

    /**
     * Sets the background color for the entire inbox
     *
     * @param inboxBackgroundColor - String - hexcode of the color
     */
    public void setInboxBackgroundColor(String inboxBackgroundColor) {
        this.inboxBackgroundColor = inboxBackgroundColor;
    }

    public String getNavBarColor() {
        return navBarColor;
    }

    /**
     * Sets the color for the top navigation toolbar
     *
     * @param navBarColor String - hexcode of the color
     */
    public void setNavBarColor(String navBarColor) {
        this.navBarColor = navBarColor;
    }

    public String getNavBarTitle() {
        return navBarTitle;
    }

    /**
     * Sets the text for the top navigation toolbar
     *
     * @param navBarTitle String
     */
    public void setNavBarTitle(String navBarTitle) {
        this.navBarTitle = navBarTitle;
    }

    public String getNavBarTitleColor() {
        return navBarTitleColor;
    }

    /**
     * Sets the color for the title in the top navigation toolbar
     *
     * @param navBarTitleColor String - hexcode of the color
     */
    public void setNavBarTitleColor(String navBarTitleColor) {
        this.navBarTitleColor = navBarTitleColor;
    }

    public String getNoMessageViewText() {
        return noMessageViewText;
    }

    /**
     * Sets the text for when there are no messages in the App Inbox
     *
     * @param noMessageViewText String
     */
    public void setNoMessageViewText(String noMessageViewText) {
        this.noMessageViewText = noMessageViewText;
    }

    public String getNoMessageViewTextColor() {
        return noMessageViewTextColor;
    }

    /**
     * Sets the color of the text for when there are no messages in the App Inbox
     *
     * @param noMessageViewTextColor String - hexcode of the color
     */
    public void setNoMessageViewTextColor(String noMessageViewTextColor) {
        this.noMessageViewTextColor = noMessageViewTextColor;
    }

    public String getSelectedTabColor() {
        return selectedTabColor;
    }

    /**
     * Sets the color of the selected tab
     *
     * @param selectedTabColor String - hexcode of the color
     */
    public void setSelectedTabColor(String selectedTabColor) {
        this.selectedTabColor = selectedTabColor;
    }

    public String getSelectedTabIndicatorColor() {
        return selectedTabIndicatorColor;
    }

    /**
     * Sets the color of the indicator of the selected tab
     *
     * @param selectedTabIndicatorColor String - hexcode of the color
     */
    public void setSelectedTabIndicatorColor(String selectedTabIndicatorColor) {
        this.selectedTabIndicatorColor = selectedTabIndicatorColor;
    }

    public String getTabBackgroundColor() {
        return tabBackgroundColor;
    }

    /**
     * Sets the background color for the tabs
     *
     * @param tabBackgroundColor String - hexcode of the color
     */
    public void setTabBackgroundColor(String tabBackgroundColor) {
        this.tabBackgroundColor = tabBackgroundColor;
    }

    public ArrayList<String> getTabs() {
        return this.tabs == null ? new ArrayList<String>() : new ArrayList<>(Arrays.asList(this.tabs));
    }

    /**
     * Sets the name of the optional two tabs.
     * The contents of the tabs are filtered based on the name of the tab.
     *
     * @param tabs ArrayList of Strings
     */
    public void setTabs(ArrayList<String> tabs) {
        if (tabs == null || tabs.size() <= 0) {
            return;
        }

        ArrayList<String> toAdd;
        if (tabs.size() > MAX_TABS) {
            toAdd = new ArrayList<>(tabs.subList(0, MAX_TABS));
        } else {
            toAdd = tabs;
        }
        this.tabs = toAdd.toArray(new String[0]);
    }

    public String getUnselectedTabColor() {
        return unselectedTabColor;
    }

    /**
     * Sets the color of the unselected tab
     *
     * @param unselectedTabColor String - hexcode of the color
     */
    public void setUnselectedTabColor(String unselectedTabColor) {
        this.unselectedTabColor = unselectedTabColor;
    }

    public boolean isUsingTabs() {
        return (tabs != null && tabs.length > 0);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(navBarColor);
        dest.writeString(navBarTitle);
        dest.writeString(navBarTitleColor);
        dest.writeString(inboxBackgroundColor);
        dest.writeStringArray(tabs);
        dest.writeString(backButtonColor);
        dest.writeString(selectedTabColor);
        dest.writeString(unselectedTabColor);
        dest.writeString(selectedTabIndicatorColor);
        dest.writeString(tabBackgroundColor);
        dest.writeString(noMessageViewText);
        dest.writeString(noMessageViewTextColor);
        dest.writeString(firstTabTitle);
    }
}

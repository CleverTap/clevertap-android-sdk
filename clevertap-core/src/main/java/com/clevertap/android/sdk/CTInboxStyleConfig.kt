package com.clevertap.android.sdk

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.inbox.CTInboxActivity

/**
 * This class has all the parameters required to configure the styling of your [CTInboxActivity]
 * All the setter methods are public and the object of this class is made immutable by [CleverTapAPI]
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
public open class CTInboxStyleConfig() : Parcelable {

    private var backButtonColorValue: String? = "#333333"
    private var firstTabTitleValue: String? = "ALL"
    private var inboxBackgroundColorValue: String? = "#D3D4DA"
    private var navBarColorValue: String? = "#FFFFFF"
    private var navBarTitleValue: String? = "App Inbox"
    private var navBarTitleColorValue: String? = "#333333"
    private var noMessageViewTextValue: String? = "No Message(s) to show"
    private var noMessageViewTextColorValue: String? = "#000000"
    private var selectedTabColorValue: String? = "#1C84FE"
    private var selectedTabIndicatorColorValue: String? = "#1C84FE"
    private var tabBackgroundColorValue: String? = "#FFFFFF"
    private var tabsValue: Array<String?>? = emptyArray()
    private var unselectedTabColorValue: String? = "#808080"

    internal constructor(config: CTInboxStyleConfig) : this() {
        navBarColorValue = config.navBarColorValue
        navBarTitleValue = config.navBarTitleValue
        navBarTitleColorValue = config.navBarTitleColorValue
        inboxBackgroundColorValue = config.inboxBackgroundColorValue
        backButtonColorValue = config.backButtonColorValue
        selectedTabColorValue = config.selectedTabColorValue
        unselectedTabColorValue = config.unselectedTabColorValue
        selectedTabIndicatorColorValue = config.selectedTabIndicatorColorValue
        tabBackgroundColorValue = config.tabBackgroundColorValue
        tabsValue = config.tabsValue?.copyOf() ?: emptyArray()
        noMessageViewTextValue = config.noMessageViewTextValue
        noMessageViewTextColorValue = config.noMessageViewTextColorValue
        firstTabTitleValue = config.firstTabTitleValue
    }

    protected constructor(parcel: Parcel) : this() {
        navBarColorValue = parcel.readString()
        navBarTitleValue = parcel.readString()
        navBarTitleColorValue = parcel.readString()
        inboxBackgroundColorValue = parcel.readString()
        tabsValue = parcel.createStringArray()
        backButtonColorValue = parcel.readString()
        selectedTabColorValue = parcel.readString()
        unselectedTabColorValue = parcel.readString()
        selectedTabIndicatorColorValue = parcel.readString()
        tabBackgroundColorValue = parcel.readString()
        noMessageViewTextValue = parcel.readString()
        noMessageViewTextColorValue = parcel.readString()
        firstTabTitleValue = parcel.readString()
    }

    override fun describeContents(): Int = 0

    /**
     * The color of the back button on the top navigation toolbar.
     *
     * Setter: sets the color of the back button on the top navigation toolbar
     * (String - hexcode of the color).
     */
    public open fun getBackButtonColor(): String? = backButtonColorValue

    /**
     * Sets the color of the back button on the top navigation toolbar.
     *
     * @param backButtonColor the hex code of the color, for example `#000000`
     */
    public open fun setBackButtonColor(backButtonColor: String?) {
        backButtonColorValue = backButtonColor
    }

    /** Kotlin property syntax, equivalent to [getBackButtonColor] / [setBackButtonColor]. */
    public var backButtonColor: String?
        @JvmSynthetic @JvmName("ktGetBackButtonColor") inline get() = getBackButtonColor()
        @JvmSynthetic @JvmName("ktSetBackButtonColor") inline set(value) = setBackButtonColor(value)

    /**
     * The title of the first tab of the App Inbox.
     *
     * Setter: sets the title of the first tab of the App Inbox (String - title of the first tab).
     */
    public open fun getFirstTabTitle(): String? = firstTabTitleValue

    /**
     * Sets the title of the first tab of the App Inbox (the tab that shows all messages).
     *
     * @param title the title
     */
    public open fun setFirstTabTitle(title: String?) {
        firstTabTitleValue = title
    }

    /** Kotlin property syntax, equivalent to [getFirstTabTitle] / [setFirstTabTitle]. */
    public var firstTabTitle: String?
        @JvmSynthetic @JvmName("ktGetFirstTabTitle") inline get() = getFirstTabTitle()
        @JvmSynthetic @JvmName("ktSetFirstTabTitle") inline set(value) = setFirstTabTitle(value)

    /**
     * The background color for the entire inbox.
     *
     * Setter: sets the background color for the entire inbox (String - hexcode of the color).
     */
    public open fun getInboxBackgroundColor(): String? = inboxBackgroundColorValue

    /**
     * Sets the background color for the entire inbox.
     *
     * @param inboxBackgroundColor the hex code of the color
     */
    public open fun setInboxBackgroundColor(inboxBackgroundColor: String?) {
        inboxBackgroundColorValue = inboxBackgroundColor
    }

    /** Kotlin property syntax, equivalent to [getInboxBackgroundColor] / [setInboxBackgroundColor]. */
    public var inboxBackgroundColor: String?
        @JvmSynthetic @JvmName("ktGetInboxBackgroundColor") inline get() = getInboxBackgroundColor()
        @JvmSynthetic @JvmName("ktSetInboxBackgroundColor") inline set(value) = setInboxBackgroundColor(value)

    /**
     * The color for the top navigation toolbar.
     *
     * Setter: sets the color for the top navigation toolbar (String - hexcode of the color).
     */
    public open fun getNavBarColor(): String? = navBarColorValue

    /**
     * Sets the color of the top navigation toolbar.
     *
     * @param navBarColor the hex code of the color
     */
    public open fun setNavBarColor(navBarColor: String?) {
        navBarColorValue = navBarColor
    }

    /** Kotlin property syntax, equivalent to [getNavBarColor] / [setNavBarColor]. */
    public var navBarColor: String?
        @JvmSynthetic @JvmName("ktGetNavBarColor") inline get() = getNavBarColor()
        @JvmSynthetic @JvmName("ktSetNavBarColor") inline set(value) = setNavBarColor(value)

    /**
     * The text for the top navigation toolbar.
     *
     * Setter: sets the text for the top navigation toolbar (String).
     */
    public open fun getNavBarTitle(): String? = navBarTitleValue

    /**
     * Sets the title shown in the top navigation toolbar.
     *
     * @param navBarTitle the title
     */
    public open fun setNavBarTitle(navBarTitle: String?) {
        navBarTitleValue = navBarTitle
    }

    /** Kotlin property syntax, equivalent to [getNavBarTitle] / [setNavBarTitle]. */
    public var navBarTitle: String?
        @JvmSynthetic @JvmName("ktGetNavBarTitle") inline get() = getNavBarTitle()
        @JvmSynthetic @JvmName("ktSetNavBarTitle") inline set(value) = setNavBarTitle(value)

    /**
     * The color for the title in the top navigation toolbar.
     *
     * Setter: sets the color for the title in the top navigation toolbar (String - hexcode of the color).
     */
    public open fun getNavBarTitleColor(): String? = navBarTitleColorValue

    /**
     * Sets the color of the title in the top navigation toolbar.
     *
     * @param navBarTitleColor the hex code of the color
     */
    public open fun setNavBarTitleColor(navBarTitleColor: String?) {
        navBarTitleColorValue = navBarTitleColor
    }

    /** Kotlin property syntax, equivalent to [getNavBarTitleColor] / [setNavBarTitleColor]. */
    public var navBarTitleColor: String?
        @JvmSynthetic @JvmName("ktGetNavBarTitleColor") inline get() = getNavBarTitleColor()
        @JvmSynthetic @JvmName("ktSetNavBarTitleColor") inline set(value) = setNavBarTitleColor(value)

    /**
     * The text for when there are no messages in the App Inbox.
     *
     * Setter: sets the text for when there are no messages in the App Inbox (String).
     */
    public open fun getNoMessageViewText(): String? = noMessageViewTextValue

    /**
     * Sets the text shown when there are no messages in the App Inbox.
     *
     * @param noMessageViewText the text
     */
    public open fun setNoMessageViewText(noMessageViewText: String?) {
        noMessageViewTextValue = noMessageViewText
    }

    /** Kotlin property syntax, equivalent to [getNoMessageViewText] / [setNoMessageViewText]. */
    public var noMessageViewText: String?
        @JvmSynthetic @JvmName("ktGetNoMessageViewText") inline get() = getNoMessageViewText()
        @JvmSynthetic @JvmName("ktSetNoMessageViewText") inline set(value) = setNoMessageViewText(value)

    /**
     * The color of the text for when there are no messages in the App Inbox.
     *
     * Setter: sets the color of the text for when there are no messages in the App Inbox
     * (String - hexcode of the color).
     */
    public open fun getNoMessageViewTextColor(): String? = noMessageViewTextColorValue

    /**
     * Sets the color of the text shown when there are no messages in the App Inbox.
     *
     * @param noMessageViewTextColor the hex code of the color
     */
    public open fun setNoMessageViewTextColor(noMessageViewTextColor: String?) {
        noMessageViewTextColorValue = noMessageViewTextColor
    }

    /** Kotlin property syntax, equivalent to [getNoMessageViewTextColor] / [setNoMessageViewTextColor]. */
    public var noMessageViewTextColor: String?
        @JvmSynthetic @JvmName("ktGetNoMessageViewTextColor") inline get() = getNoMessageViewTextColor()
        @JvmSynthetic @JvmName("ktSetNoMessageViewTextColor") inline set(value) = setNoMessageViewTextColor(value)

    /**
     * The color of the selected tab.
     *
     * Setter: sets the color of the selected tab (String - hexcode of the color).
     */
    public open fun getSelectedTabColor(): String? = selectedTabColorValue

    /**
     * Sets the color of the selected tab.
     *
     * @param selectedTabColor the hex code of the color
     */
    public open fun setSelectedTabColor(selectedTabColor: String?) {
        selectedTabColorValue = selectedTabColor
    }

    /** Kotlin property syntax, equivalent to [getSelectedTabColor] / [setSelectedTabColor]. */
    public var selectedTabColor: String?
        @JvmSynthetic @JvmName("ktGetSelectedTabColor") inline get() = getSelectedTabColor()
        @JvmSynthetic @JvmName("ktSetSelectedTabColor") inline set(value) = setSelectedTabColor(value)

    /**
     * The color of the indicator of the selected tab.
     *
     * Setter: sets the color of the indicator of the selected tab (String - hexcode of the color).
     */
    public open fun getSelectedTabIndicatorColor(): String? = selectedTabIndicatorColorValue

    /**
     * Sets the color of the indicator of the selected tab.
     *
     * @param selectedTabIndicatorColor the hex code of the color
     */
    public open fun setSelectedTabIndicatorColor(selectedTabIndicatorColor: String?) {
        selectedTabIndicatorColorValue = selectedTabIndicatorColor
    }

    /** Kotlin property syntax, equivalent to [getSelectedTabIndicatorColor] / [setSelectedTabIndicatorColor]. */
    public var selectedTabIndicatorColor: String?
        @JvmSynthetic @JvmName("ktGetSelectedTabIndicatorColor") inline get() = getSelectedTabIndicatorColor()
        @JvmSynthetic @JvmName("ktSetSelectedTabIndicatorColor") inline set(value) = setSelectedTabIndicatorColor(value)

    /**
     * The background color for the tabs.
     *
     * Setter: sets the background color for the tabs (String - hexcode of the color).
     */
    public open fun getTabBackgroundColor(): String? = tabBackgroundColorValue

    /**
     * Sets the background color of the tabs.
     *
     * @param tabBackgroundColor the hex code of the color
     */
    public open fun setTabBackgroundColor(tabBackgroundColor: String?) {
        tabBackgroundColorValue = tabBackgroundColor
    }

    /** Kotlin property syntax, equivalent to [getTabBackgroundColor] / [setTabBackgroundColor]. */
    public var tabBackgroundColor: String?
        @JvmSynthetic @JvmName("ktGetTabBackgroundColor") inline get() = getTabBackgroundColor()
        @JvmSynthetic @JvmName("ktSetTabBackgroundColor") inline set(value) = setTabBackgroundColor(value)

    /**
     * The names of the optional two tabs. Reading returns a copy (never `null`).
     *
     * Setter: sets the name of the optional two tabs.
     * The contents of the tabs are filtered based on the name of the tab.
     * A `null` or empty list is ignored; only the first two entries are kept.
     */
    @Suppress("UNCHECKED_CAST")
    public open fun getTabs(): ArrayList<String> = ArrayList(tabsValue.orEmpty().asList()) as ArrayList<String>

    /**
     * Sets the names of up to two extra tabs; messages are shown in a tab when they carry its name as a
     * tag. A `null` or empty list is ignored, and only the first two entries are kept.
     *
     * @param tabs the tab names
     */
    public open fun setTabs(tabs: ArrayList<String>?) {
        if (tabs.isNullOrEmpty()) {
            return
        }
        tabsValue = tabs.take(MAX_TABS).toTypedArray<String?>()
    }

    /** Kotlin property syntax, equivalent to [getTabs] / [setTabs]. */
    public var tabs: ArrayList<String>?
        @JvmSynthetic @JvmName("ktGetTabs") inline get() = getTabs()
        @JvmSynthetic @JvmName("ktSetTabs") inline set(value) = setTabs(value)

    /**
     * The color of the unselected tab.
     *
     * Setter: sets the color of the unselected tab (String - hexcode of the color).
     */
    public open fun getUnselectedTabColor(): String? = unselectedTabColorValue

    /**
     * Sets the color of the unselected tabs.
     *
     * @param unselectedTabColor the hex code of the color
     */
    public open fun setUnselectedTabColor(unselectedTabColor: String?) {
        unselectedTabColorValue = unselectedTabColor
    }

    /** Kotlin property syntax, equivalent to [getUnselectedTabColor] / [setUnselectedTabColor]. */
    public var unselectedTabColor: String?
        @JvmSynthetic @JvmName("ktGetUnselectedTabColor") inline get() = getUnselectedTabColor()
        @JvmSynthetic @JvmName("ktSetUnselectedTabColor") inline set(value) = setUnselectedTabColor(value)

    /**
     * Returns whether tabs were configured with [setTabs].
     *
     * @return `true` if at least one tab name is set
     */
    public open fun isUsingTabs(): Boolean = !tabsValue.isNullOrEmpty()

    /** Kotlin property syntax, equivalent to [isUsingTabs]. */
    public val isUsingTabs: Boolean
        @JvmSynthetic @JvmName("ktIsUsingTabs") inline get() = isUsingTabs()

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(navBarColorValue)
        dest.writeString(navBarTitleValue)
        dest.writeString(navBarTitleColorValue)
        dest.writeString(inboxBackgroundColorValue)
        dest.writeStringArray(tabsValue)
        dest.writeString(backButtonColorValue)
        dest.writeString(selectedTabColorValue)
        dest.writeString(unselectedTabColorValue)
        dest.writeString(selectedTabIndicatorColorValue)
        dest.writeString(tabBackgroundColorValue)
        dest.writeString(noMessageViewTextValue)
        dest.writeString(noMessageViewTextColorValue)
        dest.writeString(firstTabTitleValue)
    }

    public companion object {

        /** [Parcelable.Creator] that restores a [CTInboxStyleConfig] from a [Parcel]. */
        @JvmField
        public val CREATOR: Parcelable.Creator<CTInboxStyleConfig> = object : Parcelable.Creator<CTInboxStyleConfig> {
            override fun createFromParcel(parcel: Parcel): CTInboxStyleConfig = CTInboxStyleConfig(parcel)

            override fun newArray(size: Int): Array<CTInboxStyleConfig?> = arrayOfNulls(size)
        }

        private const val MAX_TABS = 2
    }
}

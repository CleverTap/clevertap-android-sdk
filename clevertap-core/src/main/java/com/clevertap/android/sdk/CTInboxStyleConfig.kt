package com.clevertap.android.sdk

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.inbox.CTInboxActivity

/**
 * This class has all the parameters required to configure the styling of your [CTInboxActivity]
 * All the setter methods are public and the object of this class is made immutable by [CleverTapAPI]
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class CTInboxStyleConfig() : Parcelable {

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
    open fun getBackButtonColor(): String? = backButtonColorValue

    open fun setBackButtonColor(backButtonColor: String?) {
        backButtonColorValue = backButtonColor
    }

    /** Kotlin property syntax, equivalent to [getBackButtonColor] / [setBackButtonColor]. */
    var backButtonColor: String?
        @JvmSynthetic @JvmName("ktGetBackButtonColor") inline get() = getBackButtonColor()
        @JvmSynthetic @JvmName("ktSetBackButtonColor") inline set(value) = setBackButtonColor(value)

    /**
     * The title of the first tab of the App Inbox.
     *
     * Setter: sets the title of the first tab of the App Inbox (String - title of the first tab).
     */
    open fun getFirstTabTitle(): String? = firstTabTitleValue

    open fun setFirstTabTitle(title: String?) {
        firstTabTitleValue = title
    }

    /** Kotlin property syntax, equivalent to [getFirstTabTitle] / [setFirstTabTitle]. */
    var firstTabTitle: String?
        @JvmSynthetic @JvmName("ktGetFirstTabTitle") inline get() = getFirstTabTitle()
        @JvmSynthetic @JvmName("ktSetFirstTabTitle") inline set(value) = setFirstTabTitle(value)

    /**
     * The background color for the entire inbox.
     *
     * Setter: sets the background color for the entire inbox (String - hexcode of the color).
     */
    open fun getInboxBackgroundColor(): String? = inboxBackgroundColorValue

    open fun setInboxBackgroundColor(inboxBackgroundColor: String?) {
        inboxBackgroundColorValue = inboxBackgroundColor
    }

    /** Kotlin property syntax, equivalent to [getInboxBackgroundColor] / [setInboxBackgroundColor]. */
    var inboxBackgroundColor: String?
        @JvmSynthetic @JvmName("ktGetInboxBackgroundColor") inline get() = getInboxBackgroundColor()
        @JvmSynthetic @JvmName("ktSetInboxBackgroundColor") inline set(value) = setInboxBackgroundColor(value)

    /**
     * The color for the top navigation toolbar.
     *
     * Setter: sets the color for the top navigation toolbar (String - hexcode of the color).
     */
    open fun getNavBarColor(): String? = navBarColorValue

    open fun setNavBarColor(navBarColor: String?) {
        navBarColorValue = navBarColor
    }

    /** Kotlin property syntax, equivalent to [getNavBarColor] / [setNavBarColor]. */
    var navBarColor: String?
        @JvmSynthetic @JvmName("ktGetNavBarColor") inline get() = getNavBarColor()
        @JvmSynthetic @JvmName("ktSetNavBarColor") inline set(value) = setNavBarColor(value)

    /**
     * The text for the top navigation toolbar.
     *
     * Setter: sets the text for the top navigation toolbar (String).
     */
    open fun getNavBarTitle(): String? = navBarTitleValue

    open fun setNavBarTitle(navBarTitle: String?) {
        navBarTitleValue = navBarTitle
    }

    /** Kotlin property syntax, equivalent to [getNavBarTitle] / [setNavBarTitle]. */
    var navBarTitle: String?
        @JvmSynthetic @JvmName("ktGetNavBarTitle") inline get() = getNavBarTitle()
        @JvmSynthetic @JvmName("ktSetNavBarTitle") inline set(value) = setNavBarTitle(value)

    /**
     * The color for the title in the top navigation toolbar.
     *
     * Setter: sets the color for the title in the top navigation toolbar (String - hexcode of the color).
     */
    open fun getNavBarTitleColor(): String? = navBarTitleColorValue

    open fun setNavBarTitleColor(navBarTitleColor: String?) {
        navBarTitleColorValue = navBarTitleColor
    }

    /** Kotlin property syntax, equivalent to [getNavBarTitleColor] / [setNavBarTitleColor]. */
    var navBarTitleColor: String?
        @JvmSynthetic @JvmName("ktGetNavBarTitleColor") inline get() = getNavBarTitleColor()
        @JvmSynthetic @JvmName("ktSetNavBarTitleColor") inline set(value) = setNavBarTitleColor(value)

    /**
     * The text for when there are no messages in the App Inbox.
     *
     * Setter: sets the text for when there are no messages in the App Inbox (String).
     */
    open fun getNoMessageViewText(): String? = noMessageViewTextValue

    open fun setNoMessageViewText(noMessageViewText: String?) {
        noMessageViewTextValue = noMessageViewText
    }

    /** Kotlin property syntax, equivalent to [getNoMessageViewText] / [setNoMessageViewText]. */
    var noMessageViewText: String?
        @JvmSynthetic @JvmName("ktGetNoMessageViewText") inline get() = getNoMessageViewText()
        @JvmSynthetic @JvmName("ktSetNoMessageViewText") inline set(value) = setNoMessageViewText(value)

    /**
     * The color of the text for when there are no messages in the App Inbox.
     *
     * Setter: sets the color of the text for when there are no messages in the App Inbox
     * (String - hexcode of the color).
     */
    open fun getNoMessageViewTextColor(): String? = noMessageViewTextColorValue

    open fun setNoMessageViewTextColor(noMessageViewTextColor: String?) {
        noMessageViewTextColorValue = noMessageViewTextColor
    }

    /** Kotlin property syntax, equivalent to [getNoMessageViewTextColor] / [setNoMessageViewTextColor]. */
    var noMessageViewTextColor: String?
        @JvmSynthetic @JvmName("ktGetNoMessageViewTextColor") inline get() = getNoMessageViewTextColor()
        @JvmSynthetic @JvmName("ktSetNoMessageViewTextColor") inline set(value) = setNoMessageViewTextColor(value)

    /**
     * The color of the selected tab.
     *
     * Setter: sets the color of the selected tab (String - hexcode of the color).
     */
    open fun getSelectedTabColor(): String? = selectedTabColorValue

    open fun setSelectedTabColor(selectedTabColor: String?) {
        selectedTabColorValue = selectedTabColor
    }

    /** Kotlin property syntax, equivalent to [getSelectedTabColor] / [setSelectedTabColor]. */
    var selectedTabColor: String?
        @JvmSynthetic @JvmName("ktGetSelectedTabColor") inline get() = getSelectedTabColor()
        @JvmSynthetic @JvmName("ktSetSelectedTabColor") inline set(value) = setSelectedTabColor(value)

    /**
     * The color of the indicator of the selected tab.
     *
     * Setter: sets the color of the indicator of the selected tab (String - hexcode of the color).
     */
    open fun getSelectedTabIndicatorColor(): String? = selectedTabIndicatorColorValue

    open fun setSelectedTabIndicatorColor(selectedTabIndicatorColor: String?) {
        selectedTabIndicatorColorValue = selectedTabIndicatorColor
    }

    /** Kotlin property syntax, equivalent to [getSelectedTabIndicatorColor] / [setSelectedTabIndicatorColor]. */
    var selectedTabIndicatorColor: String?
        @JvmSynthetic @JvmName("ktGetSelectedTabIndicatorColor") inline get() = getSelectedTabIndicatorColor()
        @JvmSynthetic @JvmName("ktSetSelectedTabIndicatorColor") inline set(value) = setSelectedTabIndicatorColor(value)

    /**
     * The background color for the tabs.
     *
     * Setter: sets the background color for the tabs (String - hexcode of the color).
     */
    open fun getTabBackgroundColor(): String? = tabBackgroundColorValue

    open fun setTabBackgroundColor(tabBackgroundColor: String?) {
        tabBackgroundColorValue = tabBackgroundColor
    }

    /** Kotlin property syntax, equivalent to [getTabBackgroundColor] / [setTabBackgroundColor]. */
    var tabBackgroundColor: String?
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
    open fun getTabs(): ArrayList<String> = ArrayList(tabsValue.orEmpty().asList()) as ArrayList<String>

    open fun setTabs(tabs: ArrayList<String>?) {
        if (tabs.isNullOrEmpty()) {
            return
        }
        tabsValue = tabs.take(MAX_TABS).toTypedArray<String?>()
    }

    /** Kotlin property syntax, equivalent to [getTabs] / [setTabs]. */
    var tabs: ArrayList<String>?
        @JvmSynthetic @JvmName("ktGetTabs") inline get() = getTabs()
        @JvmSynthetic @JvmName("ktSetTabs") inline set(value) = setTabs(value)

    /**
     * The color of the unselected tab.
     *
     * Setter: sets the color of the unselected tab (String - hexcode of the color).
     */
    open fun getUnselectedTabColor(): String? = unselectedTabColorValue

    open fun setUnselectedTabColor(unselectedTabColor: String?) {
        unselectedTabColorValue = unselectedTabColor
    }

    /** Kotlin property syntax, equivalent to [getUnselectedTabColor] / [setUnselectedTabColor]. */
    var unselectedTabColor: String?
        @JvmSynthetic @JvmName("ktGetUnselectedTabColor") inline get() = getUnselectedTabColor()
        @JvmSynthetic @JvmName("ktSetUnselectedTabColor") inline set(value) = setUnselectedTabColor(value)

    open fun isUsingTabs(): Boolean = !tabsValue.isNullOrEmpty()

    /** Kotlin property syntax, equivalent to [isUsingTabs]. */
    val isUsingTabs: Boolean
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

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<CTInboxStyleConfig> = object : Parcelable.Creator<CTInboxStyleConfig> {
            override fun createFromParcel(parcel: Parcel): CTInboxStyleConfig = CTInboxStyleConfig(parcel)

            override fun newArray(size: Int): Array<CTInboxStyleConfig?> = arrayOfNulls(size)
        }

        private const val MAX_TABS = 2
    }
}

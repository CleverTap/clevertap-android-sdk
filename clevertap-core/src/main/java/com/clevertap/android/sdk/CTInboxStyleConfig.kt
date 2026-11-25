package com.clevertap.android.sdk

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.inbox.CTInboxActivity
import java.util.ArrayList
import java.util.Arrays

/**
 * This class has all the parameters required to configure the styling of your [CTInboxActivity]
 * All the setter methods are public and the object of this class is made immutable by [CleverTapAPI]
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class CTInboxStyleConfig : Parcelable {

    private var _backButtonColor: String? = null

    private var _firstTabTitle: String? = null

    private var _inboxBackgroundColor: String? = null

    private var _navBarColor: String? = null

    private var _navBarTitle: String? = null

    private var _navBarTitleColor: String? = null

    private var _noMessageViewText: String? = null

    private var _noMessageViewTextColor: String? = null

    private var _selectedTabColor: String? = null

    private var _selectedTabIndicatorColor: String? = null

    private var _tabBackgroundColor: String? = null

    private var _tabs: Array<String?>? = null

    private var _unselectedTabColor: String? = null

    constructor() {
        _navBarColor = "#FFFFFF"
        _navBarTitle = "App Inbox"
        _navBarTitleColor = "#333333"
        _inboxBackgroundColor = "#D3D4DA"
        _backButtonColor = "#333333"
        _selectedTabColor = "#1C84FE"
        _unselectedTabColor = "#808080"
        _selectedTabIndicatorColor = "#1C84FE"
        _tabBackgroundColor = "#FFFFFF"
        _tabs = arrayOfNulls(0)
        _noMessageViewText = "No Message(s) to show"
        _noMessageViewTextColor = "#000000"
        _firstTabTitle = "ALL"
    }

    internal constructor(config: CTInboxStyleConfig) {
        _navBarColor = config._navBarColor
        _navBarTitle = config._navBarTitle
        _navBarTitleColor = config._navBarTitleColor
        _inboxBackgroundColor = config._inboxBackgroundColor
        _backButtonColor = config._backButtonColor
        _selectedTabColor = config._selectedTabColor
        _unselectedTabColor = config._unselectedTabColor
        _selectedTabIndicatorColor = config._selectedTabIndicatorColor
        _tabBackgroundColor = config._tabBackgroundColor
        val configTabs = config._tabs
        _tabs = if (configTabs == null) arrayOfNulls(0) else Arrays.copyOf(configTabs, configTabs.size)
        _noMessageViewText = config._noMessageViewText
        _noMessageViewTextColor = config._noMessageViewTextColor
        _firstTabTitle = config._firstTabTitle
    }

    protected constructor(`in`: Parcel) {
        _navBarColor = `in`.readString()
        _navBarTitle = `in`.readString()
        _navBarTitleColor = `in`.readString()
        _inboxBackgroundColor = `in`.readString()
        _tabs = `in`.createStringArray()
        _backButtonColor = `in`.readString()
        _selectedTabColor = `in`.readString()
        _unselectedTabColor = `in`.readString()
        _selectedTabIndicatorColor = `in`.readString()
        _tabBackgroundColor = `in`.readString()
        _noMessageViewText = `in`.readString()
        _noMessageViewTextColor = `in`.readString()
        _firstTabTitle = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * The color of the back button on the top navigation toolbar.
     *
     * Setter: sets the color of the back button on the top navigation toolbar
     * (String - hexcode of the color).
     */
    open fun getBackButtonColor(): String? = _backButtonColor

    open fun setBackButtonColor(backButtonColor: String?) {
        _backButtonColor = backButtonColor
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
    open fun getFirstTabTitle(): String? = _firstTabTitle

    open fun setFirstTabTitle(title: String?) {
        _firstTabTitle = title
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
    open fun getInboxBackgroundColor(): String? = _inboxBackgroundColor

    open fun setInboxBackgroundColor(inboxBackgroundColor: String?) {
        _inboxBackgroundColor = inboxBackgroundColor
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
    open fun getNavBarColor(): String? = _navBarColor

    open fun setNavBarColor(navBarColor: String?) {
        _navBarColor = navBarColor
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
    open fun getNavBarTitle(): String? = _navBarTitle

    open fun setNavBarTitle(navBarTitle: String?) {
        _navBarTitle = navBarTitle
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
    open fun getNavBarTitleColor(): String? = _navBarTitleColor

    open fun setNavBarTitleColor(navBarTitleColor: String?) {
        _navBarTitleColor = navBarTitleColor
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
    open fun getNoMessageViewText(): String? = _noMessageViewText

    open fun setNoMessageViewText(noMessageViewText: String?) {
        _noMessageViewText = noMessageViewText
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
    open fun getNoMessageViewTextColor(): String? = _noMessageViewTextColor

    open fun setNoMessageViewTextColor(noMessageViewTextColor: String?) {
        _noMessageViewTextColor = noMessageViewTextColor
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
    open fun getSelectedTabColor(): String? = _selectedTabColor

    open fun setSelectedTabColor(selectedTabColor: String?) {
        _selectedTabColor = selectedTabColor
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
    open fun getSelectedTabIndicatorColor(): String? = _selectedTabIndicatorColor

    open fun setSelectedTabIndicatorColor(selectedTabIndicatorColor: String?) {
        _selectedTabIndicatorColor = selectedTabIndicatorColor
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
    open fun getTabBackgroundColor(): String? = _tabBackgroundColor

    open fun setTabBackgroundColor(tabBackgroundColor: String?) {
        _tabBackgroundColor = tabBackgroundColor
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
    open fun getTabs(): ArrayList<String> {
        val current = _tabs
        @Suppress("UNCHECKED_CAST")
        return if (current == null) ArrayList() else ArrayList(Arrays.asList(*current)) as ArrayList<String>
    }

    open fun setTabs(tabs: ArrayList<String>?) {
        if (tabs == null || tabs.size <= 0) {
            return
        }

        val toAdd: ArrayList<String> = if (tabs.size > MAX_TABS) {
            ArrayList(tabs.subList(0, MAX_TABS))
        } else {
            tabs
        }
        _tabs = toAdd.toTypedArray<String?>()
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
    open fun getUnselectedTabColor(): String? = _unselectedTabColor

    open fun setUnselectedTabColor(unselectedTabColor: String?) {
        _unselectedTabColor = unselectedTabColor
    }

    /** Kotlin property syntax, equivalent to [getUnselectedTabColor] / [setUnselectedTabColor]. */
    var unselectedTabColor: String?
        @JvmSynthetic @JvmName("ktGetUnselectedTabColor") inline get() = getUnselectedTabColor()
        @JvmSynthetic @JvmName("ktSetUnselectedTabColor") inline set(value) = setUnselectedTabColor(value)

    open fun isUsingTabs(): Boolean {
        val current = _tabs
        return current != null && current.isNotEmpty()
    }

    /** Kotlin property syntax, equivalent to [isUsingTabs]. */
    val isUsingTabs: Boolean
        @JvmSynthetic @JvmName("ktIsUsingTabs") inline get() = isUsingTabs()

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(_navBarColor)
        dest.writeString(_navBarTitle)
        dest.writeString(_navBarTitleColor)
        dest.writeString(_inboxBackgroundColor)
        dest.writeStringArray(_tabs)
        dest.writeString(_backButtonColor)
        dest.writeString(_selectedTabColor)
        dest.writeString(_unselectedTabColor)
        dest.writeString(_selectedTabIndicatorColor)
        dest.writeString(_tabBackgroundColor)
        dest.writeString(_noMessageViewText)
        dest.writeString(_noMessageViewTextColor)
        dest.writeString(_firstTabTitle)
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<CTInboxStyleConfig> = object : Parcelable.Creator<CTInboxStyleConfig> {
            override fun createFromParcel(`in`: Parcel): CTInboxStyleConfig {
                return CTInboxStyleConfig(`in`)
            }

            override fun newArray(size: Int): Array<CTInboxStyleConfig?> {
                return arrayOfNulls(size)
            }
        }

        private const val MAX_TABS = 2
    }
}

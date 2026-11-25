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
    open var backButtonColor: String?
        get() = _backButtonColor
        set(backButtonColor) {
            _backButtonColor = backButtonColor
        }

    /**
     * The title of the first tab of the App Inbox.
     *
     * Setter: sets the title of the first tab of the App Inbox (String - title of the first tab).
     */
    open var firstTabTitle: String?
        get() = _firstTabTitle
        set(title) {
            _firstTabTitle = title
        }

    /**
     * The background color for the entire inbox.
     *
     * Setter: sets the background color for the entire inbox (String - hexcode of the color).
     */
    open var inboxBackgroundColor: String?
        get() = _inboxBackgroundColor
        set(inboxBackgroundColor) {
            _inboxBackgroundColor = inboxBackgroundColor
        }

    /**
     * The color for the top navigation toolbar.
     *
     * Setter: sets the color for the top navigation toolbar (String - hexcode of the color).
     */
    open var navBarColor: String?
        get() = _navBarColor
        set(navBarColor) {
            _navBarColor = navBarColor
        }

    /**
     * The text for the top navigation toolbar.
     *
     * Setter: sets the text for the top navigation toolbar (String).
     */
    open var navBarTitle: String?
        get() = _navBarTitle
        set(navBarTitle) {
            _navBarTitle = navBarTitle
        }

    /**
     * The color for the title in the top navigation toolbar.
     *
     * Setter: sets the color for the title in the top navigation toolbar (String - hexcode of the color).
     */
    open var navBarTitleColor: String?
        get() = _navBarTitleColor
        set(navBarTitleColor) {
            _navBarTitleColor = navBarTitleColor
        }

    /**
     * The text for when there are no messages in the App Inbox.
     *
     * Setter: sets the text for when there are no messages in the App Inbox (String).
     */
    open var noMessageViewText: String?
        get() = _noMessageViewText
        set(noMessageViewText) {
            _noMessageViewText = noMessageViewText
        }

    /**
     * The color of the text for when there are no messages in the App Inbox.
     *
     * Setter: sets the color of the text for when there are no messages in the App Inbox
     * (String - hexcode of the color).
     */
    open var noMessageViewTextColor: String?
        get() = _noMessageViewTextColor
        set(noMessageViewTextColor) {
            _noMessageViewTextColor = noMessageViewTextColor
        }

    /**
     * The color of the selected tab.
     *
     * Setter: sets the color of the selected tab (String - hexcode of the color).
     */
    open var selectedTabColor: String?
        get() = _selectedTabColor
        set(selectedTabColor) {
            _selectedTabColor = selectedTabColor
        }

    /**
     * The color of the indicator of the selected tab.
     *
     * Setter: sets the color of the indicator of the selected tab (String - hexcode of the color).
     */
    open var selectedTabIndicatorColor: String?
        get() = _selectedTabIndicatorColor
        set(selectedTabIndicatorColor) {
            _selectedTabIndicatorColor = selectedTabIndicatorColor
        }

    /**
     * The background color for the tabs.
     *
     * Setter: sets the background color for the tabs (String - hexcode of the color).
     */
    open var tabBackgroundColor: String?
        get() = _tabBackgroundColor
        set(tabBackgroundColor) {
            _tabBackgroundColor = tabBackgroundColor
        }

    /**
     * The names of the optional two tabs. Reading returns a copy (never `null`).
     *
     * Setter: sets the name of the optional two tabs.
     * The contents of the tabs are filtered based on the name of the tab.
     * A `null` or empty list is ignored; only the first two entries are kept.
     */
    open var tabs: ArrayList<String>?
        get() {
            val current = _tabs
            @Suppress("UNCHECKED_CAST")
            return if (current == null) ArrayList() else ArrayList(Arrays.asList(*current)) as ArrayList<String>
        }
        set(tabs) {
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

    /**
     * The color of the unselected tab.
     *
     * Setter: sets the color of the unselected tab (String - hexcode of the color).
     */
    open var unselectedTabColor: String?
        get() = _unselectedTabColor
        set(unselectedTabColor) {
            _unselectedTabColor = unselectedTabColor
        }

    open val isUsingTabs: Boolean
        get() {
            val current = _tabs
            return current != null && current.isNotEmpty()
        }

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

package com.clevertap.demo.ui.main

import com.clevertap.demo.BuildConfig

object HomeScreenModel {

    const val LOCAL_PUSH_SECTION = "LOCAL PUSH (JSON)"

    val listData: Map<String, List<String>> by lazy {
        mapOf(
            "EVENTS" to listOf(
                "Simple Event (App Opened)",
                "Product Viewed Event",
                "Charged Event (Transaction)",
                "Screen View Event",
                "Custom Event",
                "Nested Object Event"
            ),
            "USER PROFILE" to listOf(
                "Push Basic Profile",
                "Push Complete User Profile",
                "Update User Preferences",
                "Set Profile Location",
                "Get All User Profile Properties",
                "User Login",
                "Push Nested Object Profile"
            ),
            "PROFILE OPERATIONS" to listOf(
                "Remove Single Property",
                "Set Multi-Value Property",
                "Add to Multi-Value Property",
                "Remove from Multi-Value Property",
                "Increment Loyalty Points",
                "Decrement Cart Count",
                "Increment Nested Property (dot notation)",
                "Decrement Nested Property (dot notation)",
                "Get Specific Nested Property (dot notation)"
            ),
            "INBOX" to listOf(
                "Open Inbox(with tabs)",
                "Open Inbox(without tabs)",
                "Show Total Counts",
                "Show Unread Counts",
                "Get All Inbox Messages",
                "Get Unread Messages",
                "Get InboxMessage by messageID",
                "Delete InboxMessage by messageID",
                "Delete InboxMessage by Object",
                "Delete multiple InboxMessages by list of messageIDs",
                "Mark as read by messageID",
                "Mark as read by Object",
                "Mark multiple InboxMessages as read by list of messageIDs",
                "Notification Viewed event for Message",
                "Notification Clicked event for Message",
                "Custom KV Root level",
                "Open Compose Inbox Screen"
            ),
            "DISPLAY UNITS" to listOf(
                "Get Display Unit For Id", "Get All Display Units",
                "Notification Viewed event for Display Unit", "Notification Clicked event for Display Unit",
                "Log per-slide attribution for all Display Units",
                "Element Viewed event for every slide",
                "Element Clicked event for first slide"
            ),
            "PRODUCT CONFIGS" to listOf(
                "Set Default Product Configs",
                "Fetch",
                "Activate",
                "Fetch And Activate",
                "Reset",
                "Fetch With Minimum Fetch Interval In Seconds",
                "Get Product Configs",
                "Response lastFetchTimeStampInMillis"
            ),
            "FEATURE FLAGS" to listOf("Get Feature Flag"),
            "WEBVIEW" to listOf("Raise events from WebView"),
            "GEOFENCE" to listOf("Init Geofence", "Trigger Location", "Deactivate Geofence"),
            "DEVICE IDENTIFIERS" to listOf("Fetch CleverTapAttribution Identifier", "Fetch CleverTap ID"),
            "PUSH TEMPLATES" to listOf(
                "Basic Push",
                "Carousel Push",
                "Manual Carousel Push",
                "FilmStrip Carousel Push",
                "Rating Push",
                "Product Display",
                "Linear Product Display",
                "Five CTA",
                "Zero Bezel",
                "Zero Bezel Text Only",
                "Timer Push",
                "Input Box - CTA + reminder Push Campaign - DOC true",
                "Input Box - Reply with Event",
                "Input Box - Reply with Intent",
                "Input Box - CTA + reminder Push Campaign - DOC false",
                "Input Box - CTA - DOC true",
                "Input Box - CTA - DOC false",
                "Input Box - reminder - DOC true",
                "Input Box - reminder - DOC false",
                "Three CTA"
            ),
            "PROMPT LOCAL IAM" to listOf(
                "Half-Interstitial Local IAM",
                "Half-Interstitial Local IAM with image URL",
                "Half-Interstitial Local IAM with fallbackToSettings - true",
                "Alert Local IAM",
                "Alert Local IAM with followDeviceOrientation - false",
                "Alert Local IAM with fallbackToSettings - true",
                "Hard permission dialog with fallbackToSettings - false",
                "Hard permission dialog with fallbackToSettings - true"
            ),
            "INAPP Control" to listOf("Suspend", "Discard", "Resume", "Discard and Dismiss"),
            "CS INAPP" to listOf("Fetch CS InApps", "Clear all CS InApp Resources", "Clear expired only InAPP Resources"),
            "VARIABLES" to listOf(
                "Define Variable",
                "Define file Variables with listeners",
                "Fetch Variables",
                "Sync Variables",
                "Parse Variables",
                "Get Variable",
                "Get Variable Value",
                "Add Variables Changed Callback",
                "Remove Variables Changed Callback",
                "Add One Time Variables Changed Callback",
                "Remove One Time Variables Changed Callback",
                "Define Multiple vars, fetch and print",
                "Get AB Variants and print"
            ),
            "FILE TYPE VARIABLES" to listOf(
                "Define file Variables listeners \n adds file variables with fileReady() listeners",
                "Define file Variables with multiple listeners \n adds file variables with fileReady() listeners",
                "Global listeners & Define file Variables \n Adds listeners first and then registers the variables",
                "Multiple Global listeners & Define file Variables \n Adds listeners first and then registers the variables",
                "PrintFileVariables",
                "Clear All File Resources"
                //"Add onceVariablesChangedAndNoDownloadsPending \n first time after app launch for first time",
            ),
            "LOCALE" to listOf("Set Locale"),
            "CUSTOM TEMPLATES" to listOf("Sync Registered Custom Templates", "Test Custom Template Dialog"),
            "OPT OUT" to listOf(
                "Opt Out - userOptOut: true, allowSystemEvents: true",
                "Opt Out - userOptOut: true, allowSystemEvents: false",
                "Opt Out - userOptOut: false, allowSystemEvents: true",
                "Opt Out - userOptOut: false, allowSystemEvents: false",
                "Opt Out - userOptOut: true (single param)",
                "Opt Out - userOptOut: false (single param)"
            ),
            // Live Update local demos (SDK-rendered pt_progress + the Mode A custom factory). Section
            // index 19 -> click codes "19-N" are handled in HomeScreenFragment (they render locally,
            // needing a Context).
            "LIVE UPDATES" to listOf(
                "Progress: Order Tracker (local demo)",
                "Progress: Actions + Deep Link (local demo)",
                "Progress: Countdown Chip + Promoted (local demo)",
                "Progress: No Promotion (local demo)",
                "Progress: Start/End Icons (local demo)",
                "Progress: Plain Bar - determinate (local demo)",
                "Progress: Plain Bar + Start/End Icons (local demo)",
                "Progress: Indeterminate Bar (local demo)",
                "Progress: Indeterminate Bar + Start/End Icons (local demo)",
                "Progress: Unequal Segments 10/80/10 (local demo)",
                "Progress: On Track - tracker + styled by progress (local demo)",
                "Progress: On Track - no tracker, fade only (local demo)",
                "Progress: Light/Dark Colors - default, white, black (local demo)",
                "Progress: Edge Points 0/100 - ends hidden (local demo)",
                "Progress: Edge Points 5/95 - control, all 4 dots (local demo)",
                "Custom Live Update - Mode A factory (local demo)",
                "Paste Live Update payload (QA)"
            ),
        ) + localPushSection()
    }

    /**
     * Command string for the local push tester, or null when the section is absent. Derived from the
     * section's position so it cannot drift out of step with [listData].
     */
    val localPushCommand: String? by lazy {
        listData.keys.indexOf(LOCAL_PUSH_SECTION).takeIf { it >= 0 }?.let { "$it-0" }
    }

    /**
     * Debug-only section: LocalPushActivity lives in the debug source set, so there is nothing to
     * open in a release build. Appended last, which keeps every other section's index unchanged.
     */
    private fun localPushSection(): Map<String, List<String>> =
        if (BuildConfig.DEBUG) mapOf(LOCAL_PUSH_SECTION to listOf("Open Local Push Tester")) else emptyMap()
}
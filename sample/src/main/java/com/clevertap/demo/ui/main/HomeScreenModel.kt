package com.clevertap.demo.ui.main

object HomeScreenModel {

    val listData: Map<String, List<String>> by lazy {
        mapOf(
            "EVENTS" to listOf(
                "Record Event",
                "Record event with properties",
                "Record Charged Event",
                "Record Screen Event"
            ),
            "USER PROFILE" to listOf(
                "Push profile", "Update(Replace) Single-Value properties",
                "Update(Add) Single-Value properties", "Update(Remove) Single-Value properties",
                "Update(Replace) Multi-Value property", "Update(Add) Multi-Value property",
                "Update(Remove) Multi-Value property", "Update(Add) Increment Value",
                "Update(Add) Decrement Value", "Profile Location", "Get User Profile Property",
                "onUserLogin"
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
                "Notification Viewed event for Display Unit", "Notification Clicked event for Display Unit"
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
            "INAPP" to listOf("Suspend", "Discard", "Resume"),
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
                "Remove One Time Variables Changed Callback"
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
        )
    }
}
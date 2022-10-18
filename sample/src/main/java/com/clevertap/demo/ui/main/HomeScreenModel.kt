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
                "Open Inbox(with tabs)","Open Inbox(without tabs)","Show Total Counts", "Show Unread Counts", "Get All Inbox Messages",
                "Get Unread Messages", "Get InboxMessage by messageID", "Delete InboxMessage by messageID",
                "Delete InboxMessage by Object", "Mark as read by messageID", "Mark as read by Object",
                "Notification Viewed event for Message", "Notification Clicked event for Message"
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
            )
        )
    }
}
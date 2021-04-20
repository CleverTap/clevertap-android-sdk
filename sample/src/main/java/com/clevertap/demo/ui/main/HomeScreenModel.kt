package com.clevertap.demo.ui.main

object HomeScreenModel {

    val listData: Map<String, List<String>> by lazy {
        mapOf(
                "EVENTS" to listOf("Record Event", "Record event with properties", "Record Charged Event"),
                "USER PROFILE" to listOf(
                        "Push profile", "Update(Replace) Single-Value properties",
                        "Update(Add) Single-Value properties", "Update(Remove) Single-Value properties",
                        "Update(Replace) Multi-Value property", "Update(Add) Multi-Value property",
                        "Update(Remove) Multi-Value property", "Profile Location", "Get User Profile Property",
                        "onUserLogin"
                ),
                "INBOX" to listOf(
                        "Open Inbox", "Show Total Counts", "Show Unread Counts", "Get All Inbox Messages",
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
                "GEOFENCE" to listOf("Init Geofence", "Trigger Location", "Deactivate Geofence")
        )
    }
}
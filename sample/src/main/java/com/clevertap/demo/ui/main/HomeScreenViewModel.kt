package com.clevertap.demo.ui.main

import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.clevertap.android.sdk.CTInboxStyleConfig
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.CTLocalInApp
import com.clevertap.android.sdk.inapp.callbacks.FetchInAppsCallback
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback
import com.clevertap.demo.ExampleVariables
import com.clevertap.demo.MyApplication
import org.json.JSONArray
import java.util.Date

private const val TAG = "HomeScreenViewModel"

/**
 * ViewModel for Home Screen managing CleverTap SDK interactions
 */
class HomeScreenViewModel(
    private val cleverTapAPI: CleverTapAPI?,
    private val ctMultiInstance: CleverTapAPI? = MyApplication.ctMultiInstance
) : ViewModel() {

    val clickCommand: MutableLiveData<String> by lazy { MutableLiveData<String>() }

    private val exampleVariables by lazy { ExampleVariables() }

    /**
     * Handles child item clicks from the expandable list
     * @param groupPosition Section index from HomeScreenModel
     * @param childPosition Item index within the section
     */
    fun onChildClick(groupPosition: Int = 0, childPosition: Int = 0) {
        val commandPosition = "$groupPosition-$childPosition"
        log("Child clicked: Section=$groupPosition, Item=$childPosition, Command=$commandPosition")

        clickCommand.value = commandPosition

        executeCommand(groupPosition, childPosition)
    }

    private fun executeCommand(section: Int, item: Int) {
        when (section) {
            0 -> handleEventsSection(item)
            1 -> handleUserProfileSection(item)
            2 -> handleProfileOperationsSection(item)
            3 -> handleInboxSection(item)
            4 -> handleDisplayUnitsSection(item)
            5 -> handleProductConfigsSection(item)
            6 -> handleFeatureFlagsSection(item)
            9 -> handleDeviceIdentifiersSection(item)
            10 -> handlePushTemplatesSection(item)
            11 -> handlePromptLocalIAMSection(item)
            12 -> handleInAppSection(item)
            13 -> handleCSInAppSection(item)
            14 -> handleVariablesSection(item)
            15 -> handleFileTypeVariablesSection(item)
            16 -> handleLocaleSection(item)
            17 -> handleCustomTemplatesSection(item)
            18 -> handleOptOutSection(item)
        }
    }

    // ========== EVENTS SECTION ==========
    private fun handleEventsSection(item: Int) {
        when (item) {
            0 -> recordSimpleEvent()
            1 -> recordProductViewedEvent()
            2 -> recordAddToCartEvent()
            3 -> recordVideoWatchedEvent()
            4 -> recordSearchEvent()
            5 -> recordChargedEvent()
            6 -> recordScreenEvent()
            7 -> recordAppRatingEvent()
            8 -> recordShareEvent()
        }
    }

    private fun recordSimpleEvent() {
        logStep("EVENTS", "Recording simple event without properties")
        
        val eventName = "App Opened"
        printVar("Event Name", eventName)
        
        cleverTapAPI?.pushEvent(eventName)
    }

    private fun recordProductViewedEvent() {
        logStep("EVENTS", "Recording Product Viewed event")

        val eventProperties = mapOf(
            "Product ID" to "PROD-12345",
            "Product Name" to "Wireless Headphones",
            "Category" to "Electronics",
            "Price" to 149.99,
            "Currency" to "USD",
            "Brand" to "AudioTech",
            "In Stock" to true,
            "Discount Percentage" to 15,
            "Image URL" to "https://example.com/products/headphones.jpg"
        )

        printMap("Event Properties", eventProperties)
        cleverTapAPI?.pushEvent("Product Viewed", eventProperties)
    }

    private fun recordAddToCartEvent() {
        logStep("EVENTS", "Recording Add to Cart event")

        val eventProperties = mapOf(
            "Product ID" to "PROD-67890",
            "Product Name" to "Smart Watch",
            "Quantity" to 1,
            "Price" to 299.99,
            "Total Amount" to 299.99,
            "Cart Total" to 449.98,
            "Items in Cart" to 2
        )

        printMap("Event Properties", eventProperties)
        cleverTapAPI?.pushEvent("Add to Cart", eventProperties)
    }

    private fun recordVideoWatchedEvent() {
        logStep("EVENTS", "Recording Video Watched event")

        val eventProperties = mapOf(
            "Video ID" to "VID-001",
            "Video Title" to "Getting Started Tutorial",
            "Duration" to 180,
            "Watched Duration" to 150,
            "Completion Percentage" to 83,
            "Video Category" to "Tutorial",
            "Quality" to "1080p",
            "Platform" to "Android"
        )

        printMap("Event Properties", eventProperties)
        cleverTapAPI?.pushEvent("Video Watched", eventProperties)
    }

    private fun recordSearchEvent() {
        logStep("EVENTS", "Recording Search event")

        val eventProperties = mapOf(
            "Search Query" to "wireless headphones",
            "Results Count" to 24,
            "Filters Applied" to arrayListOf("Price: Under $200", "Rating: 4+ stars"),
            "Search Category" to "Electronics",
            "Time Spent" to 45
        )

        printMap("Event Properties", eventProperties)
        cleverTapAPI?.pushEvent("Search", eventProperties)
    }

    private fun recordAppRatingEvent() {
        logStep("EVENTS", "Recording App Rating event")

        val eventProperties = mapOf(
            "Rating" to 5,
            "Review Text" to "Great app! Easy to use.",
            "Platform" to "Google Play",
            "App Version" to "2.4.1"
        )

        printMap("Event Properties", eventProperties)
        cleverTapAPI?.pushEvent("App Rated", eventProperties)
    }

    private fun recordShareEvent() {
        logStep("EVENTS", "Recording Share event")

        val eventProperties = mapOf(
            "Content Type" to "Product",
            "Content ID" to "PROD-12345",
            "Share Method" to "WhatsApp",
            "Platform" to "Android"
        )

        printMap("Event Properties", eventProperties)
        cleverTapAPI?.pushEvent("Content Shared", eventProperties)
    }

    private fun recordChargedEvent() {
        logStep("EVENTS", "Recording Charged event (E-commerce transaction)")

        val chargeDetails = hashMapOf<String, Any>(
            "Amount" to 449.98,
            "Currency" to "USD",
            "Payment Mode" to "Credit Card",
            "Transaction ID" to "TXN-${System.currentTimeMillis()}",
            "Shipping Charges" to 9.99,
            "Discount" to 50.00,
            "Coupon Code" to "SAVE50",
            "Order Status" to "Confirmed"
        )

        val items = arrayListOf<HashMap<String, Any>>(
            hashMapOf(
                "Product ID" to "PROD-12345",
                "Product Name" to "Wireless Headphones",
                "Category" to "Electronics",
                "Price" to 149.99,
                "Quantity" to 1,
                "Brand" to "AudioTech"
            ),
            hashMapOf(
                "Product ID" to "PROD-67890",
                "Product Name" to "Smart Watch",
                "Category" to "Wearables",
                "Price" to 299.99,
                "Quantity" to 1,
                "Brand" to "TechGear"
            ),
            hashMapOf(
                "Product ID" to "PROD-11111",
                "Product Name" to "Phone Case",
                "Category" to "Accessories",
                "Price" to 19.99,
                "Quantity" to 2,
                "Brand" to "ProtectPro"
            )
        )

        printMap("Charge Details", chargeDetails)
        printList("Items", items)

        cleverTapAPI?.pushChargedEvent(chargeDetails, items)
    }

    private fun recordScreenEvent() {
        logStep("EVENTS", "Recording screen view event")
        
        val screenName = "Product Details Screen"
        printVar("Screen Name", screenName)
        
        cleverTapAPI?.recordScreen(screenName)
    }

    // ========== USER PROFILE SECTION ==========
    private fun handleUserProfileSection(item: Int) {
        when (item) {
            0 -> pushBasicProfile()
            1 -> pushCompleteUserProfile()
            2 -> pushEcommerceProfile()
            3 -> updateUserPreferences()
            4 -> updateSubscriptionInfo()
            5 -> setProfileLocation()
            6 -> getUserProfileProperties()
            7 -> performUserLogin()
        }
    }

    private fun pushBasicProfile() {
        logStep("USER PROFILE", "Pushing basic user profile information")

        val profileUpdate = mapOf(
            "Name" to "Sarah Johnson",
            "Email" to "sarah.johnson@example.com",
            "Phone" to "+14155551234",
            "Gender" to "F",
            "DOB" to Date()
        )

        printMap("Profile Update", profileUpdate)
        cleverTapAPI?.pushProfile(profileUpdate)
    }

    private fun pushCompleteUserProfile() {
        logStep("USER PROFILE", "Pushing complete user profile with custom properties")

        val profileUpdate = buildMap {
            // Standard properties
            put("Name", "Alex Chen")
            put("Email", "alex.chen@example.com")
            put("Phone", "+16505551234")
            put("Gender", "M")
            put("DOB", Date())
            
            // Custom properties
            put("Customer Type", "Premium")
            put("Account Created", Date())
            put("City", "San Francisco")
            put("Country", "USA")
            put("Language", "English")
            put("Timezone", "America/Los_Angeles")
            
            // Boolean flags
            put("Email Verified", true)
            put("Phone Verified", true)
            put("MSG-email", true)
            put("MSG-push", true)
            put("MSG-sms", false)
        }

        printMap("Profile Update", profileUpdate)
        cleverTapAPI?.pushProfile(profileUpdate)
    }

    private fun pushEcommerceProfile() {
        logStep("USER PROFILE", "Pushing e-commerce specific profile")

        val profileUpdate = mapOf(
            "Name" to "Emily Davis",
            "Email" to "emily.davis@example.com",
            "Loyalty Points" to 2500,
            "Customer Tier" to "Gold",
            "Total Orders" to 42,
            "Total Spend" to 3450.75,
            "Favorite Categories" to arrayListOf("Electronics", "Fashion", "Home Decor"),
            "Preferred Payment" to "Credit Card",
            "Last Purchase Date" to Date(),
            "Newsletter Subscribed" to true
        )

        printMap("Profile Update", profileUpdate)
        cleverTapAPI?.pushProfile(profileUpdate)
    }

    private fun updateUserPreferences() {
        logStep("USER PROFILE", "Updating user preferences")

        val profileUpdate = mapOf(
            "Dark Mode Enabled" to true,
            "Notification Frequency" to "Daily",
            "Preferred Language" to "English",
            "Currency" to "USD",
            "Content Interests" to arrayListOf("Technology", "Business", "Sports")
        )

        printMap("Profile Update", profileUpdate)
        cleverTapAPI?.pushProfile(profileUpdate)
    }

    private fun updateSubscriptionInfo() {
        logStep("USER PROFILE", "Updating subscription information")

        val profileUpdate = mapOf(
            "Subscription Plan" to "Premium Annual",
            "Subscription Status" to "Active",
            "Subscription Start" to Date(),
            "Auto Renew" to true,
            "Features Access" to arrayListOf("Ad-Free", "Unlimited Downloads", "Priority Support")
        )

        printMap("Profile Update", profileUpdate)
        cleverTapAPI?.pushProfile(profileUpdate)
    }

    private fun removeSingleValueProperty() {
        logStep("PROFILE OPERATIONS", "Removing a single profile property")
        
        val keyToRemove = "Temporary Flag"
        printVar("Removing Key", keyToRemove)
        
        cleverTapAPI?.removeValueForKey(keyToRemove)
    }

    private fun setMultiValueProperty() {
        logStep("PROFILE OPERATIONS", "Setting multi-value property (replaces existing)")

        val favoriteCategories = arrayListOf("Electronics", "Fashion", "Home & Garden")
        printVar("Key", "Favorite Categories")
        printList("Values", favoriteCategories)

        cleverTapAPI?.setMultiValuesForKey("Favorite Categories", favoriteCategories)
    }

    private fun addToMultiValueProperty() {
        logStep("PROFILE OPERATIONS", "Adding values to multi-value property")

        val newInterests = arrayListOf("Technology", "Travel")
        printVar("Key", "Interests")
        printList("Adding Values", newInterests)

        cleverTapAPI?.addMultiValuesForKey("Interests", newInterests)
    }

    private fun removeFromMultiValueProperty() {
        logStep("PROFILE OPERATIONS", "Removing values from multi-value property")

        val removeInterests = arrayListOf("Sports")
        printVar("Key", "Interests")
        printList("Removing Values", removeInterests)

        cleverTapAPI?.removeMultiValuesForKey("Interests", removeInterests)
    }

    private fun incrementLoyaltyPoints() {
        logStep("PROFILE OPERATIONS", "Incrementing loyalty points")
        
        val pointsToAdd = 100
        printVar("Key", "Loyalty Points")
        printVar("Increment By", pointsToAdd)
        
        cleverTapAPI?.incrementValue("Loyalty Points", pointsToAdd)
    }

    private fun decrementCartCount() {
        logStep("PROFILE OPERATIONS", "Decrementing cart count")
        
        val decrementBy = 1
        printVar("Key", "Cart Items")
        printVar("Decrement By", decrementBy)
        
        cleverTapAPI?.decrementValue("Cart Items", decrementBy)
    }

    private fun setProfileLocation() {
        logStep("USER PROFILE", "Setting profile location")
        cleverTapAPI?.location = cleverTapAPI?.location
        printVar("Location", cleverTapAPI?.location.toString())
    }

    private fun getUserProfileProperties() {
        logStep("USER PROFILE", "Getting user profile properties")
        logAllProfileProperties()
        printVar("CleverTapId", cleverTapAPI?.cleverTapID.orEmpty())
        printVar("CleverTap AttributionIdentifier", cleverTapAPI?.cleverTapAttributionIdentifier.orEmpty())
    }

    private fun performUserLogin() {
        logStep("USER PROFILE", "Performing user login")
        onUserLogin(cleverTapAPI)
        onUserLogin(ctMultiInstance)
    }

    // ========== PROFILE OPERATIONS SECTION ==========
    private fun handleProfileOperationsSection(item: Int) {
        when (item) {
            0 -> removeSingleValueProperty()
            1 -> setMultiValueProperty()
            2 -> addToMultiValueProperty()
            3 -> removeFromMultiValueProperty()
            4 -> incrementLoyaltyPoints()
            5 -> decrementCartCount()
        }
    }

    // ========== INBOX SECTION ==========
    private fun handleInboxSection(item: Int) {
        when (item) {
            0 -> openInboxWithTabs()
            1 -> openInboxWithoutTabs()
            2 -> showTotalInboxCount()
            3 -> showUnreadInboxCount()
            4 -> getAllInboxMessages()
            5 -> getUnreadInboxMessages()
            6 -> getInboxMessageById()
            7 -> deleteInboxMessageById()
            8 -> deleteInboxMessageByObject()
            9 -> deleteMultipleInboxMessages()
            10 -> markMessageAsReadById()
            11 -> markMessageAsReadByObject()
            12 -> markMultipleMessagesAsRead()
            13 -> raiseNotificationViewedEvent()
            14 -> raiseNotificationClickedEvent()
            15 -> getCustomKVData()
        }
    }

    private fun openInboxWithTabs() {
        logStep("INBOX", "Opening inbox with tabs")

        val inboxTabs = arrayListOf("Promotions", "Offers", "Others")
        printList("Inbox Tabs", inboxTabs)

        CTInboxStyleConfig().apply {
            tabs = inboxTabs
            tabBackgroundColor = "#FF0000"
            selectedTabIndicatorColor = "#0000FF"
            selectedTabColor = "#000000"
            unselectedTabColor = "#FFFFFF"
            backButtonColor = "#FF0000"
            navBarTitleColor = "#FF0000"
            navBarTitle = "MY INBOX"
            navBarColor = "#FFFFFF"
            inboxBackgroundColor = "#00FF00"
            firstTabTitle = "First Tab"
            cleverTapAPI?.showAppInbox(this)
        }
    }

    private fun openInboxWithoutTabs() {
        logStep("INBOX", "Opening inbox without tabs")

        CTInboxStyleConfig().apply {
            tabBackgroundColor = "#FF0000"
            selectedTabIndicatorColor = "#0000FF"
            selectedTabColor = "#000000"
            unselectedTabColor = "#FFFFFF"
            backButtonColor = "#FF0000"
            navBarTitleColor = "#FF0000"
            navBarTitle = "MY INBOX"
            navBarColor = "#FFFFFF"
            inboxBackgroundColor = "#00FF00"
            cleverTapAPI?.showAppInbox(this)
        }
    }

    private fun showTotalInboxCount() {
        logStep("INBOX", "Showing total inbox message count")
        printVar("Total Message Count", cleverTapAPI?.inboxMessageCount ?: 0)
    }

    private fun showUnreadInboxCount() {
        logStep("INBOX", "Showing unread inbox message count")
        printVar("Unread Message Count", cleverTapAPI?.inboxMessageUnreadCount ?: 0)
    }

    private fun getAllInboxMessages() {
        logStep("INBOX", "Getting all inbox messages")
        cleverTapAPI?.allInboxMessages?.forEach {
            printVar("Inbox Message ID", it.messageId)
        }
    }

    private fun getUnreadInboxMessages() {
        logStep("INBOX", "Getting all unread inbox messages")
        cleverTapAPI?.unreadInboxMessages?.forEach {
            printVar("Unread Message ID", it.messageId)
        }
    }

    private fun getInboxMessageById() {
        logStep("INBOX", "Getting inbox message by ID")

        val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId

        firstMessageId?.also { id ->
            val message = cleverTapAPI?.getInboxMessageForId(id)
            printVar("Message ID", id)
            printVar("Message Data", message?.data.toString())
        } ?: log("No inbox messages found")
    }

    private fun deleteInboxMessageById() {
        logStep("INBOX", "Deleting inbox message by ID")

        cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId?.also { id ->
            cleverTapAPI?.deleteInboxMessage(id)
            printVar("Deleted Message ID", id)
        } ?: log("No inbox messages found")
    }

    private fun deleteInboxMessageByObject() {
        logStep("INBOX", "Deleting inbox message by object")

        cleverTapAPI?.allInboxMessages?.firstOrNull()?.also { message ->
            cleverTapAPI?.deleteInboxMessage(message)
            printVar("Deleted Message ID", message.messageId)
        } ?: log("No inbox messages found")
    }

    private fun deleteMultipleInboxMessages() {
        logStep("INBOX", "Deleting multiple inbox messages")

        val messageIDs = cleverTapAPI?.unreadInboxMessages?.map { it.messageId } ?: emptyList()
        printList("Message IDs to Delete", messageIDs)

        cleverTapAPI?.deleteInboxMessagesForIDs(ArrayList(messageIDs))
    }

    private fun markMessageAsReadById() {
        logStep("INBOX", "Marking message as read by ID")

        cleverTapAPI?.unreadInboxMessages?.firstOrNull()?.messageId?.also { id ->
            cleverTapAPI?.markReadInboxMessage(id)
            printVar("Marked Read - Message ID", id)
        } ?: log("No unread messages found")
    }

    private fun markMessageAsReadByObject() {
        logStep("INBOX", "Marking message as read by object")

        cleverTapAPI?.unreadInboxMessages?.firstOrNull()?.also { message ->
            cleverTapAPI?.markReadInboxMessage(message)
            printVar("Marked Read - Message ID", message.messageId)
        } ?: log("No unread messages found")
    }

    private fun markMultipleMessagesAsRead() {
        logStep("INBOX", "Marking multiple messages as read")

        val messageIDs = cleverTapAPI?.unreadInboxMessages?.map { it.messageId } ?: emptyList()
        printList("Message IDs to Mark Read", messageIDs)

        cleverTapAPI?.markReadInboxMessagesForIDs(ArrayList(messageIDs))
    }

    private fun raiseNotificationViewedEvent() {
        logStep("INBOX", "Raising notification viewed event")

        cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId?.also { id ->
            cleverTapAPI?.pushInboxNotificationViewedEvent(id)
            printVar("Viewed Event - Message ID", id)
        } ?: log("No inbox messages found")
    }

    private fun raiseNotificationClickedEvent() {
        logStep("INBOX", "Raising notification clicked event")

        cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId?.also { id ->
            cleverTapAPI?.pushInboxNotificationClickedEvent(id)
            printVar("Clicked Event - Message ID", id)
        } ?: log("No inbox messages found")
    }

    private fun getCustomKVData() {
        logStep("INBOX", "Getting custom KV data")

        val customData = cleverTapAPI?.allInboxMessages?.firstOrNull()?.customData
        printVar("Custom Data", customData.toString())
    }

    // ========== DISPLAY UNITS SECTION ==========
    private fun handleDisplayUnitsSection(item: Int) {
        when (item) {
            0 -> getDisplayUnitById()
            1 -> getAllDisplayUnits()
            2 -> raiseDisplayUnitViewedEvent()
            3 -> raiseDisplayUnitClickedEvent()
        }
    }

    private fun getDisplayUnitById() {
        logStep("DISPLAY UNITS", "Getting display unit by ID")

        cleverTapAPI?.allDisplayUnits?.firstOrNull()?.unitID?.also { id ->
            val displayUnit = cleverTapAPI?.getDisplayUnitForId(id)
            printVar("Display Unit ID", id)
            printVar("Display Unit", displayUnit.toString())
        } ?: log("No display units found")
    }

    private fun getAllDisplayUnits() {
        logStep("DISPLAY UNITS", "Getting all display units")
        printVar("All Display Units", cleverTapAPI?.allDisplayUnits.toString())
    }

    private fun raiseDisplayUnitViewedEvent() {
        logStep("DISPLAY UNITS", "Raising display unit viewed event")

        cleverTapAPI?.allDisplayUnits?.firstOrNull()?.unitID?.also { id ->
            cleverTapAPI?.pushDisplayUnitViewedEventForID(id)
            printVar("Viewed Event - Display Unit ID", id)
        } ?: log("No display units found")
    }

    private fun raiseDisplayUnitClickedEvent() {
        logStep("DISPLAY UNITS", "Raising display unit clicked event")

        cleverTapAPI?.allDisplayUnits?.firstOrNull()?.unitID?.also { id ->
            cleverTapAPI?.pushDisplayUnitClickedEventForID(id)
            printVar("Clicked Event - Display Unit ID", id)
        } ?: log("No display units found")
    }

    // ========== PRODUCT CONFIGS SECTION ==========
    private fun handleProductConfigsSection(item: Int) {
        when (item) {
            0 -> setDefaultProductConfigs()
            1 -> fetchProductConfigs()
            2 -> activateProductConfigs()
            3 -> fetchAndActivateProductConfigs()
            4 -> resetProductConfigs()
            5 -> fetchWithMinimumInterval()
            6 -> getProductConfigValues()
            7 -> getLastFetchTimestamp()
        }
    }

    private fun setDefaultProductConfigs() {
        logStep("PRODUCT CONFIGS", "Setting default product configs")

        val defaults = hashMapOf<String, Any>(
            "text color" to "red",
            "msg count" to 100,
            "price" to 100.50,
            "is shown" to true,
            "json" to """{"key":"val","key2":50}"""
        )

        printMap("Default Configs", defaults)
        cleverTapAPI?.productConfig()?.setDefaults(defaults)
    }

    private fun fetchProductConfigs() {
        logStep("PRODUCT CONFIGS", "Fetching product configs")
        cleverTapAPI?.productConfig()?.fetch()
    }

    private fun activateProductConfigs() {
        logStep("PRODUCT CONFIGS", "Activating product configs")
        cleverTapAPI?.productConfig()?.activate()
    }

    private fun fetchAndActivateProductConfigs() {
        logStep("PRODUCT CONFIGS", "Fetching and activating product configs")
        cleverTapAPI?.productConfig()?.fetchAndActivate()
    }

    private fun resetProductConfigs() {
        logStep("PRODUCT CONFIGS", "Resetting product configs")
        cleverTapAPI?.productConfig()?.reset()
    }

    private fun fetchWithMinimumInterval() {
        logStep("PRODUCT CONFIGS", "Fetching with minimum interval (60 seconds)")
        printVar("Minimum Fetch Interval", "60 seconds")
        cleverTapAPI?.productConfig()?.fetch(60)
    }

    private fun getProductConfigValues() {
        logStep("PRODUCT CONFIGS", "Getting all product config values")

        cleverTapAPI?.productConfig()?.apply {
            printVar("text color (String)", getString("text color"))
            printVar("is shown (Boolean)", getBoolean("is shown"))
            printVar("msg count (Long)", getLong("msg count"))
            printVar("price (Double)", getDouble("price"))
            printVar("json (String)", getString("json"))
        }
    }

    private fun getLastFetchTimestamp() {
        logStep("PRODUCT CONFIGS", "Getting last fetch timestamp")

        val timestamp = cleverTapAPI?.productConfig()?.lastFetchTimeStampInMillis ?: 0
        printVar("Last Fetch Timestamp (ms)", timestamp)
    }

    // ========== FEATURE FLAGS SECTION ==========
    private fun handleFeatureFlagsSection(item: Int) {
        when (item) {
            0 -> getFeatureFlag()
        }
    }

    private fun getFeatureFlag() {
        logStep("FEATURE FLAGS", "Getting feature flag value")

        val isShown = cleverTapAPI?.featureFlag()?.get("is shown", true) ?: true
        printVar("is shown (Boolean)", isShown)
    }

    // ========== DEVICE IDENTIFIERS SECTION ==========
    private fun handleDeviceIdentifiersSection(item: Int) {
        when (item) {
            0 -> fetchAttributionIdentifier()
            1 -> fetchCleverTapID()
        }
    }

    private fun fetchAttributionIdentifier() {
        logStep("DEVICE IDENTIFIERS", "Fetching CleverTap Attribution Identifier")
        printVar("Attribution Identifier", cleverTapAPI?.cleverTapAttributionIdentifier.orEmpty())
    }

    private fun fetchCleverTapID() {
        logStep("DEVICE IDENTIFIERS", "Fetching CleverTap ID")

        cleverTapAPI?.getCleverTapID { deviceId ->
            val threadType = if (Looper.myLooper() == Looper.getMainLooper()) "Main Thread" else "Background Thread"
            printVar("CleverTap Device ID", deviceId.orEmpty())
            printVar("Thread", threadType)
        }
    }

    // ========== PUSH TEMPLATES SECTION ==========
    private fun handlePushTemplatesSection(item: Int) {
        val eventNames = listOf(
            "Send Basic Push",
            "Send Carousel Push",
            "Send Manual Carousel Push",
            "Send Filmstrip Carousel Push",
            "Send Rating Push",
            "Send Product Display Notification",
            "Send Linear Product Display Push",
            "Send CTA Notification",
            "Send Zero Bezel Notification",
            "Send Zero Bezel Text Only Notification",
            "Send Timer Notification",
            "Send Input Box Notification",
            "Send Input Box Reply with Event Notification",
            "Send Input Box Reply with Auto Open Notification",
            "Send Input Box Remind Notification DOC FALSE",
            "Send Input Box CTA DOC true",
            "Send Input Box CTA DOC false",
            "Send Input Box Reminder DOC true",
            "Send Input Box Reminder DOC false",
            "Send Three CTA Notification"
        )

        if (item < eventNames.size) {
            logStep("PUSH TEMPLATES", "Pushing event: ${eventNames[item]}")
            cleverTapAPI?.pushEvent(eventNames[item])
        }
    }

    // ========== PROMPT LOCAL IAM SECTION ==========
    private fun handlePromptLocalIAMSection(item: Int) {
        when (item) {
            0 -> showHalfInterstitialLocalIAM()
            1 -> showHalfInterstitialWithImage()
            2 -> showHalfInterstitialWithFallback()
            3 -> showAlertLocalIAM()
            4 -> showAlertWithoutOrientation()
            5 -> showAlertWithFallback()
            6 -> showHardPermissionDialogNoFallback()
            7 -> showHardPermissionDialogWithFallback()
        }
    }

    private fun showHalfInterstitialLocalIAM() {
        logStep("PROMPT LOCAL IAM", "Showing half-interstitial local in-app message")

        val builder = CTLocalInApp.builder()
            .setInAppType(CTLocalInApp.InAppType.HALF_INTERSTITIAL)
            .setTitleText("Get Notified")
            .setMessageText("Please enable notifications on your device to use Push Notifications.")
            .followDeviceOrientation(true)
            .setPositiveBtnText("Allow")
            .setNegativeBtnText("Cancel")
            .setBackgroundColor(Constants.WHITE)
            .setBtnBorderColor(Constants.BLUE)
            .setTitleTextColor(Constants.BLUE)
            .setMessageTextColor(Constants.BLACK)
            .setBtnTextColor(Constants.WHITE)
            .setBtnBackgroundColor(Constants.BLUE)
            .build()

        cleverTapAPI?.promptPushPrimer(builder)
    }

    private fun showHalfInterstitialWithImage() {
        logStep("PROMPT LOCAL IAM", "Showing half-interstitial with image URL")

        val imageUrl = "https://icons.iconarchive.com/icons/treetog/junior/64/camera-icon.png"
        printVar("Image URL", imageUrl)

        val builder = CTLocalInApp.builder()
            .setInAppType(CTLocalInApp.InAppType.HALF_INTERSTITIAL)
            .setTitleText("Get Notified")
            .setMessageText("Please enable notifications on your device to use Push Notifications.")
            .followDeviceOrientation(true)
            .setPositiveBtnText("Allow")
            .setNegativeBtnText("Cancel")
            .setBackgroundColor(Constants.WHITE)
            .setBtnBorderColor(Constants.BLUE)
            .setTitleTextColor(Constants.BLUE)
            .setMessageTextColor(Constants.BLACK)
            .setBtnTextColor(Constants.WHITE)
            .setImageUrl(imageUrl, "Clevertap Camera")
            .setBtnBackgroundColor(Constants.BLUE)
            .build()

        cleverTapAPI?.promptPushPrimer(builder)
    }

    private fun showHalfInterstitialWithFallback() {
        logStep("PROMPT LOCAL IAM", "Showing half-interstitial with fallback to settings")
        printVar("Fallback to Settings", true)

        val builder = CTLocalInApp.builder()
            .setInAppType(CTLocalInApp.InAppType.HALF_INTERSTITIAL)
            .setTitleText("Get Notified")
            .setMessageText("Please enable notifications on your device to use Push Notifications.")
            .followDeviceOrientation(true)
            .setPositiveBtnText("Allow")
            .setNegativeBtnText("Cancel")
            .setBackgroundColor(Constants.WHITE)
            .setBtnBorderColor(Constants.BLUE)
            .setTitleTextColor(Constants.BLUE)
            .setMessageTextColor(Constants.BLACK)
            .setBtnTextColor(Constants.WHITE)
            .setBtnBackgroundColor(Constants.BLUE)
            .setFallbackToSettings(true)
            .build()

        cleverTapAPI?.promptPushPrimer(builder)
    }

    private fun showAlertLocalIAM() {
        logStep("PROMPT LOCAL IAM", "Showing alert local in-app message")

        val builder = CTLocalInApp.builder()
            .setInAppType(CTLocalInApp.InAppType.ALERT)
            .setTitleText("Get Notified")
            .setMessageText("Enable Notification permission")
            .followDeviceOrientation(true)
            .setPositiveBtnText("Allow")
            .setNegativeBtnText("Cancel")
            .build()

        cleverTapAPI?.promptPushPrimer(builder)
    }

    private fun showAlertWithoutOrientation() {
        logStep("PROMPT LOCAL IAM", "Showing alert without device orientation")
        printVar("Follow Device Orientation", false)

        val builder = CTLocalInApp.builder()
            .setInAppType(CTLocalInApp.InAppType.ALERT)
            .setTitleText("Get Notified")
            .setMessageText("Enable Notification permission")
            .followDeviceOrientation(false)
            .setPositiveBtnText("Allow")
            .setNegativeBtnText("Cancel")
            .build()

        cleverTapAPI?.promptPushPrimer(builder)
    }

    private fun showAlertWithFallback() {
        logStep("PROMPT LOCAL IAM", "Showing alert with fallback to settings")
        printVar("Fallback to Settings", true)

        val builder = CTLocalInApp.builder()
            .setInAppType(CTLocalInApp.InAppType.ALERT)
            .setTitleText("Get Notified")
            .setMessageText("Enable Notification permission")
            .followDeviceOrientation(true)
            .setPositiveBtnText("Allow")
            .setNegativeBtnText("Cancel")
            .setFallbackToSettings(true)
            .build()

        cleverTapAPI?.promptPushPrimer(builder)
    }

    private fun showHardPermissionDialogNoFallback() {
        logStep("PROMPT LOCAL IAM", "Showing hard permission dialog (no fallback)")
        printVar("Fallback to Settings", false)

        if (cleverTapAPI?.isPushPermissionGranted == false) {
            cleverTapAPI.promptForPushPermission(false)
        } else {
            log("Notification permission is already granted")
        }
    }

    private fun showHardPermissionDialogWithFallback() {
        logStep("PROMPT LOCAL IAM", "Showing hard permission dialog (with fallback)")
        printVar("Fallback to Settings", true)

        if (cleverTapAPI?.isPushPermissionGranted == false) {
            cleverTapAPI.promptForPushPermission(true)
        } else {
            log("Notification permission is already granted")
        }
    }

    // ========== IN-APP SECTION ==========
    private fun handleInAppSection(item: Int) {
        when (item) {
            0 -> suspendInAppNotifications()
            1 -> discardInAppNotifications()
            2 -> resumeInAppNotifications()
            3 -> discardInAppNotifications(true)
        }
    }

    private fun suspendInAppNotifications() {
        logStep("IN-APP", "Suspending in-app notifications")
        cleverTapAPI?.suspendInAppNotifications()
    }

    private fun discardInAppNotifications(override: Boolean? = null) {
        logStep("IN-APP", "Discarding in-app notifications")
        if (override != null) {
            cleverTapAPI?.discardInAppNotifications(override)
        } else {
            cleverTapAPI?.discardInAppNotifications()
        }
    }

    private fun resumeInAppNotifications() {
        logStep("IN-APP", "Resuming in-app notifications")
        cleverTapAPI?.resumeInAppNotifications()
    }

    // ========== CS IN-APP SECTION ==========
    private fun handleCSInAppSection(item: Int) {
        when (item) {
            0 -> fetchCSInApps()
            1 -> clearAllCSInAppResources()
            2 -> clearExpiredInAppResources()
        }
    }

    private fun fetchCSInApps() {
        logStep("CS IN-APP", "Fetching client-side in-apps")

        cleverTapAPI?.fetchInApps(object : FetchInAppsCallback {
            override fun onInAppsFetched(isSuccess: Boolean) {
                printVar("InApps Fetched", isSuccess)
            }
        })
    }

    private fun clearAllCSInAppResources() {
        logStep("CS IN-APP", "Clearing all client-side in-app resources")
        printVar("Expire Now", false)
        cleverTapAPI?.clearInAppResources(false)
    }

    private fun clearExpiredInAppResources() {
        logStep("CS IN-APP", "Clearing expired in-app resources only")
        printVar("Expire Now", true)
        cleverTapAPI?.clearInAppResources(true)
    }

    // ========== VARIABLES SECTION ==========
    private fun handleVariablesSection(item: Int) {
        when (item) {
            0 -> defineBasicVariables()
            1 -> defineFileVariablesWithListeners()
            2 -> fetchVariables()
            3 -> syncVariables()
            4 -> parseVariables()
            5 -> getVariables()
            6 -> getVariableValues()
            7 -> addVariablesChangedCallback()
            8 -> removeVariablesChangedCallback()
            9 -> addOneTimeVariablesChangedCallback()
            10 -> removeOneTimeVariablesChangedCallback()
            11 -> defineMultipleVarsAndFetch()
            12 -> printAbVariants()
        }
    }

    private fun defineBasicVariables() {
        logStep("VARIABLES", "Defining basic type variables")

        cleverTapAPI?.apply {
            defineVariable("var_int", 3)
            defineVariable("var_long", 4L)
            defineVariable("var_short", 2)
            defineVariable("var_float", 5f)
            defineVariable("var_double", 6)
            defineVariable("var_string", "str")
            defineVariable("var_boolean", true)
        }

        log("Defined variables: var_int, var_long, var_short, var_float, var_double, var_string, var_boolean")
    }

    private fun defineFileVariablesWithListeners() {
        logStep("VARIABLES", "Defining file variables with listeners")

        FileVarsData.defineFileVars(cleverTapAPI!!)

        log("Printing file variables values (may be null if not yet fetched):")
        FileVarsData.printFileVariables(cleverTapAPI!!)
    }

    private fun fetchVariables() {
        logStep("VARIABLES", "Fetching variables")

        cleverTapAPI?.fetchVariables { isSuccess ->
            printVar("Variables Fetched", isSuccess)
        }
    }

    private fun syncVariables() {
        logStep("VARIABLES", "Syncing variables")
        cleverTapAPI?.syncVariables()
    }

    private fun parseVariables() {
        logStep("VARIABLES", "Parsing variables from example object")
        cleverTapAPI?.parseVariables(exampleVariables)
    }

    private fun getVariables() {
        logStep("VARIABLES", "Getting variable objects (basic types)")

        cleverTapAPI?.let { ct ->
            buildString {
                appendLine("Variable Objects:")
                appendLine("var_int: ${ct.getVariable<Int>("var_int")}")
                appendLine("var_long: ${ct.getVariable<Long>("var_long")}")
                appendLine("var_short: ${ct.getVariable<Short>("var_short")}")
                appendLine("var_float: ${ct.getVariable<Float>("var_float")}")
                appendLine("var_double: ${ct.getVariable<Double>("var_double")}")
                appendLine("var_string: ${ct.getVariable<String>("var_string")}")
                appendLine("var_boolean: ${ct.getVariable<Boolean>("var_boolean")}")
            }.also { log(it) }

            FileVarsData.printFileVariables(ct)
        }
    }

    private fun getVariableValues() {
        logStep("VARIABLES", "Getting variable values (basic types)")

        cleverTapAPI?.let { ct ->
            buildString {
                appendLine("Variable Values:")
                appendLine("var_int: ${ct.getVariableValue("var_int")}")
                appendLine("var_long: ${ct.getVariableValue("var_long")}")
                appendLine("var_short: ${ct.getVariableValue("var_short")}")
                appendLine("var_float: ${ct.getVariableValue("var_float")}")
                appendLine("var_double: ${ct.getVariableValue("var_double")}")
                appendLine("var_string: ${ct.getVariableValue("var_string")}")
                appendLine("var_boolean: ${ct.getVariableValue("var_boolean")}")
            }.also { log(it) }

            FileVarsData.printFileVariablesValues(ct, TAG)
        }
    }

    private fun addVariablesChangedCallback() {
        logStep("VARIABLES", "Adding variables changed callback")

        cleverTapAPI?.apply {
            addVariablesChangedCallback(exampleVariables.variablesChangedCallback)
            onVariablesChangedAndNoDownloadsPending(object : VariablesChangedCallback() {
                override fun variablesChanged() {
                    log("Files downloaded, onVariablesChangedAndNoDownloadsPending callback triggered")
                    log("Reprinting file variable data:")
                    FileVarsData.printFileVariables(cleverTapAPI!!)
                }
            })
        }
    }

    private fun removeVariablesChangedCallback() {
        logStep("VARIABLES", "Removing variables changed callback")
        cleverTapAPI?.removeVariablesChangedCallback(exampleVariables.variablesChangedCallback)
    }

    private fun addOneTimeVariablesChangedCallback() {
        logStep("VARIABLES", "Adding one-time variables changed callback")

        cleverTapAPI?.apply {
            addOneTimeVariablesChangedCallback(exampleVariables.oneTimeVariablesChangedCallback)
            onceVariablesChangedAndNoDownloadsPending(object : VariablesChangedCallback() {
                override fun variablesChanged() {
                    log("onceVariablesChangedAndNoDownloadsPending - triggered only once globally")
                }
            })
        }
    }

    private fun removeOneTimeVariablesChangedCallback() {
        logStep("VARIABLES", "Removing one-time variables changed callback")
        cleverTapAPI?.removeOneTimeVariablesChangedCallback(exampleVariables.oneTimeVariablesChangedCallback)
    }

    private fun defineMultipleVarsAndFetch() {
        logStep("VARIABLES", "Defining multiple test variables and fetching")

        defineTestAccountVariables()

        cleverTapAPI?.fetchVariables { isSuccess ->
            printVar("Test Variables Fetched", isSuccess)
            if (isSuccess) {
                printTestVariables()
            }
        }
    }

    private fun printAbVariants() {
        logStep("VARIABLES", "Printing AB Variants")
        val variants = cleverTapAPI?.variants()
        printVar("AB Variants", variants)
    }

    // ========== FILE TYPE VARIABLES SECTION ==========
    private fun handleFileTypeVariablesSection(item: Int) {
        when (item) {
            0 -> defineFileVariablesWithSingleListener()
            1 -> defineFileVariablesWithMultipleListeners()
            2 -> defineGlobalListenersThenFileVars()
            3 -> defineMultipleGlobalListenersThenFileVars()
            4 -> printFileVariables()
            5 -> clearAllFileResources()
        }
    }

    private fun defineFileVariablesWithSingleListener() {
        logStep("FILE TYPE VARIABLES", "Defining file variables with single listener")

        FileVarsData.defineFileVars(cleverTapAPI!!, tag = TAG)

        log("Printing file variables values (may be null if not yet fetched):")
        FileVarsData.printFileVariables(cleverTapAPI!!, tag = TAG)
    }

    private fun defineFileVariablesWithMultipleListeners() {
        logStep("FILE TYPE VARIABLES", "Defining file variables with multiple listeners (3)")
        printVar("Listener Count", 3)

        FileVarsData.defineFileVars(cleverTapAPI!!, tag = TAG, fileReadyListenerCount = 3)

        log("Printing file variables values (may be null if not yet fetched):")
        FileVarsData.printFileVariables(cleverTapAPI!!, tag = TAG)
    }

    private fun defineGlobalListenersThenFileVars() {
        logStep("FILE TYPE VARIABLES", "Adding global listeners then defining file variables")

        FileVarsData.addGlobalCallbacks(cleverTapAPI!!, tag = TAG)
        FileVarsData.defineFileVars(cleverTapAPI!!, tag = TAG)
    }

    private fun defineMultipleGlobalListenersThenFileVars() {
        logStep("FILE TYPE VARIABLES", "Adding multiple global listeners (3) then defining file variables")
        printVar("Listener Count", 3)

        FileVarsData.addGlobalCallbacks(cleverTapAPI!!, tag = TAG, listenerCount = 3)
        FileVarsData.defineFileVars(cleverTapAPI!!, tag = TAG, fileReadyListenerCount = 3)
    }

    private fun printFileVariables() {
        logStep("FILE TYPE VARIABLES", "Printing file variables")
        FileVarsData.printFileVariables(cleverTapAPI!!, tag = TAG)
    }

    private fun clearAllFileResources() {
        logStep("FILE TYPE VARIABLES", "Clearing all file resources")
        printVar("Expire Now", false)
        cleverTapAPI?.clearFileResources(false)
    }

    // ========== LOCALE SECTION ==========
    private fun handleLocaleSection(item: Int) {
        when (item) {
            0 -> setLocale()
        }
    }

    private fun setLocale() {
        logStep("LOCALE", "Setting locale")
        val locale = "en_IN"
        printVar("Locale", locale)
        cleverTapAPI?.locale = locale
    }

    // ========== CUSTOM TEMPLATES SECTION ==========
    private fun handleCustomTemplatesSection(item: Int) {
        when (item) {
            0 -> syncRegisteredCustomTemplates()
        }
    }

    private fun syncRegisteredCustomTemplates() {
        logStep("CUSTOM TEMPLATES", "Syncing registered custom templates")
        cleverTapAPI?.syncRegisteredInAppTemplates()
    }

    // ========== OPT OUT SECTION ==========
    private fun handleOptOutSection(item: Int) {
        val optOutConfigs = listOf(
            Triple(true, true, "userOptOut=true, allowSystemEvents=true"),
            Triple(true, false, "userOptOut=true, allowSystemEvents=false"),
            Triple(false, true, "userOptOut=false, allowSystemEvents=true"),
            Triple(false, false, "userOptOut=false, allowSystemEvents=false"),
            Triple(true, null, "userOptOut=true (single param)"),
            Triple(false, null, "userOptOut=false (single param)")
        )

        if (item < optOutConfigs.size) {
            val (userOptOut, allowSystemEvents, description) = optOutConfigs[item]
            logStep("OPT OUT", "Setting opt out: $description")

            printVar("User Opt Out", userOptOut)
            allowSystemEvents?.let { printVar("Allow System Events", it) }

            if (allowSystemEvents != null) {
                cleverTapAPI?.setOptOut(userOptOut, allowSystemEvents)
            } else {
                cleverTapAPI?.setOptOut(userOptOut)
            }
        }
    }

    // ========== HELPER METHODS ==========

    private fun onUserLogin(cleverTapAPI: CleverTapAPI?) {
        cleverTapAPI ?: return

        val randomN = (0..10_000).random()
        val randomP = (10_000..99_999).random()

        val newProfile = buildMap {
            put("Name", "Don Joe $randomN")
            put("Email", listOf("donjoe$randomN@gmail.com"))
            put("Phone", "+141566$randomP")
            put("CustomOca", "fasdsa")
        }

        printMap("Login Profile", newProfile)
        cleverTapAPI.onUserLogin(newProfile)
    }

    private fun defineTestAccountVariables() {
        val factoryMap = mapOf(
            "int" to 12,
            "str" to "factory str"
        )

        cleverTapAPI?.apply {
            // Factory variables
            defineVariable("factory_var_int", 11)
            defineVariable("factory_var_map", factoryMap)
            defineVariable("group.factory_var_in_group", 13.toByte())
            defineVariable("streaming.quality_auto", true)
            defineVariable("streaming.max_bitrate", 8000)
            defineVariable("streaming.protocol", "HLS")

            // Basic variables
            defineVariable("var_int", 1)
            defineVariable("var_long", 1L)
            defineVariable("var_short", 1)
            defineVariable("var_float", 1.1)
            defineVariable("var_double", 1.1111)
            defineVariable("var_string", "default")
            defineVariable("var_boolean", false)

            // File variables
            defineFileVariable("factory_var_file")
            defineFileVariable("group.factory_var_file_in_group")
            defineFileVariable("factory_file_jpeg")
            defineFileVariable("factory_file_png")
            defineFileVariable("factory_file_gif")
            defineFileVariable("documents.factory_file_pdf")
            defineFileVariable("audio.factory_file_mp3")
            defineFileVariable("video.factory_file_mp4")
        }

        log("Defined test account variables")
    }

    private fun printTestVariables() {
        cleverTapAPI?.let { ct ->
            buildString {
                appendLine("Test Variables:")
                appendLine("factory_var_int: ${ct.getVariableValue("factory_var_int")}")
                appendLine("factory_var_map: ${ct.getVariableValue("factory_var_map")}")
                appendLine("group.factory_var_in_group: ${ct.getVariableValue("group.factory_var_in_group")}")
                appendLine("streaming.quality_auto: ${ct.getVariableValue("streaming.quality_auto")}")
                appendLine("streaming.max_bitrate: ${ct.getVariableValue("streaming.max_bitrate")}")
                appendLine("streaming.protocol: ${ct.getVariableValue("streaming.protocol")}")
                appendLine("var_int: ${ct.getVariableValue("var_int")}")
                appendLine("var_long: ${ct.getVariableValue("var_long")}")
                appendLine("var_short: ${ct.getVariableValue("var_short")}")
                appendLine("var_float: ${ct.getVariableValue("var_float")}")
                appendLine("var_double: ${ct.getVariableValue("var_double")}")
                appendLine("var_string: ${ct.getVariableValue("var_string")}")
                appendLine("var_boolean: ${ct.getVariableValue("var_boolean")}")
            }.also { log(it) }
        }
    }

    private fun logAllProfileProperties() {
        buildString {
            appendLine("Profile Properties:")
            appendLine("  Name: ${cleverTapAPI?.getProperty("Name")}")
            appendLine("  Email: ${cleverTapAPI?.getProperty("Email")}")
            appendLine("  Phone: ${cleverTapAPI?.getProperty("Phone")}")
            appendLine("  Gender: ${cleverTapAPI?.getProperty("Gender")}")
            appendLine("  Employed: ${cleverTapAPI?.getProperty("Employed")}")
            appendLine("  Education: ${cleverTapAPI?.getProperty("Education")}")
            appendLine("  Married: ${cleverTapAPI?.getProperty("Married")}")
            appendLine("  DOB: ${cleverTapAPI?.getProperty("DOB")}")
            appendLine("  Age: ${cleverTapAPI?.getProperty("Age")}")
            appendLine("  MSG-email: ${cleverTapAPI?.getProperty("MSG-email")}")
            appendLine("  MSG-push: ${cleverTapAPI?.getProperty("MSG-push")}")
            appendLine("  MSG-sms: ${cleverTapAPI?.getProperty("MSG-sms")}")
            appendLine("  MyStuffList: ${cleverTapAPI?.getProperty("MyStuffList")}")
            appendLine("  MyStuffArray: ${cleverTapAPI?.getProperty("MyStuffArray")}")
            appendLine("  HeightCm: ${cleverTapAPI?.getProperty("HeightCm")}")
            appendLine("  HairColor: ${cleverTapAPI?.getProperty("HairColor")}")
            appendLine("  Race: ${cleverTapAPI?.getProperty("Race")}")
            appendLine("  County: ${cleverTapAPI?.getProperty("County")}")
            appendLine("  Sport: ${cleverTapAPI?.getProperty("Sport")}")
            appendLine("  MyCarsList: ${cleverTapAPI?.getProperty("MyCarsList")}")
        }.also { log(it) }
    }

    // ========== LOGGING UTILITIES ==========

    /**
     * Logs a step with section and description
     */
    private fun logStep(section: String, description: String) {
        Log.i(TAG, "═══════════════════════════════════════════════════════")
        Log.i(TAG, "SECTION: $section")
        Log.i(TAG, "STEP: $description")
        Log.i(TAG, "═══════════════════════════════════════════════════════")
    }

    /**
     * Logs a simple message
     */
    private fun log(message: String) {
        Log.i(TAG, message)
    }

    /**
     * Prints a single variable with its value
     */
    private fun printVar(name: String, value: Any?) {
        Log.i(TAG, "  ▶ $name: $value")
    }

    /**
     * Prints a map of variables
     */
    private fun printMap(title: String, map: Map<*, *>) {
        Log.i(TAG, "  ▶ $title:")
        map.forEach { (key, value) ->
            Log.i(TAG, "      • $key = $value")
        }
    }

    /**
     * Prints a list of items
     */
    private fun <T> printList(title: String, list: List<T>) {
        Log.i(TAG, "  ▶ $title:")
        list.forEachIndexed { index, item ->
            Log.i(TAG, "      • [$index] $item")
        }
    }
}

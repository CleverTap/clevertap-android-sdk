package com.clevertap.demo.ui.main

import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.clevertap.android.sdk.CTInboxStyleConfig
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.CTLocalInApp
import java.util.*

class HomeScreenViewModel(private val cleverTapAPI: CleverTapAPI?) : ViewModel() {

    val clickCommand: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun onChildClick(groupPosition: Int = 0, childPosition: Int = 0) {
        Log.i("HomeScreenViewModel", "child click $groupPosition $childPosition")
        val commandPosition = "$groupPosition$childPosition"
        clickCommand.value = commandPosition
        when (commandPosition) {
            "00" -> {
                cleverTapAPI?.pushEvent("testEventPushAmp")
            }
            "01" -> {
                //Record an event with properties
                val prodViewedAction = mapOf(
                    "Product Name" to "Casio Chronograph Watch",
                    "Category" to "Mens Accessories", "Price" to 59.99, "Date" to Date()
                )
                cleverTapAPI?.pushEvent("Product viewed", prodViewedAction)
                //cleverTapAPI?.pushEvent("video-inapp")
                //cleverTapAPI?.pushEvent("video-inbox")
                cleverTapAPI?.pushEvent("caurousel-inapp")
                cleverTapAPI?.pushEvent("icon-inbox")
            }
            "02" -> {
                //Record a Charged (Transactional) event
                val chargeDetails = hashMapOf<String, Any>(
                    "Amount" to 300, "Payment Mode" to "Credit card",
                    "Charged ID" to 24052013
                )

                val item1 = hashMapOf<String, Any>(
                    "Product category" to "books",
                    "Book name" to "The Millionaire next door", "Quantity" to 1
                )

                val item2 = hashMapOf<String, Any>(
                    "Product category" to "books",
                    "Book name" to "Achieving inner zen", "Quantity" to 1
                )

                val item3 = hashMapOf<String, Any>(
                    "Product category" to "books",
                    "Book name" to "Chuck it, let's do it", "Quantity" to 5
                )

                val items = arrayListOf<HashMap<String, Any>>()
                items.apply { add(item1); add(item2); add(item3) }

                cleverTapAPI?.pushChargedEvent(chargeDetails, items)
            }
            "03" -> cleverTapAPI?.recordScreen("Cart Screen Viewed")
            "10" -> {
                //Record a profile
                val profileUpdate = HashMap<String, Any>()
                profileUpdate["Name"] = "User Name" // String
                profileUpdate["Email"] = "User@gmail.com" // Email address of the user
                profileUpdate["Phone"] = "+14155551234" // Phone (with the country code, starting with +)
                profileUpdate["Gender"] = "M" // Can be either M or F
                profileUpdate["Employed"] = "Y" // Can be either Y or N
                profileUpdate["Education"] = "Graduate" // Can be either Graduate, College or School
                profileUpdate["Married"] = "Y" // Can be either Y or N
                profileUpdate["DOB"] = Date() // Date of Birth. Set the Date object to the appropriate value first
                profileUpdate["Age"] = 28 // Not required if DOB is set
                profileUpdate["MSG-email"] = false // Disable email notifications
                profileUpdate["MSG-push"] = true // Enable push notifications
                profileUpdate["MSG-sms"] = false // Disable SMS notifications

                profileUpdate["MyStuffList"] = arrayListOf("bag", "shoes") //ArrayList of Strings
                profileUpdate["MyStuffArray"] = arrayOf("Jeans", "Perfume")

                cleverTapAPI?.pushProfile(profileUpdate)
            }
            "11" -> {
                //Update(Replace) Single-Value User Profile Properties
                val profileUpdate = HashMap<String, Any>()
                profileUpdate["Name"] = "Updated User Name" // String
                profileUpdate["Email"] = "UpdatedUser@gmail.com" // Email address of the user
                profileUpdate["Gender"] = "F" // Can be either M or F
                profileUpdate["Employed"] = "N" // Can be either Y or N
                profileUpdate["Education"] = "College" // Can be either Graduate, College or School
                profileUpdate["Married"] = "N" // Can be either Y or N
                profileUpdate["MSG-push"] = false // Disable push notifications

                cleverTapAPI?.pushProfile(profileUpdate)
            }
            "12" -> {
                //Update(Add) Single-Value User Profile Properties
                val profileUpdate = mapOf("Customer Type" to "Silver", "Preferred Language" to "English")
                cleverTapAPI?.pushProfile(profileUpdate)
            }
            "13" -> {
                //Update(Remove) Single-Value User Profile Properties or
                //Update(Remove) Can be used to remove PII data(for eg. Email,Phone,Name), locally
                cleverTapAPI?.removeValueForKey("Customer Type")
//                cleverTapAPI?.removeValueForKey("Email")
//                cleverTapAPI?.removeValueForKey("Phone")
//                cleverTapAPI?.removeValueForKey("Name")
            }
            "14" -> {
                // Update(Replace) Multi-Value property
                cleverTapAPI?.setMultiValuesForKey("MyStuffList", arrayListOf("Updated Bag", "Updated Shoes"))
            }
            "15" -> {
                // Update(Add) Multi-Value property
                cleverTapAPI?.addMultiValueForKey("MyStuffList", "Coat")
                // or
                cleverTapAPI?.addMultiValuesForKey("MyStuffList", arrayListOf("Socks", "Scarf"))
            }
            "16" -> {
                // Update(Remove) Multi-Value property
                cleverTapAPI?.removeMultiValueForKey("MyStuffList", "Coat")
                // or
                cleverTapAPI?.removeMultiValuesForKey("MyStuffList", arrayListOf("Socks", "Scarf"))
            }
            "17" -> {
                //Update(Add) Increment Value
                cleverTapAPI?.incrementValue("score", 50)
            }
            "18" -> {
                // Update(Add) Decrement Value
                cleverTapAPI?.decrementValue("score", 30)
            }
            "19" -> {
                // Profile location
                cleverTapAPI?.location = cleverTapAPI?.location
            }
            "110" -> {
                // Get Profile Info
                println("Profile Name = ${cleverTapAPI?.getProperty("Name")}")
                println("Profile CleverTapId = ${cleverTapAPI?.cleverTapID}")
                println("Profile CleverTap AttributionIdentifier = ${cleverTapAPI?.cleverTapAttributionIdentifier}")
            }
            "111" -> {
                // onUserLogin
                val newProfile = HashMap<String, Any>()
                val n = (0..10_000).random()
                val p = (10_000..99_999).random()
                newProfile["Name"] = "Don Joe $n" // String
                newProfile["Email"] = "donjoe$n@gmail.com" // Email address of the user
                newProfile["Phone"] = "+141566$p" // Phone (with the country code, starting with +)
                newProfile["Identity"] = "00002" // Identity of the user
                // add any other key value pairs.....
                cleverTapAPI?.onUserLogin(newProfile)
            }
            "20" -> {
                // Open Inbox(Customised, with tabs)
                val inboxTabs =
                    arrayListOf("Promotions", "Offers", "Others")//Anything after the first 2 will be ignored
                CTInboxStyleConfig().apply {
                    tabs = inboxTabs //Do not use this if you don't want to use tabs
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
                    cleverTapAPI?.showAppInbox(this) //Opens activity With Tabs
                }
            }
            "21" -> {
                // Open Inbox(Customised, without tabs)
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
                    cleverTapAPI?.showAppInbox(this) //Opens activity Without Tabs
                }
            }


            "22" -> println("Total inbox message count = ${cleverTapAPI?.inboxMessageCount}") // show total inbox message count
            "23" -> println("Unread inbox message count = ${cleverTapAPI?.inboxMessageUnreadCount}") // show unread inbox message count
            "24" ->  // All inbox messages
                cleverTapAPI?.allInboxMessages?.forEach {
                    println("All inbox messages ID = ${it.messageId}")
                }

            "25" ->  // All unread inbox messages
                cleverTapAPI?.unreadInboxMessages?.forEach {
                    println("All unread inbox messages ID = ${it.messageId}")
                }
            "26" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Get message object belonging to the given message id only. Message id should be a String
                firstMessageId?.also {
                    val inboxMessageForId = cleverTapAPI?.getInboxMessageForId(it)
                    println("inboxMessage For Id $it = ${inboxMessageForId?.data}")
                } ?: println("inboxMessage Id is null")
            }
            "27" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Delete message object belonging to the given message id only. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.deleteInboxMessage(it)
                    println("Deleted inboxMessage For Id = $it")
                } ?: println("inboxMessage Id is null")
            }
            "28" -> {
                val firstMessage = cleverTapAPI?.allInboxMessages?.firstOrNull()
                //Delete message object belonging to the given CTInboxMessage.
                firstMessage?.also {
                    cleverTapAPI?.deleteInboxMessage(it)
                    println("Deleted inboxMessage = ${it.messageId}")
                } ?: println("inboxMessage is null")
            }
            "29" -> {
                val firstMessageId = cleverTapAPI?.unreadInboxMessages?.firstOrNull()?.messageId
                //Mark Message as Read. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.markReadInboxMessage(it)
                    println("Marked Message as Read For Id = $it")
                } ?: println("inboxMessage Id is null")
            }
            "210" -> {
                val firstMessage = cleverTapAPI?.unreadInboxMessages?.firstOrNull()
                //Mark message as Read. Message should object of CTInboxMessage
                firstMessage?.also {
                    cleverTapAPI?.markReadInboxMessage(it)
                    println("Marked Message as Read = ${it.messageId}")
                } ?: println("inboxMessage is null")
            }
            "211" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Raise Notification Viewed event for Inbox Message. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.pushInboxNotificationViewedEvent(it)
                    println("Raised Notification Viewed event For Id = $it")
                } ?: println("inboxMessage Id is null")
            }
            "212" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Raise Notification Clicked event for Inbox Message. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.pushInboxNotificationClickedEvent(it)
                    println("Raised Notification Clicked event For Id = $it")
                } ?: println("inboxMessage Id is null")
            }
            "30" -> {
                val displayUnitID = cleverTapAPI?.allDisplayUnits?.firstOrNull()?.unitID
                //Get DisplayUnit by unit id. unit id should be a String
                displayUnitID?.also {
                    val displayUnitForId = cleverTapAPI?.getDisplayUnitForId(it)
                    println("DisplayUnit for Id $it = $displayUnitForId")
                } ?: println("DisplayUnit Id is null")
            }
            "31" -> println("All Display Units = ${cleverTapAPI?.allDisplayUnits}") // get all display units
            "32" -> {
                val displayUnitID = cleverTapAPI?.allDisplayUnits?.firstOrNull()?.unitID
                //Raise Notification Viewed event for DisplayUnit. Message id should be a String
                displayUnitID?.also {
                    cleverTapAPI?.pushDisplayUnitViewedEventForID(it)
                    println("Raised Notification Viewed event For DisplayUnit Id = $it")
                } ?: println("DisplayUnit Id is null")
            }
            "33" -> {
                val displayUnitID = cleverTapAPI?.allDisplayUnits?.firstOrNull()?.unitID
                //Raise Notification Clicked event for DisplayUnit. Message id should be a String
                displayUnitID?.also {
                    cleverTapAPI?.pushDisplayUnitClickedEventForID(it)
                    println("Raised Notification Clicked event For DisplayUnit Id = $it")
                } ?: println("DisplayUnit Id is null")
            }
            "40" -> {
                val hashMap = hashMapOf<String, Any>(
                    "text color" to "red", "msg count" to 100, "price" to 100.50, "is shown" to true,
                    "json" to """{"key":"val","key2":50}"""
                )
                cleverTapAPI?.productConfig()?.setDefaults(hashMap)
            }
            "41" -> cleverTapAPI?.productConfig()?.fetch()
            "42" -> cleverTapAPI?.productConfig()?.activate()
            "43" -> cleverTapAPI?.productConfig()?.fetchAndActivate()
            "44" -> cleverTapAPI?.productConfig()?.reset()
            "45" -> cleverTapAPI?.productConfig()?.fetch(60)
            "46" -> //get all product config values
                cleverTapAPI?.productConfig()?.apply {
                    println("Product Config text color val in string : ${getString("text color")}")
                    println("Product Config is shown val in boolean : ${getBoolean("is shown")}")
                    println("Product Config msg count val in long : ${getLong("msg count")}")
                    println("Product Config price val in double : ${getDouble("price")}")
                    println("Product Config json val in string : ${getString("json")}")
                }
            "47" -> println("Product Config lastFetchTimeStampInMillis = ${cleverTapAPI?.productConfig()?.lastFetchTimeStampInMillis}")
            "50" -> println(
                "Feature Flags is shown val in boolean = ${
                    cleverTapAPI?.featureFlag()?.get("is shown", true)
                }"
            )
            "80" -> println("CleverTapAttribution Identifier = ${cleverTapAPI?.cleverTapAttributionIdentifier}")
            "81" -> cleverTapAPI?.getCleverTapID {
                println(
                    "CleverTap DeviceID from Application class= $it, thread=${
                        if (Looper.myLooper() == Looper.getMainLooper()) "mainthread" else "bg thread"
                        // Current Thread is Main Thread.
                    }"
                )
            }
            "90"-> cleverTapAPI?.pushEvent("Send Basic Push")
            "91"-> cleverTapAPI?.pushEvent("Send Carousel Push")
            "92"-> cleverTapAPI?.pushEvent("Send Manual Carousel Push")
            "93"-> cleverTapAPI?.pushEvent("Send Filmstrip Carousel Push")
            "94"-> cleverTapAPI?.pushEvent("Send Rating Push")
            "95"-> cleverTapAPI?.pushEvent("Send Product Display Notification")
            "96"-> cleverTapAPI?.pushEvent("Send Linear Product Display Push")
            "97"-> cleverTapAPI?.pushEvent("Send CTA Notification")
            "98"-> cleverTapAPI?.pushEvent("Send Zero Bezel Notification")
            "99"-> cleverTapAPI?.pushEvent("Send Zero Bezel Text Only Notification")
            "910"-> cleverTapAPI?.pushEvent("Send Timer Notification")
            "911"-> cleverTapAPI?.pushEvent("Send Input Box Notification")
            "912"-> cleverTapAPI?.pushEvent("Send Input Box Reply with Event Notification")
            "913"-> cleverTapAPI?.pushEvent("Send Input Box Reply with Auto Open Notification")
            "914"-> cleverTapAPI?.pushEvent("Send Input Box Remind Notification DOC FALSE")
            "915"-> cleverTapAPI?.pushEvent("Send Input Box CTA DOC true")
            "916"-> cleverTapAPI?.pushEvent("Send Input Box CTA DOC false")
            "917"-> cleverTapAPI?.pushEvent("Send Input Box Reminder DOC true")
            "918"-> cleverTapAPI?.pushEvent("Send Input Box Reminder DOC false")
            "919"-> cleverTapAPI?.pushEvent("Send Three CTA Notification")
            "100"-> {
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

            "101"->{
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
                    .setImageUrl("https://icons.iconarchive.com/icons/treetog/junior/64/camera-icon.png")
                    .setBtnBackgroundColor(Constants.BLUE)
                    .build()
                cleverTapAPI?.promptPushPrimer(builder)
            }

            "102"->{
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

            "103"->{
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

            "104"->{
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

            "105"->{
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

            "106"->{
                if (cleverTapAPI?.isPushPermissionGranted == false) {
                    cleverTapAPI.promptForPushPermission(false)
                }else{
                    Log.v("HomeScreenViewModel","Notification permission is already granted.")
                }
            }

            "107"->{
                if (cleverTapAPI?.isPushPermissionGranted == false) {
                    cleverTapAPI.promptForPushPermission(true)
                }else{
                    Log.v("HomeScreenViewModel","Notification permission is already granted.")
                }
            }

            //"60" -> webViewClickListener?.onWebViewClick()

        }
    }
}

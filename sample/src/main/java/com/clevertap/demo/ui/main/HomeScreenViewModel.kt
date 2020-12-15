package com.clevertap.demo.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.clevertap.android.sdk.CTInboxStyleConfig
import com.clevertap.android.sdk.CleverTapAPI
import java.util.Date

class HomeScreenViewModel(private val cleverTapAPI: CleverTapAPI?) : ViewModel() {

    val clickCommand: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun onChildClick(groupPosition: Int = 0, childPosition: Int = 0) {
        Log.i("HomeScreenViewModel", "child click $groupPosition $childPosition")
        val commandPosition = "$groupPosition$childPosition"
        clickCommand.value = commandPosition
        when (commandPosition) {
            "00" -> cleverTapAPI?.pushEvent("testEvent")
            "01" -> {
                //Record an event with properties
                val prodViewedAction = mapOf(
                    "Product Name" to "Casio Chronograph Watch",
                    "Category" to "Mens Accessories", "Price" to 59.99, "Date" to Date()
                )
                cleverTapAPI?.pushEvent("Product viewed", prodViewedAction)
            }
            "02" -> {
                //Record a Charged (Transactional) event
                val chargeDetails = hashMapOf(
                    "Amount" to 300, "Payment Mode" to "Credit card",
                    "Charged ID" to 24052013
                )

                val item1 = hashMapOf(
                    "Product category" to "books",
                    "Book name" to "The Millionaire next door", "Quantity" to 1
                )

                val item2 = hashMapOf(
                    "Product category" to "books",
                    "Book name" to "Achieving inner zen", "Quantity" to 1
                )

                val item3 = hashMapOf(
                    "Product category" to "books",
                    "Book name" to "Chuck it, let's do it", "Quantity" to 5
                )

                val items = arrayListOf<HashMap<String, Any>>()
                items.apply { add(item1); add(item2); add(item3) }

                cleverTapAPI?.pushChargedEvent(chargeDetails, items)
            }
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
                //Update(Remove) Single-Value User Profile Properties
                cleverTapAPI?.removeValueForKey("Customer Type")
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
                // Profile location
                cleverTapAPI?.location = cleverTapAPI?.location
            }
            "18" -> {
                // Get Profile Info
                println("Profile Name = ${cleverTapAPI?.getProperty("Name")}")
                println("Profile CleverTapId = ${cleverTapAPI?.cleverTapID}")
                println("Profile CleverTap AttributionIdentifier = ${cleverTapAPI?.cleverTapAttributionIdentifier}")
            }
            "20" -> {
                // Open Inbox
                val inboxTabs =
                    arrayListOf("Promotions", "Offers", "Others")//Anything after the first 2 will be ignored
                CTInboxStyleConfig().apply {
                    tabs = inboxTabs //Do not use this if you don't want to use tabs
                    tabBackgroundColor = "#6960EC"
                    selectedTabIndicatorColor = "#FFFFFF"
                    selectedTabColor = "#FFFFFF"
                    unselectedTabColor = "#E5E4E2"
                    backButtonColor = "#FFFFFF"
                    navBarTitleColor = "#FFFFFF"
                    navBarTitle = "MY INBOX"
                    navBarColor = "#3F51B5"
                    inboxBackgroundColor = "#E5E4E2"
                    cleverTapAPI?.showAppInbox(this) //Opens activity With Tabs
                }
            }
            "21" -> println("Total inbox message count = ${cleverTapAPI?.inboxMessageCount}") // show total inbox message count
            "22" -> println("Unread inbox message count = ${cleverTapAPI?.inboxMessageUnreadCount}") // show unread inbox message count
            "23" ->  // All inbox messages
                cleverTapAPI?.allInboxMessages?.forEach {
                    println("All inbox messages ID = ${it.messageId}")
                }

            "24" ->  // All unread inbox messages
                cleverTapAPI?.unreadInboxMessages?.forEach {
                    println("All unread inbox messages ID = ${it.messageId}")
                }
            "25" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Get message object belonging to the given message id only. Message id should be a String
                firstMessageId?.also {
                    val inboxMessageForId = cleverTapAPI?.getInboxMessageForId(it)
                    println("inboxMessage For Id $it = ${inboxMessageForId?.data}")
                } ?: println("inboxMessage Id is null")
            }
            "26" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Delete message object belonging to the given message id only. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.deleteInboxMessage(it)
                    println("Deleted inboxMessage For Id = $it")
                } ?: println("inboxMessage Id is null")
            }
            "27" -> {
                val firstMessage = cleverTapAPI?.allInboxMessages?.firstOrNull()
                //Delete message object belonging to the given CTInboxMessage.
                firstMessage?.also {
                    cleverTapAPI?.deleteInboxMessage(it)
                    println("Deleted inboxMessage = ${it.messageId}")
                } ?: println("inboxMessage is null")
            }
            "28" -> {
                val firstMessageId = cleverTapAPI?.unreadInboxMessages?.firstOrNull()?.messageId
                //Mark Message as Read. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.markReadInboxMessage(it)
                    println("Marked Message as Read For Id = $it")
                } ?: println("inboxMessage Id is null")
            }
            "29" -> {
                val firstMessage = cleverTapAPI?.unreadInboxMessages?.firstOrNull()
                //Mark message as Read. Message should object of CTInboxMessage
                firstMessage?.also {
                    cleverTapAPI?.markReadInboxMessage(it)
                    println("Marked Message as Read = ${it.messageId}")
                } ?: println("inboxMessage is null")
            }
            "210" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Raise Notification Viewed event for Inbox Message. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.pushInboxNotificationViewedEvent(it)
                    println("Raised Notification Viewed event For Id = $it")
                } ?: println("inboxMessage Id is null")
            }
            "211" -> {
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
                val hashMap = hashMapOf(
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
            //"60" -> webViewClickListener?.onWebViewClick()

        }
    }
}

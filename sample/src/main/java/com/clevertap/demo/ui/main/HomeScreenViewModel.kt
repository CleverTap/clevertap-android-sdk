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
import java.util.Date

private const val TAG = "HomeScreenViewModel"

class HomeScreenViewModel(private val cleverTapAPI: CleverTapAPI?) : ViewModel() {

    val clickCommand: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun onChildClick(groupPosition: Int = 0, childPosition: Int = 0) {
        Log.i("HomeScreenViewModel", "child click $groupPosition $childPosition")
        val commandPosition = "$groupPosition-$childPosition"
        clickCommand.value = commandPosition
        val exampleVariables = ExampleVariables()

        when (commandPosition) {
            "0-0" -> {
                cleverTapAPI?.pushEvent("testEventPushAmp")
                cleverTapAPI?.pushEvent("BlockBRTesting")
            }

            "0-1" -> {
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

            "0-2" -> {
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

            "0-3" -> cleverTapAPI?.recordScreen("Cart Screen Viewed")
            "1-0" -> {
                //Record a profile
                val profileUpdate = HashMap<String, Any>()
                profileUpdate["Name"] = "User Name" // String
                profileUpdate["Email"] = "User@gmail.com" // Email address of the user
                profileUpdate["Phone"] =
                    "+14155551234" // Phone (with the country code, starting with +)
                profileUpdate["Gender"] = "M" // Can be either M or F
                profileUpdate["Employed"] = "Y" // Can be either Y or N
                profileUpdate["Education"] = "Graduate" // Can be either Graduate, College or School
                profileUpdate["Married"] = "Y" // Can be either Y or N
                profileUpdate["DOB"] =
                    Date() // Date of Birth. Set the Date object to the appropriate value first
                profileUpdate["Age"] = 28 // Not required if DOB is set
                profileUpdate["MSG-email"] = false // Disable email notifications
                profileUpdate["MSG-push"] = true // Enable push notifications
                profileUpdate["MSG-sms"] = false // Disable SMS notifications

                profileUpdate["MyStuffList"] = arrayListOf("bag", "shoes") //ArrayList of Strings
                profileUpdate["MyStuffArray"] = arrayOf("Jeans", "Perfume")

                cleverTapAPI?.pushProfile(profileUpdate)
            }

            "1-1" -> {
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

            "1-2" -> {
                //Update(Add) Single-Value User Profile Properties
                val profileUpdate =
                    mapOf("Customer Type" to "Silver", "Preferred Language" to "English")
                cleverTapAPI?.pushProfile(profileUpdate)
            }

            "1-3" -> {
                //Update(Remove) Single-Value User Profile Properties or
                //Update(Remove) Can be used to remove PII data(for eg. Email,Phone,Name), locally
                cleverTapAPI?.removeValueForKey("Customer Type")
//                cleverTapAPI?.removeValueForKey("Email")
//                cleverTapAPI?.removeValueForKey("Phone")
//                cleverTapAPI?.removeValueForKey("Name")
            }

            "1-4" -> {
                // Update(Replace) Multi-Value property
                cleverTapAPI?.setMultiValuesForKey(
                    "MyStuffList",
                    arrayListOf("Updated Bag", "Updated Shoes")
                )
            }

            "1-5" -> {
                // Update(Add) Multi-Value property
                cleverTapAPI?.addMultiValueForKey("MyStuffList", "Coat")
                // or
                cleverTapAPI?.addMultiValuesForKey("MyStuffList", arrayListOf("Socks", "Scarf"))
            }

            "1-6" -> {
                // Update(Remove) Multi-Value property
                cleverTapAPI?.removeMultiValueForKey("MyStuffList", "Coat")
                // or
                cleverTapAPI?.removeMultiValuesForKey("MyStuffList", arrayListOf("Socks", "Scarf"))
            }

            "1-7" -> {
                //Update(Add) Increment Value
                cleverTapAPI?.incrementValue("score", 50)
            }

            "1-8" -> {
                // Update(Add) Decrement Value
                cleverTapAPI?.decrementValue("score", 30)
            }

            "1-9" -> {
                // Profile location
                cleverTapAPI?.location = cleverTapAPI?.location
            }

            "1-10" -> {
                // Get Profile Info
                Log.i(TAG, "Profile Name = ${cleverTapAPI?.getProperty("Name")}")
                Log.i(TAG, "Profile CleverTapId = ${cleverTapAPI?.cleverTapID}")
                Log.i(
                    TAG,
                    "Profile CleverTap AttributionIdentifier = ${cleverTapAPI?.cleverTapAttributionIdentifier}"
                )
            }

            "1-11" -> {
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

            "2-0" -> {
                // Open Inbox(Customised, with tabs)
                val inboxTabs =
                    arrayListOf(
                        "Promotions",
                        "Offers",
                        "Others"
                    )//Anything after the first 2 will be ignored
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

            "2-1" -> {
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

            "2-2" -> Log.i(
                TAG,
                "Total inbox message count = ${cleverTapAPI?.inboxMessageCount}"
            ) // show total inbox message count
            "2-3" -> Log.i(
                TAG,
                "Unread inbox message count = ${cleverTapAPI?.inboxMessageUnreadCount}"
            ) // show unread inbox message count
            "2-4" ->  // All inbox messages
                cleverTapAPI?.allInboxMessages?.forEach {
                    Log.i(TAG, "All inbox messages ID = ${it.messageId}")
                }

            "2-5" ->  // All unread inbox messages
                cleverTapAPI?.unreadInboxMessages?.forEach {
                    Log.i(TAG, "All unread inbox messages ID = ${it.messageId}")
                }

            "2-6" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Get message object belonging to the given message id only. Message id should be a String
                firstMessageId?.also {
                    val inboxMessageForId = cleverTapAPI?.getInboxMessageForId(it)
                    Log.i(TAG, "inboxMessage For Id $it = ${inboxMessageForId?.data}")
                } ?: Log.i(TAG, "inboxMessage Id is null")
            }

            "2-7" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Delete message object belonging to the given message id only. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.deleteInboxMessage(it)
                    Log.i(TAG, "Deleted inboxMessage For Id = $it")
                } ?: Log.i(TAG, "inboxMessage Id is null")
            }

            "2-8" -> {
                val firstMessage = cleverTapAPI?.allInboxMessages?.firstOrNull()
                //Delete message object belonging to the given CTInboxMessage.
                firstMessage?.also {
                    cleverTapAPI?.deleteInboxMessage(it)
                    Log.i(TAG, "Deleted inboxMessage = ${it.messageId}")
                } ?: Log.i(TAG, "inboxMessage is null")
            }

            "2-9" -> {
                val messageIDs = ArrayList<String>()
                cleverTapAPI?.unreadInboxMessages?.forEach {
                    messageIDs.add(it.messageId)
                }
                //Delete multiple messages. List of message id should be of type String
                messageIDs.also {
                    cleverTapAPI?.deleteInboxMessagesForIDs(it)
                    Log.i(TAG, "Deleted list of inboxMessages For IDs = $it")
                }
            }

            "2-10" -> {
                val firstMessageId = cleverTapAPI?.unreadInboxMessages?.firstOrNull()?.messageId
                //Mark Message as Read. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.markReadInboxMessage(it)
                    Log.i(TAG, "Marked Message as Read For Id = $it")
                } ?: Log.i(TAG, "inboxMessage Id is null")
            }

            "2-11" -> {
                val firstMessage = cleverTapAPI?.unreadInboxMessages?.firstOrNull()
                //Mark message as Read. Message should object of CTInboxMessage
                firstMessage?.also {
                    cleverTapAPI?.markReadInboxMessage(it)
                    Log.i(TAG, "Marked Message as Read = ${it.messageId}")
                } ?: Log.i(TAG, "inboxMessage is null")
            }

            "2-12" -> {
                val messageIDs = ArrayList<String>()
                cleverTapAPI?.unreadInboxMessages?.forEach {
                    messageIDs.add(it.messageId)
                }
                //Mark multiple messages as read. List of message ids should be of type String
                messageIDs.also {
                    cleverTapAPI?.markReadInboxMessagesForIDs(it)
                    Log.i(TAG, "Marked Messages as read for list of IDs = $it")
                }
            }

            "2-13" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Raise Notification Viewed event for Inbox Message. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.pushInboxNotificationViewedEvent(it)
                    Log.i(TAG, "Raised Notification Viewed event For Id = $it")
                } ?: Log.i(TAG, "inboxMessage Id is null")
            }

            "2-14" -> {
                val firstMessageId = cleverTapAPI?.allInboxMessages?.firstOrNull()?.messageId
                //Raise Notification Clicked event for Inbox Message. Message id should be a String
                firstMessageId?.also {
                    cleverTapAPI?.pushInboxNotificationClickedEvent(it)
                    Log.i(TAG, "Raised Notification Clicked event For Id = $it")
                } ?: Log.i(TAG, "inboxMessage Id is null")
            }

            "2-15" -> {
                val customData = cleverTapAPI?.allInboxMessages?.firstOrNull()?.customData
                Log.i(TAG, "inboxMessage customData = $customData")
            }

            "3-0" -> {
                val displayUnitID = cleverTapAPI?.allDisplayUnits?.firstOrNull()?.unitID
                //Get DisplayUnit by unit id. unit id should be a String
                displayUnitID?.also {
                    val displayUnitForId = cleverTapAPI?.getDisplayUnitForId(it)
                    Log.i(TAG, "DisplayUnit for Id $it = $displayUnitForId")
                } ?: Log.i(TAG, "DisplayUnit Id is null")
            }

            "3-1" -> Log.i(
                TAG,
                "All Display Units = ${cleverTapAPI?.allDisplayUnits}"
            ) // get all display units
            "3-2" -> {
                val displayUnitID = cleverTapAPI?.allDisplayUnits?.firstOrNull()?.unitID
                //Raise Notification Viewed event for DisplayUnit. Message id should be a String
                displayUnitID?.also {
                    cleverTapAPI?.pushDisplayUnitViewedEventForID(it)
                    Log.i(TAG, "Raised Notification Viewed event For DisplayUnit Id = $it")
                } ?: Log.i(TAG, "DisplayUnit Id is null")
            }

            "3-3" -> {
                val displayUnitID = cleverTapAPI?.allDisplayUnits?.firstOrNull()?.unitID
                //Raise Notification Clicked event for DisplayUnit. Message id should be a String
                displayUnitID?.also {
                    cleverTapAPI?.pushDisplayUnitClickedEventForID(it)
                    Log.i(TAG, "Raised Notification Clicked event For DisplayUnit Id = $it")
                } ?: Log.i(TAG, "DisplayUnit Id is null")
            }

            "4-0" -> {
                val hashMap = hashMapOf<String, Any>(
                    "text color" to "red",
                    "msg count" to 100,
                    "price" to 100.50,
                    "is shown" to true,
                    "json" to """{"key":"val","key2":50}"""
                )
                cleverTapAPI?.productConfig()?.setDefaults(hashMap)
            }

            "4-1" -> cleverTapAPI?.productConfig()?.fetch()
            "4-2" -> cleverTapAPI?.productConfig()?.activate()
            "4-3" -> cleverTapAPI?.productConfig()?.fetchAndActivate()
            "4-4" -> cleverTapAPI?.productConfig()?.reset()
            "4-5" -> cleverTapAPI?.productConfig()?.fetch(60)
            "4-6" -> //get all product config values
                cleverTapAPI?.productConfig()?.apply {
                    Log.i(
                        TAG,
                        "Product Config text color val in string : ${getString("text color")}"
                    )
                    Log.i(TAG, "Product Config is shown val in boolean : ${getBoolean("is shown")}")
                    Log.i(TAG, "Product Config msg count val in long : ${getLong("msg count")}")
                    Log.i(TAG, "Product Config price val in double : ${getDouble("price")}")
                    Log.i(TAG, "Product Config json val in string : ${getString("json")}")
                }

            "4-7" -> Log.i(
                TAG,
                "Product Config lastFetchTimeStampInMillis = ${cleverTapAPI?.productConfig()?.lastFetchTimeStampInMillis}"
            )

            "5-0" -> Log.i(
                TAG,
                "Feature Flags is shown val in boolean = ${
                    cleverTapAPI?.featureFlag()?.get("is shown", true)
                }"
            )

            "8-0" -> Log.i(
                TAG,
                "CleverTapAttribution Identifier = ${cleverTapAPI?.cleverTapAttributionIdentifier}"
            )

            "8-1" -> cleverTapAPI?.getCleverTapID {
                Log.i(
                    TAG,
                    "CleverTap DeviceID from Application class= $it, thread=${
                        if (Looper.myLooper() == Looper.getMainLooper()) "mainthread" else "bg thread"
                        // Current Thread is Main Thread.
                    }"
                )
            }

            "9-0" -> cleverTapAPI?.pushEvent("Send Basic Push")
            "9-1" -> cleverTapAPI?.pushEvent("Send Carousel Push")
            "9-2" -> cleverTapAPI?.pushEvent("Send Manual Carousel Push")
            "9-3" -> cleverTapAPI?.pushEvent("Send Filmstrip Carousel Push")
            "9-4" -> cleverTapAPI?.pushEvent("Send Rating Push")
            "9-5" -> cleverTapAPI?.pushEvent("Send Product Display Notification")
            "9-6" -> cleverTapAPI?.pushEvent("Send Linear Product Display Push")
            "9-7" -> cleverTapAPI?.pushEvent("Send CTA Notification")
            "9-8" -> cleverTapAPI?.pushEvent("Send Zero Bezel Notification")
            "9-9" -> cleverTapAPI?.pushEvent("Send Zero Bezel Text Only Notification")
            "9-10" -> cleverTapAPI?.pushEvent("Send Timer Notification")
            "9-11" -> cleverTapAPI?.pushEvent("Send Input Box Notification")
            "9-12" -> cleverTapAPI?.pushEvent("Send Input Box Reply with Event Notification")
            "9-13" -> cleverTapAPI?.pushEvent("Send Input Box Reply with Auto Open Notification")
            "9-14" -> cleverTapAPI?.pushEvent("Send Input Box Remind Notification DOC FALSE")
            "9-15" -> cleverTapAPI?.pushEvent("Send Input Box CTA DOC true")
            "9-16" -> cleverTapAPI?.pushEvent("Send Input Box CTA DOC false")
            "9-17" -> cleverTapAPI?.pushEvent("Send Input Box Reminder DOC true")
            "9-18" -> cleverTapAPI?.pushEvent("Send Input Box Reminder DOC false")
            "9-19" -> cleverTapAPI?.pushEvent("Send Three CTA Notification")
            "10-0" -> {
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

            "10-1" -> {
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

            "10-2" -> {
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

            "10-3" -> {
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

            "10-4" -> {
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

            "10-5" -> {
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

            "10-6" -> {
                if (cleverTapAPI?.isPushPermissionGranted == false) {
                    cleverTapAPI.promptForPushPermission(false)
                } else {
                    Log.v("HomeScreenViewModel", "Notification permission is already granted.")
                }
            }

            "10-7" -> {
                if (cleverTapAPI?.isPushPermissionGranted == false) {
                    cleverTapAPI.promptForPushPermission(true)
                } else {
                    Log.v("HomeScreenViewModel", "Notification permission is already granted.")
                }
            }

            "11-0" -> {
                cleverTapAPI?.pushEvent("Footer InApp")
            }

            "11-1" -> {
                cleverTapAPI?.pushEvent("Footer InApp without image")
            }

            "11-2" -> {
                cleverTapAPI?.pushEvent("Header")
            }

            "12-0" -> {
                cleverTapAPI?.fetchInApps(object : FetchInAppsCallback {
                    override fun onInAppsFetched(isSuccess: Boolean) {
                        Log.i(TAG, "InAppsFetched = $isSuccess")
                    }
                })
            }

            "12-1" -> {
                cleverTapAPI?.clearInAppResources(false)
            }

            "12-2" -> {
                cleverTapAPI?.clearInAppResources(true)
            }

            "13-0" -> {
                cleverTapAPI?.defineVariable("var_int", 3)
                cleverTapAPI?.defineVariable("var_long", 4L)
                cleverTapAPI?.defineVariable("var_short", 2)
                cleverTapAPI?.defineVariable("var_float", 5f)
                cleverTapAPI?.defineVariable("var_double", 6)
                cleverTapAPI?.defineVariable("var_string", "str")
                cleverTapAPI?.defineVariable("var_boolean", true)
            }

            "13-1" -> {

                Log.i(TAG, "onChildClick: 13-1: Starting to define file vars:")

                FileVarsData.defineFileVars(cleverTapAPI!!)

                Log.i(
                    TAG,
                    "onChildClick: 13-1: Printing file vars values, they might be null if not yet fetched"
                )
                FileVarsData.printFileVariables(cleverTapAPI)
            }

            "13-2" -> {
                cleverTapAPI?.fetchVariables { isSuccess ->
                    Log.i(
                        TAG,
                        "Variables Fetched = $isSuccess"
                    )
                }
            }

            "13-3" -> {
                cleverTapAPI?.syncVariables()
            }

            "13-4" -> {
                cleverTapAPI?.parseVariables(exampleVariables)
            }

            "13-5" -> {

                Log.i(TAG, "onChildClick: 13-5")

                val builder = buildString {
                    append("Printing variables (basic types) :")
                    appendLine()
                    append(cleverTapAPI!!.getVariable<Int>("var_int"))
                    appendLine()
                    append(cleverTapAPI.getVariable<Long>("var_long"))
                    appendLine()
                    append(cleverTapAPI.getVariable<Short>("var_short"))
                    appendLine()
                    append(cleverTapAPI.getVariable<Float>("var_float"))
                    appendLine()
                    append(cleverTapAPI.getVariable<Double>("var_double"))
                    appendLine()
                    append(cleverTapAPI.getVariable<String>("var_string"))
                    appendLine()
                    append(cleverTapAPI.getVariable<Boolean>("var_boolean"))
                }

                Log.i(TAG, builder)
                FileVarsData.printFileVariables(cleverTapAPI!!)
            }

            "13-6" -> {
                Log.i(TAG, "onChildClick: 13-6")

                val builder = buildString {
                    append("Printing variables Values (basic types) :")
                    appendLine()
                    append(cleverTapAPI!!.getVariableValue("var_int"))
                    appendLine()
                    append(cleverTapAPI.getVariableValue("var_long"))
                    appendLine()
                    append(cleverTapAPI.getVariableValue("var_short"))
                    appendLine()
                    append(cleverTapAPI.getVariableValue("var_float"))
                    appendLine()
                    append(cleverTapAPI.getVariableValue("var_double"))
                    appendLine()
                    append(cleverTapAPI.getVariableValue("var_string"))
                    appendLine()
                    append(cleverTapAPI.getVariableValue("var_boolean"))
                    appendLine()
                }
                Log.i(TAG, builder)

                FileVarsData.printFileVariablesValues(cleverTapAPI!!, TAG)
            }

            "13-7" -> {
                cleverTapAPI?.addVariablesChangedCallback(exampleVariables.variablesChangedCallback)
                cleverTapAPI?.onVariablesChangedAndNoDownloadsPending(object :
                    VariablesChangedCallback() {
                    override fun variablesChanged() {
                        Log.i(
                            TAG,
                            "Files downloaded, onVariablesChangedAndNoDownloadsPending - should come after each fetch"
                        )
                        Log.i(TAG, "variablesChanged: reprinting files var data")
                        FileVarsData.printFileVariables(cleverTapAPI)
                    }
                })
            }

            "13-8" -> {
                cleverTapAPI?.removeVariablesChangedCallback(exampleVariables.variablesChangedCallback)
            }

            "13-9" -> {
                cleverTapAPI?.addOneTimeVariablesChangedCallback(exampleVariables.oneTimeVariablesChangedCallback)
                cleverTapAPI?.onceVariablesChangedAndNoDownloadsPending(object :
                    VariablesChangedCallback() {
                    override fun variablesChanged() {
                        Log.i(
                            TAG,
                            "onceVariablesChangedAndNoDownloadsPending - should come only once globally"
                        )
                    }
                })
            }

            "13-10" -> {
                cleverTapAPI?.removeOneTimeVariablesChangedCallback(exampleVariables.oneTimeVariablesChangedCallback)
            }
            "14-0" -> {
                Log.i(TAG, "onChildClick: 14-0")

                FileVarsData.defineFileVars(cleverTapAPI!!, tag = TAG)

                Log.i(
                    TAG,
                    "onChildClick: 14-0: Printing file vars values, they might be null if not yet fetched"
                )
                cleverTapAPI.fetchVariables()
                FileVarsData.printFileVariables(cleverTapAPI, tag = TAG)
            }
            "14-1" -> {

                Log.i(TAG, "onChildClick: 14-1")

                FileVarsData.defineFileVars(cleverTapAPI = cleverTapAPI!!, tag = TAG, fileReadyListenerCount = 3)

                Log.i(
                    TAG,
                    "onChildClick: 14-1: Printing file vars values, they might be null if not yet fetched"
                )
                cleverTapAPI.fetchVariables()
                FileVarsData.printFileVariables(cleverTapAPI, tag = TAG)
            }
            "14-2" -> {
                Log.i(TAG, "onChildClick: 14-2")
                FileVarsData.addGlobalCallbacks(cleverTapAPI!!, tag = TAG)
                FileVarsData.defineFileVars(cleverTapAPI!!, tag = TAG)
                cleverTapAPI.fetchVariables()

            }
            "14-3" -> {
                Log.i(TAG, "onChildClick: 14-3")
                FileVarsData.addGlobalCallbacks(cleverTapAPI!!, tag = TAG, listenerCount = 3)
                FileVarsData.defineFileVars(cleverTapAPI!!, tag = TAG, fileReadyListenerCount = 3)
                cleverTapAPI.fetchVariables()
            }
            "14-4" -> {
                Log.i(TAG, "onChildClick: 14-4")
                FileVarsData.printFileVariables(cleverTapAPI!!, tag = TAG)
            }
            "15-0" -> {
                cleverTapAPI?.locale = "en_IN"
            }
            //"60" -> webViewClickListener?.onWebViewClick()

        }
    }
}

## Example Usage

### Get the Default Singleton Instance

```java
    CleverTapAPI clevertap = CleverTapAPI.getDefaultInstance(getApplicationContext());
```

### Creating Default Instance by specifying custom CleverTap ID

* Add the following to `AndroidManifest.xml` file
```xml
<meta-data
        android:name="CLEVERTAP_USE_CUSTOM_ID"
        android:value="1"/>
```

* Register the ActivityLifecycleCallback with a unique custom CleverTap ID per user

```java
    ActivityLifecycleCallback.register(this,"uniqueCustomClevertapIDForUser");
```

* Or, create a default instance with a unique custom CleverTap ID per users

```java
    CleverTapAPI cleverTapAPI = CleverTapAPI.getDefaultInstance(getApplicationContext(),"uniqueCustomClevertapIDForUser");
```

### Record Events
```java
   clevertap.pushEvent(“Event Name”);
```

* Record an Event with properties  
```java
    HashMap<String, Object> prodViewedAction = new HashMap<String, Object>();
    prodViewedAction.put("Product Name", "Casio Chronograph Watch");
    prodViewedAction.put("Category", "Mens Accessories");
    prodViewedAction.put("Price", 59.99);
    prodViewedAction.put("Date", new java.util.Date());

    clevertap.pushEvent("Product viewed", prodViewedAction);
```

* Record a Charged (purchase made) Event  
```java
    HashMap<String, Object> chargeDetails = new HashMap<String, Object>();
    chargeDetails.put("Amount", 300);
    chargeDetails.put("Payment Mode", "Credit card");
    chargeDetails.put("Charged ID", 24052013);

    HashMap<String, Object> item1 = new HashMap<String, Object>();
    item1.put("Product category", "books");
    item1.put("Book name", "The Millionaire next door");
    item1.put("Quantity", 1);

    HashMap<String, Object> item2 = new HashMap<String, Object>();
    item2.put("Product category", "books");
    item2.put("Book name", "Achieving inner zen");
    item2.put("Quantity", 1);

    HashMap<String, Object> item3 = new HashMap<String, Object>();
    item3.put("Product category", "books");
    item3.put("Book name", "Chuck it, let's do it");
    item3.put("Quantity", 5);

    ArrayList<HashMap<String, Object>> items = new ArrayList<HashMap<String, Object>>();
    items.add(item1);
    items.add(item2);
    items.add(item3);

    clevertap.pushChargedEvent(chargeDetails, items);
```

### Record User Profile properties

```java
    // each of the below fields are optional
    // if set, these populate demographic information in the Dashboard
    HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
    profileUpdate.put("Name", "Jack Montana");                  // String
    profileUpdate.put("Identity", 61026032);                    // String or number
    profileUpdate.put("Email", "jack@gmail.com");               // Email address of the user
    profileUpdate.put("Phone", "+14155551234");                 // Phone (with the country code, starting with +)
    profileUpdate.put("Gender", "M");                           // Can be either M or F
    profileUpdate.put("Employed", "Y");                         // Can be either Y or N
    profileUpdate.put("Education", "Graduate");                 // Can be either Graduate, College or School
    profileUpdate.put("Married", "Y");                          // Can be either Y or N
    profileUpdate.put("DOB", new Date());                       // Date of Birth. Set the Date object to the appropriate value first
    profileUpdate.put("Age", 28);                               // Not required if DOB is set
    profileUpdate.put("Tz", "Asia/Kolkata");                    //an abbreviation such as "PST", a full name such as "America/Los_Angeles",
                                                                //or a custom ID such as "GMT-8:00"
    profileUpdate.put("Photo", "www.foobar.com/image.jpeg");    // URL to the Image

    // optional fields. controls whether the user will be sent email, push etc.
    profileUpdate.put("MSG-email", false);                      // Disable email notifications
    profileUpdate.put("MSG-push", true);                        // Enable push notifications
    profileUpdate.put("MSG-sms", false);                        // Disable SMS notifications

    ArrayList<String> stuff = new ArrayList<String>();
    stuff.add("bag");
    stuff.add("shoes");
    profileUpdate.put("MyStuff", stuff);                        //ArrayList of Strings

    String[] otherStuff = {"Jeans","Perfume"};
    profileUpdate.put("MyStuff", otherStuff);                   //String Array

    clevertap.pushProfile(profileUpdate);
```

### Handling Multiple Device Users

Use `onUserLogin` to maintain multiple distinct user profiles on the same device

```java
    // each of the below fields are optional
    // with the exception of one of Identity, Email, FBID or GPID
    HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
    profileUpdate.put("Name", "Jack Montana");    // String
    profileUpdate.put("Identity", 61026032);      // String or number
    profileUpdate.put("Email", "jack@gmail.com"); // Email address of the user
    profileUpdate.put("Phone", "+14155551234");   // Phone (with the country code, starting with +)
    profileUpdate.put("Gender", "M");             // Can be either M or F
    profileUpdate.put("Employed", "Y");           // Can be either Y or N
    profileUpdate.put("Education", "Graduate");   // Can be either Graduate, College or School
    profileUpdate.put("Married", "Y");            // Can be either Y or N

    profileUpdate.put("DOB", new Date());         // Date of Birth. Set the Date object to the appropriate value first
    profileUpdate.put("Age", 28);                 // Not required if DOB is set
    // optional fields. controls whether the user will be sent email, push etc.
    profileUpdate.put("MSG-email", false);        // Disable email notifications
    profileUpdate.put("MSG-push", true);          // Enable push notifications
    profileUpdate.put("MSG-sms", false);          // Disable SMS notifications

    ArrayList<String> stuff = new ArrayList<String>();
    stuff.add("bag");
    stuff.add("shoes");
    profileUpdate.put("MyStuff", stuff);                        //ArrayList of Strings

    String[] otherStuff = {"Jeans","Perfume"};
    profileUpdate.put("MyStuff", otherStuff);                   //String Array

    cleverTapAPI.onUserLogin(profileUpdate);
```

### Integrate Custom Proxy Domain
The custom proxy domain feature allows to proxy all events raised from the CleverTap SDK through your required domain,
ideal for handling or relaying CleverTap events and Push Impression events with your application server.
Following ways can be used to configure custom proxy domain(s):

#### Using Manifest file
1. Add your CleverTap Account credentials in the Manifest file against the `CLEVERTAP_ACCOUNT_ID` and `CLEVERTAP_TOKEN` keys.
2. Add the **CLEVERTAP_PROXY_DOMAIN** key with the proxy domain value for handling events through the custom proxy domain.
3. Add the **CLEVERTAP_SPIKY_PROXY_DOMAIN** key with proxy domain value for handling push impression events.

```xml
        <meta-data
            android:name="CLEVERTAP_ACCOUNT_ID"
            android:value="YOUR ACCOUNT ID" />
        <meta-data
            android:name="CLEVERTAP_TOKEN"
            android:value="YOUR ACCOUNT TOKEN" />
        <meta-data
            android:name="CLEVERTAP_PROXY_DOMAIN"
            android:value="YOUR PROXY DOMAIN"/>
        <meta-data
            android:name="CLEVERTAP_SPIKY_PROXY_DOMAIN"
            android:value="YOUR SPIKY PROXY DOMAIN"/>
```

#### Using `changeCredentials` API

```java
CleverTapAPI.changeCredentials(
                "YOUR CLEVERTAP ACCOUNT ID",
                "YOUR CLEVERTAP ACCOUNT TOKEN",
                "YOUR PROXY DOMAIN",
                "YOUR SPIKY PROXY DOMAIN"
        );
```

#### Using CleverTap's Additional Instance

```java
        CleverTapInstanceConfig cleverTapInstanceConfig = CleverTapInstanceConfig.createInstance(
                applicationContext,
                "YOUR CLEVERTAP ACCOUNT ID",
                "YOUR CLEVERTAP ACCOUNT TOKEN"
        );
        
        cleverTapInstanceConfig.setProxyDomain("YOUR PROXY DOMAIN");
        cleverTapInstanceConfig.setSpikyProxyDomain("YOUR SPIKY PROXY DOMAIN");

        CleverTapAPI.instanceWithConfig(applicationContext, cleverTapInstanceConfig);
```

### Using App Inbox

#### Adding Inbox Dependencies

Add the following dependencies in your app's `build.gradle`

```groovy
implementation "${ext.appcompat}"//MANDATORY for App Inbox
implementation "${ext.recyclerview}"//MANDATORY for App Inbox
implementation "${ext.viewpager}"//MANDATORY for App Inbox
implementation "${ext.material}"//MANDATORY for App Inbox
implementation "${ext.glide}"//MANDATORY for App Inbox

//Optional ExoPlayer Libraries for Audio/Video Inbox Messages. Audio/Video messages will be dropped without these dependencies
implementation "${ext.exoplayer}"
implementation "${ext.exoplayer_hls}"
implementation "${ext.exoplayer_ui}"
```
#### Initializing the Inbox

Initializing the Inbox will provide a callback to two methods `inboxDidInitialize()` AND `inboxMessagesDidUpdate()`

```java
import com.clevertap.android.sdk.inbox.CTInboxActivity;
import com.clevertap.android.sdk.CTInboxListener;
import com.clevertap.android.sdk.CTInboxStyleConfig;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;

public class MainActivity extends AppCompatActivity implements CTInboxListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
         private CleverTapAPI cleverTapDefaultInstance = CleverTapAPI.getDefaultInstance(this);
         if (cleverTapDefaultInstance != null) {
            //Set the Notification Inbox Listener
            cleverTapDefaultInstance.setCTNotificationInboxListener(this);
            //Initialize the inbox and wait for callbacks on overridden methods
            cleverTapDefaultInstance.initializeInbox();
        }
    }
}
```

#### Configure Styling and Showing the Inbox

Customize the config object and call the Inbox in the `inboxDidInitialize()` method
Call this method on the button click which opens the CleverTap Inbox for your App

```java
@Override
public void inboxDidInitialize(){
    ArrayList<String> tabs = new ArrayList<>();
    tabs.add("Promotions");
    tabs.add("Offers");
    tabs.add("Others");//We support upto 2 tabs only. Additional tabs will be ignored

    CTInboxStyleConfig styleConfig = new CTInboxStyleConfig();
    styleConfig.setFirstTabTitle("First Tab");//By default, name of the first tab is "ALL"
    styleConfig.setTabs(tabs);//Do not use this if you don't want to use tabs
    styleConfig.setTabBackgroundColor("#FF0000");//provide Hex code in string ONLY
    styleConfig.setSelectedTabIndicatorColor("#0000FF");
    styleConfig.setSelectedTabColor("#000000");
    styleConfig.setUnselectedTabColor("#FFFFFF");
    styleConfig.setBackButtonColor("#FF0000");
    styleConfig.setNavBarTitleColor("#FF0000");
    styleConfig.setNavBarTitle("MY INBOX");
    styleConfig.setNavBarColor("#FFFFFF");
    styleConfig.setInboxBackgroundColor("#00FF00");

    cleverTapDefaultInstance.showAppInbox(styleConfig); //Opens activity tith Tabs
    //OR
    cleverTapDefaultInstance.showAppInbox();//Opens Activity with default style config
}
```
### Dismissing App Inbox
Use the following method to dismiss the App Inbox Activity as per your business use case -

```java
cleverTapDefaultInstance.dismissAppInbox();
```

### App Inbox Item and Button Click Callbacks
 
Let's understand the types of buttons first that App Inbox supports:
- URL button (fires the deeplink with the associated URL) 
- Copy to button (Copies the associated text to the clipboard)
- KV button (contains the custom kev-value pair for custom handling)


The Android SDK v4.6.1 and above supports `onInboxItemClicked` callback on the click of an App Inbox item, such as text or media.
From the Android SDK v4.6.8 onwards and below v4.7.0, the `onInboxItemClicked` callback supports the button click besides the item click.

The callback returns `CTInboxMessage` object, `itemIndex` and `buttonIndex` parameters. To use this callback, check that your activity implements the `InboxMessageListener` and overrides the following method:

```java
@Override
public void onInboxItemClicked(CTInboxMessage message, int contentPageIndex, int buttonIndex){
    Log.i(TAG, "InboxItemClicked at" + contentPageIndex + " page-index with button-index:" + buttonIndex);
    //The buttonIndex corresponds to the CTA button clicked (0, 1, or 2). A value of -1 indicates the app inbox body/message clicked.
        
    List<CTInboxMessageContent> inboxMessageContentList = message.getInboxMessageContents();
    //The contentPageIndex corresponds to the page index of the content, which ranges from 0 to the total number of pages for carousel templates. For non-carousel templates, the value is always 0, as they only have one page of content.
    CTInboxMessageContent messageContentObject = inboxMessageContentList.get(contentPageIndex);
    if (buttonIndex != -1) {
        //button is clicked
        try {
            List<CTInboxMessageContent> inboxMessageContentList = message.getInboxMessageContents();
            JSONObject buttonObject = (JSONObject) messageContentObject.getLinks().get(buttonIndex);
            String buttonType = buttonObject.getString("type");
            Log.i(TAG, "type of button clicked: " + buttonType);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    } else {
        //Item is clicked
        Log.i(TAG, "type/template of App Inbox item:" + message.type);
    }
}
```

Android SDK v3.6.1 and above supports an exclusive `onInboxButtonClick` callback on the click of **KV** type of buttons. It returns a Map of Key-Value pairs. To use this, make sure your activity implements the `InboxMessageButtonListener` and override the following method:

```java
 @Override
public void onInboxButtonClick(HashMap<String, String> hashMap) {
    Log.i(TAG, "InboxButtonClick with payload:" + payload);
}
```

### Creating your own App Inbox

You can choose to create your own App Inbox with the help of the following APIs -

```java
//Initialize App Inbox
cleverTapDefaultInstance.initializeInbox();

//Get Inbox Message Count
cleverTapDefaultInstance.getInboxMessageCount();

//Get Inbox Unread Count
cleverTapDefaultInstance.getInboxMessageUnreadCount();

//Get All messages
cleverTapDefaultInstance.getAllInboxMessages();

//Get only Unread messages
cleverTapDefaultInstance.getUnreadInboxMessages();

//Get message object belonging to the given message id only. Message id should be a String
cleverTapDefaultInstance.getInboxMessageForId(messageId);

//Delete message from the Inbox. Message id should be a String
cleverTapDefaultInstance.deleteInboxMessage(messageId);

//Delete message from the Inbox. Message should object of CTInboxMessage
cleverTapDefaultInstance.deleteInboxMessage(message);

//Mark Message as Read. Message id should be a String
cleverTapDefaultInstance.markReadInboxMessage(messageId);

//Mark message as Read. Message should object of CTInboxMessage
cleverTapDefaultInstance.markReadInboxMessage(message);

//Raise Notification Viewed event for Inbox Message. Message id should be a String
cleverTapDefaultInstance.pushInboxNotificationViewedEvent(messageId);

//Raise Notification Clicked event for Inbox Message. Message id should be a String
cleverTapDefaultInstance.pushInboxNotificationClickedEvent(messageId);

//Callback on Inbox Message update/delete/read (any activity)
@Override
public void inboxMessagesDidUpdate() {    }
```

### Additional AndroidManifest.xml Configuration to Support Notifications

#### Push Notifications

If using FCM, inside the `<application></application>` tags, register the following services
```xml
    <service android:name="com.clevertap.android.sdk.pushnotification.fcm.FcmMessageListenerService">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>
```

To set a custom notification icon (only for small icon), add the following meta data entry in your AndroidManifest.xml
```xml
    <meta-data
        android:name="CLEVERTAP_NOTIFICATION_ICON"
        android:value="ic_stat_red_star"/> <!-- name of your file in the drawable directory without the file extension. -->
```
To track the push notification events and deeplinks add the following receiver in your AndroidManifest.xml -
```xml
    <receiver
        android:name="com.clevertap.android.sdk.pushnotification.CTPushNotificationReceiver"
        android:exported="false"
        android:enabled="true">
    </receiver>
```
CleverTap handles closing the notification with Action buttons. You will have to add an additional
CleverTap IntentService to your AndroidManifest.xml and the SDK will do it for you automatically

```xml
    <service
         android:name="com.clevertap.android.sdk.pushnotification.CTNotificationIntentService"
         android:exported="false">
         <intent-filter>
             <action android:name="com.clevertap.PUSH_EVENT"/>
         </intent-filter>
     </service>
``` 

Starting from `core v5.1.0` we have introduced a new feature that allows developers to define a
default notification channel for their app. This feature provides flexibility in handling push
notifications. Please note that this is only supported for clevertap core notifications. Support for
push templates will be released soon.
To specify the default notification channel ID, you can add the following metadata in your app's
manifest file:

```xml
<meta-data
    android:name="CLEVERTAP_DEFAULT_CHANNEL_ID"
    android:value="your_default_channel_id" />
```

By including this metadata, you can define a specific notification channel that CleverTap will use
if the channel provided in push payload is not registered by your app. This ensures that push
notifications are displayed consistently even if the app's notification channels are not set up.

In case the SDK does not find the default channel ID specified in the manifest, it will
automatically fallback to using a default channel called "Miscellaneous". This ensures that push
notifications are still delivered, even if no specific default channel is specified in the manifest.

This enhancement provides developers with greater control over the default notification channel used
by CleverTap for push notifications, ensuring a seamless and customizable user experience.

Starting from `core v5.1.0` below APIs allows you to retrieve a notification bitmap from the
specified `bitmapSrcUrl` with a specified timeout and size. In case the bitmap retrieval fails, you
can choose to fallback to the app icon by setting the `fallbackToAppIcon` parameter. This API
provides more control over the bitmap retrieval process for custom rendering.

```java
@Override
public void onMessageReceived(RemoteMessage message) {
        Bundle messageBundle = mParser.toBundle(message);
        // this method must be called on background thread
        // context, messageBundle must be non null.
        // timeout must be in range of 1 - 20000 millis.
        CleverTapAPI.getNotificationBitmapWithTimeout(
context,messageBundle, "https://www.pushicons.com/icon",
       true, 5000);
        }
```

Below API extends the functionality of the previous one by additionally allowing you to specify the
desired size in bytes for the retrieved bitmap.

```java
@Override
public void onMessageReceived(RemoteMessage message) {
        Bundle messageBundle = mParser.toBundle(message);
        // this method must be called on background thread
        // context, messageBundle must be non null.
        // timeout must be in range of 1 - 20000 millis and size must be greater than 0.
        CleverTapAPI.getNotificationBitmapWithTimeoutAndSize(
context,messageBundle, "https://www.pushicons.com/icon",
       true, 5000,1024);
        }
```

#### Pull Notifications

Starting with v3.4.0, the SDK supports Pull Notifications. Pull Notifications is a capability that
allows you to reach users on devices which suppress notifications via GCM/FCM. To allow your app to
use CleverTap's Pull Notifications via background ping service, add the following fields in your
app's `AndroidManifest.xml`

```xml
<meta-data
    android:name="CLEVERTAP_BACKGROUND_SYNC"
    android:value="1"/>
 ```

#### In-App Notifications

To support in-app notifications, register the following activity in your AndroidManifest.xml
```xml
    <activity
        android:name="com.clevertap.android.sdk.InAppNotificationActivity"
        android:theme="@android:style/Theme.Translucent.NoTitleBar"
        android:configChanges="orientation|keyboardHidden"/>

    <meta-data
        android:name="CLEVERTAP_INAPP_EXCLUDE"
        android:value="YourSplashActivity1, YourSplashActivity2" />
   ```

#### Push primer Android 13 notification runtime permission.

Using Half-Interstitial in-app

Java
```java
JSONObject jsonObject = CTLocalInApp.builder()
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
        .build();
cleverTapAPI.promptPushPrimer(jsonObject);
```

Kotlin
```kotlin
val jsonObject = CTLocalInApp.builder()
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
cleverTapAPI.promptPushPrimer(jsonObject)
```
Using Alert in-app

Java
```java
JSONObject jsonObject = CTLocalInApp.builder()
        .setInAppType(CTLocalInApp.InAppType.ALERT)
        .setTitleText("Get Notified")
        .setMessageText("Enable Notification permission")
        .followDeviceOrientation(true)
        .setPositiveBtnText("Allow")
        .setNegativeBtnText("Cancel")
        .build();
cleverTapAPI.promptPushPrimer(jsonObject);
```
Kotlin
```kotlin
val jsonObject = CTLocalInApp.builder()
    .setInAppType(CTLocalInApp.InAppType.ALERT)
    .setTitleText("Get Notified")
    .setMessageText("Enable Notification permission")
    .followDeviceOrientation(true)
    .setPositiveBtnText("Allow")
    .setNegativeBtnText("Cancel")
    .build()
cleverTapAPI.promptPushPrimer(jsonObject)

```

#### Call Android OS runtime notification dialog without using push primer.

Takes boolean as a parameter. If true and the permission is denied then we fallback to app’s notification settings, if it’s false then we just throw a verbose log saying permission is denied.

Java
```java
cleverTapAPI.promptForPushPermission(true);
```
Kotlin
```kotlin
cleverTapAPI.promptForPushPermission(true)
```

#### Check the status of notification permission whether it's granted or denied.
Returns true if permission is granted, else returns false if permission is denied.

Java
```java
cleverTapAPI.isPushPermissionGranted();
```
Kotlin
```kotlin
cleverTapAPI.isPushPermissionGranted
```

#### Call Android OS runtime notification dialog for HTML in-app campaigns.
Two methods will be available to call hard permission dialog flow from HTML InApp’s as shown below:-
promptPushPermission(boolean shouldShowFallbackSettings) - Use to trigger OS notification dialog.
dismissInAppNotification() - Use to dismiss the current InApp.

Sample code usage
```html
<script>
document.querySelector('#bt_gnp').addEventListener(
	'click',e => {
if(window.CleverTap){
 		CleverTap.promptPushPermission(true); // true/false on whether to show app’s notification page if permission is denied.
	}
})</script>
```

#### CTLocalInApp builder methods description

Builder Methods | Parameters | Description | Required
---:|:---:|:---:|:---
`setInAppType(InAppType)` | CTLocalInApp.InAppType.HALF_INTERSTITIAL OR CTLocalInApp.InAppType.ALERT | Accepts only HALF_INTERSTITIAL & ALERT type to display the type of InApp | Required
`setTitleText(String)` | Text | Sets the title of the local in-app | Required
`setMessageText(String)` | Text | Sets the subtitle of the local in-app | Required
`followDeviceOrientation(boolean)` | true/false | If true then the local InApp is shown for both portrait and landscape. If it sets false then local InApp only displays for portrait mode | Required
`setPositiveBtnText(String)` | Text | Sets the text of the positive button | Required
`setNegativeBtnText(String)` | Text | Sets the text of the negative button | Required
`setFallbackToSettings(boolean)` | true/false | If true and the permission  is denied then we fallback to app’s notification settings, if it’s false then we just throw a verbose log saying permission is denied | Optional
`setBackgroundColor(String)` | Accepts Hex color as String | Sets the background color of the local in-app | Optional
`setBtnBorderColor(String)` | Accepts Hex color as String | Sets the border color of both positive/negative buttons | Optional
`setTitleTextColor(String)` | Accepts Hex color as String | Sets the title color of the local in-app | Optional
`setMessageTextColor(String)` | Accepts Hex color as String | Sets the sub-title color of the local in-app | Optional
`setBtnTextColor(String)` | Accepts Hex color as String | Sets the color of text for both positive/negative buttons | Optional
`setBtnBackgroundColor(String)` | Accepts Hex color as String | Sets the background color for both positive/negative buttons | Optional
`setBtnBorderRadius(String)` | Text | Sets the radius for both positive/negative buttons. Default radius is “2” if not set | Optional


#### Available Callbacks for Push Primer

Based on notification permission grant/deny, we’ll be providing a callback `PushPermissionResponseListener` .Below is a sample implementation to get the permission result
```java
public class MainActivity extends AppCompatActivity implements PushPermissionResponseListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
         private CleverTapAPI cleverTapDefaultInstance = CleverTapAPI.getDefaultInstance(this);
         if (cleverTapDefaultInstance != null) {
            cleverTapDefaultInstance.ct.setPushPermissionResponseListener(this);            
        }
    }
    
    @Override
    public void onPushPermissionResponse(boolean accepted) {
        Log.i(TAG, "onPushPermissionResponse :  InApp---> response() called accepted="+accepted);
    }
}
```

From CT-SDK 4.7.0+, new method onShow() is introduced for InAppNotificationListener . Below is the new method added for when the InApp is shown:-
```java
@Override
public void onShow(CTInAppNotification ctInAppNotification) {

}
```

Please note from Android 13+ devices to render notification we'll have to call createNotificationChannel() after the permission is accepted.

Java
```java
public class MainActivity extends AppCompatActivity implements PushPermissionResponseListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        private CleverTapAPI cleverTapDefaultInstance = CleverTapAPI.getDefaultInstance(this);
        if (cleverTapDefaultInstance != null) {
            cleverTapDefaultInstance.ct.setPushPermissionResponseListener(this);
        }
    }

    @Override
    public void onPushPermissionResponse(boolean accepted) {
        if(accepted){
            //For Android 13+ we need to create notification channel after notification permission is accepted
            CleverTapAPI.createNotificationChannel(getApplicationContext(), "BRTesting", "Testing Channel",
                    "Testing Channel for BR", NotificationManager.IMPORTANCE_HIGH, true);
        }
    }
}
```
Kotlin
```kotlin
class HomeScreenActivity : AppCompatActivity(),PushPermissionResponseListener{
    override fun onCreate(savedInstanceState: Bundle?) {
        var cleverTapDefaultInstance = CleverTapAPI.getDefaultInstance(this)
        cleverTapDefaultInstance?.apply {
            pushPermissionNotificationResponseListener = this@HomeScreenActivity
        }
    }

    override fun onPushPermissionResponse(accepted: Boolean) {
        if(accepted){
            //For Android 13+ we need to create notification channel after notification permission is accepted
            CleverTapAPI.createNotificationChannel(
                this, "BRTesting", "Core",
                "Core notifications", NotificationManager.IMPORTANCE_MAX, true
            )
        }
    }
}
```

#### Google Ad Id changes for Android 13
Please note if using Google Ad Id for apps targeting Android 13+, will have to declare the below permission
```xml
<uses-permission android:name="com.google.android.gms.permission.AD_ID"/>
```

#### Remote Config Variables

From CleverTap SDK v5.0.0 onwards, you can use Remote Config Variables in your app. Please refer to the [Remote Config Variables doc](Variables.md) to read more on how to integrate this to your app.

#### Encryption of PII data 

PII data is stored across the SDK and could be sensitive information. 
From CleverTap SDK v5.2.0 onwards, you can enable encryption for PII data wiz. **Email, Identity, Name and Phone**.  
  
Currently 2 levels of encryption are supported i.e None(0) and Medium(1). Encryption level is None by default.  
**None** - All stored data is in plaintext    
**Medium** - PII data is encrypted completely. 
   
The only way to set encryption level for default instance is from the `AndroidManifest.xml`

* Add the following to `AndroidManifest.xml` file
```xml
<meta-data
    android:name="CLEVERTAP_ENCRYPTION_LEVEL"
    android:value="1" />
```

* Different instances can have different encryption levels. To set an encryption level for an additional instance
```kotlin
val clevertapAdditionalInstanceConfig = CleverTapInstanceConfig.createInstance(
    applicationContext,
    "ADDITIONAL_CLEVERTAP_ACCOUNT_ID",
    "ADDITIONAL_CLEVERTAP_ACCOUNT_TOKEN"
)

clevertapAdditionalInstanceConfig.setEncryptionLevel(CryptHandler.EncryptionLevel.MEDIUM)
val clevertapAdditionalInstance = CleverTapAPI.instanceWithConfig(applicationContext ,clevertapAdditionalInstanceConfig)
```
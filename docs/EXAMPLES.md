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
### Using App Inbox

#### Adding Inbox Dependencies

Add the following dependencies in your app's `build.gradle`

```groovy
implementation "androidx.appcompat:appcompat:1.3.1"//MANDATORY for App Inbox
implementation "androidx.recyclerview:recyclerview:1.2.1"//MANDATORY for App Inbox
implementation "androidx.viewpager:viewpager:1.0.0"//MANDATORY for App Inbox
implementation "com.google.android.material:material:1.4.0"//MANDATORY for App Inbox
implementation "com.github.bumptech.glide:glide:4.12.0"//MANDATORY for App Inbox

//Optional ExoPlayer Libraries for Audio/Video Inbox Messages. Audio/Video messages will be dropped without these dependencies
implementation "com.google.android.exoplayer:exoplayer:2.15.1"
implementation "com.google.android.exoplayer:exoplayer-hls:2.15.1"
implementation "com.google.android.exoplayer:exoplayer-ui:2.15.1"
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
CleverTap handles closing the notification with Action buttons. You will have to add an additional CleverTap IntentService to your AndroidManifest.xml and the SDK will do it for you automatically
```xml
    <service
         android:name="com.clevertap.android.sdk.pushnotification.CTNotificationIntentService"
         android:exported="false">
         <intent-filter>
             <action android:name="com.clevertap.PUSH_EVENT"/>
         </intent-filter>
     </service>
```  

Starting with v3.4.3, the CleverTap SDK supports specifying a custom FCM Sender ID. The SDK will now request for a token with the specified FCM Sender ID if it is present in the `AndroidManifest.xml`. If the FCM Sender ID is not present in the `AndroidManifest.xml` file, then the SDK will request for the token in the default manner which uses the app's `google-services.json` file. To use a custom FCM Sender ID, add the following field in your app's `AndroidManifest.xml`
```xml
    <meta-data
        android:name="FCM_SENDER_ID"
        android:value="id:1234567890"/>
```

#### Push Amplification

Starting with v3.4.0, the SDK supports Push Amplification. Push Amplification is a capability that allows you to reach users on devices which suppress notifications via GCM/FCM. To allow your app to use CleverTap's Push Amplification via background ping service, add the following fields in your app's `AndroidManifest.xml`

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

#### Tracking the Install Referrer

From CleverTap SDK v3.6.4 onwards, just remove the above the Broadcast Receiver if you are using it and add the following gradle dependency to capture UTM details, app install time, referrer click time and other metrics provided by the Google Install Referrer Library.

```groovy
    implementation "com.android.installreferrer:installreferrer:2.2"
```

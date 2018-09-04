## Example Usage

### Get the Default Singleton Instance
```java
    CleverTapAPI clevertap = CleverTapAPI.getDefaultInstance(getApplicationContext());
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
    
### Additional AndroidManifest.xml Configuration to Support Notifications

#### Push Notifications

If using FCM, inside the `<application></application>` tags, register the following services
```xml
    <service
        android:name="com.clevertap.android.sdk.FcmTokenListenerService">
        <intent-filter>
            <action android:name="com.google.firebase.INSTANCE_ID_EVENT"/>
        </intent-filter>
    </service>
    
    <service
        android:name="com.clevertap.android.sdk.FcmMessageListenerService">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT"/>
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
        android:name="com.clevertap.android.sdk.CTPushNotificationReceiver"
        android:exported="false"
        android:enabled="true">
    </receiver>
```
CleverTap handles closing the notification with Action buttons. You will have to add an additional CleverTap IntentService to your AndroidManifest.xml and the SDK will do it for you automatically
```xml
    <service
         android:name="com.clevertap.android.sdk.CTNotificationIntentService"
         android:exported="false">
         <intent-filter>
             <action android:name="com.clevertap.PUSH_EVENT"/>
         </intent-filter>
     </service>
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

Add the following between the `<application></application>` tags. This will enable you to capture UTM parameters for app installs
```xml
    <receiver
        android:name="com.clevertap.android.sdk.InstallReferrerBroadcastReceiver"
        android:exported="true">
            <intent-filter>
                <action android:name="com.android.vending.INSTALL_REFERRER"/>
            </intent-filter>
    </receiver>
```

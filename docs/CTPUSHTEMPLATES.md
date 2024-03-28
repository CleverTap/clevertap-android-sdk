# Push Templates by CleverTap

CleverTap Push Templates SDK helps you engage with your users using fancy push notification templates built specifically to work with [CleverTap](https://www.clevertap.com).

# Table of contents

- [Installation](#installation)
- [Dashboard Usage](#dashboard-usage)
- [Template Types](#template-types)
- [Template Keys](#template-keys)
- [Developer Notes](#developer-notes)
- [Sample App](#sample-app)

# Installation

[(Back to top)](#table-of-contents)

### Out of the box

1. Add the dependencies to the `build.gradle`

```groovy
implementation "com.clevertap.android:push-templates:1.2.3"
implementation "com.clevertap.android:clevertap-android-sdk:6.1.1" // 4.4.0 and above
```

2. Add the following line to your Application class before the `onCreate()`

#### Kotlin
```kotlin
CleverTapAPI.setNotificationHandler(PushTemplateNotificationHandler() as NotificationHandler);
```
#### Java
```java
CleverTapAPI.setNotificationHandler((NotificationHandler)new PushTemplateNotificationHandler());
```

### Custom Handling Push Notifications

Add the following code in your custom FirebaseMessageService class

```java
public class PushTemplateMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        CTFcmMessageHandler()
                .createNotification(getApplicationContext(), remoteMessage);
    }
    @Override
    public void onNewToken(@NonNull final String s) {
        //no-op
    }
}
```

### Migration from v0.0.8 to v1.0.0 and above

Remove the following Receivers and Services from your `AndroidManifest.xml` and follow the steps given above

```xml
<service
    android:name="com.clevertap.pushtemplates.PushTemplateMessagingService">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT"/>
    </intent-filter>
</service>

<service
    android:name="com.clevertap.pushtemplates.PTNotificationIntentService"
    android:exported="false">
        <intent-filter>
            <action android:name="com.clevertap.PT_PUSH_EVENT"/>
        </intent-filter>
</service>

<receiver
android:name="com.clevertap.pushtemplates.PTPushNotificationReceiver"
android:exported="false"
android:enabled="true">
</receiver>

<receiver
android:name="com.clevertap.pushtemplates.PushTemplateReceiver"
android:exported="false"
android:enabled="true">
</receiver>
```

# Dashboard Usage

[(Back to top)](#table-of-contents)

While creating a Push Notification campaign on CleverTap, just follow the steps below -

1. On the "WHAT" section pass the desired values in the "title" and "message" fields (NOTE: We prioritise title and message provided in the key-value pair - as shown in step 2, over these fields)

![Basic](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/basic.png)

2. Click on "Advanced" and then click on "Add pair" to add the [Template Keys](#template-keys)

![KVs](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/kv.png)

3. You can also add the above keys into one JSON object and use the `pt_json` key to fill in the values

![KVs in JSON](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/json.png)

4. Send a test push and schedule!

# Template Types

[(Back to top)](#table-of-contents)

## Basic Template

Basic Template is the basic push notification received on apps.

(Expanded and unexpanded example)

![Basic with color](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/basic%20color.png)


## Auto Carousel Template

Auto carousel is an automatic revolving carousel push notification.

(Expanded and unexpanded example)

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/autocarouselv0.0.3.gif" alt="Auto-Carousel" width="450" height="800"/>


## Manual Carousel Template

This is the manual version of the carousel. The user can navigate to the next image by clicking on the arrows.

(Expanded and unexpanded example)

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/manual.gif" alt="Manual" width="450" height="800"/>

If only one image can be downloaded, this template falls back to the Basic Template

### Filmstrip Variant

The manual carousel has an extra variant called `filmstrip`. This can be used by adding the following key-value -

Template Key | Required | Value
---:|:---:|:---
pt_manual_carousel_type | Optional | `filmstrip`


(Expanded and unexpanded example)

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/filmstrip.gif" alt="Filmstrip" width="450" height="800"/>

## Rating Template

Rating template lets your users give you feedback, this feedback is captured in the event "Rating Submitted" with in the property `wzrk_c2a`.<br/>(Expanded and unexpanded example)<br/>

![Rating](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/rating.gif)

## Product Catalog Template

Product catalog template lets you show case different images of a product (or a product catalog) before the user can decide to click on the "BUY NOW" option which can take them directly to the product via deep links. This template has two variants.

### Vertical View

(Expanded and unexpanded example)

![Product Display](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/productdisplay.gif)

### Linear View

Use the following keys to enable linear view variant of this template.

Template Key | Required | Value
---:|:---:|:---
pt_product_display_linear | Optional | `true`

![Product Display](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/proddisplaylinear.gif)


## Five Icons Template

Five icons template is a sticky push notification with no text, just 5 icons and a close button which can help your users go directly to the functionality of their choice with a button's click.

If at least 3 icons are not retrieved, the library doesn't render any notification. The bifurcation of each CTA is captured in the event Notification Clicked with in the property `wzrk_c2a`.

If user clicks on any notification area except the five & close icons, then by default it will launch an activity intent.

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/fiveicon.png" width="412" height="100">

## Timer Template

This template features a live countdown timer. You can even choose to show different title, message, and background image after the timer expires.

Timer notification is only supported for Android N (7) and above. For OS versions below N, the library falls back to the Basic Template.

![Timer](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/timer.gif)

## Zero Bezel Template

The Zero Bezel template ensures that the background image covers the entire available surface area of the push notification. All the text is overlayed on the image.

The library will fallback to the Basic Template if the image can't be downloaded.

![Zero Bezel](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/zerobezel.gif)

## Input Box Template

The Input Box Template lets you collect any kind of input including feedback from your users. It has four variants.

### With CTAs

The CTA variant of the Input Box Template use action buttons on the notification to collect input from the user.

To set the CTAs use the Advanced Options when setting up the campaign on the dashboard.

![Input_Box_CTAs](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/inputctabasicdismiss.gif)

Template Key | Required | Value
---:|:---:|:---
pt_dismiss_on_click | Optional | Dismisses the notification without opening the app

*Note If `pt_dismiss_on_click` is false we'll have to add the below code to not dismiss the
notification for Android 12 and above

    fun dismissNotification(intent: Intent?, applicationContext: Context){
        intent?.extras?.apply {
            var autoCancel = true
            var notificationId = -1

            getString("actionId")?.let {
                Log.d("ACTION_ID", it)
                autoCancel = getBoolean("autoCancel", true)
                notificationId = getInt("notificationId", -1)
            }
            /**
             * If using InputBox template, add ptDismissOnClick flag to not dismiss notification
             * if pt_dismiss_on_click is false in InputBox template payload. Alternatively if normal
             * notification is raised then we dismiss notification.
             */
            val ptDismissOnClick = intent.extras!!.getString(PTConstants.PT_DISMISS_ON_CLICK,"")

            if (autoCancel && notificationId > -1 && ptDismissOnClick.isNullOrEmpty()) {
                val notifyMgr: NotificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notifyMgr.cancel(notificationId)
            }
        }
    }

### CTAs with Remind Later option

This variant of the Input Box Template is particularly useful if the user wants to be reminded of the notification after sometime. Clicking on the remind later button raises an event to the user profiles, with a custom user property p2 whose value is a future time stamp. You can have a campaign running on the dashboard that will send a reminder notification at the timestamp in the event property.

To set one of the CTAs as a Remind Later button set the action id to `remind` from the dashboard.

Template Key | Required | Value
---:|:---:|:---
pt_event_name | Required | for e.g. `Remind Later`,
pt_event_property_<property_name_1> | Optional | for e.g. `<property_value>`,
pt_event_property_<property_name_2> | Required | future epoch timestamp. For e.g., `$D_1592503813`
pt_dismiss_on_click | Required | Value should be `true`. It dismisses the notification without opening the app and raises a required event to the user profile, needed to send a reminder notification.

![Input_Box_CTA_Remind](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/inputCtaRemind.gif)

### Reply as an Event

This variant raises an event capturing the user's input as an event property. The app is not opened after the user sends the reply.

To use this variant, use the following values for the keys.

Template Key | Required | Value
---:|:---:|:---
pt_input_label | Required | for e.g., `Search`
pt_input_feedback | Required | for e.g., `Thanks for your feedback`
pt_event_name | Required | for e.g. `Searched`,
pt_event_property_<property_name_1> | Optional | for e.g. `<property_value>`,
pt_event_property_<property_name_2> | Required to capture input | fixed value - `pt_input_reply`

![Input_Box_CTA_No_Open](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/inputCtaNoOpen.gif)

### Reply as an Intent

This variant passes the reply to the app as an Intent. The app can then process the reply and take appropriate actions.

To use this variant, use the following values for the keys.

Template Key | Required | Value
---:|:---:|:---
pt_input_label | Required | for e.g., `Search`
pt_input_feedback | Required | for e.g., `Thanks for your feedback`
pt_input_auto_open | Required | fixed value - `true`

<br/> To capture the input, the app can get the `pt_input_reply` key from the Intent extras.

![Input_Box_CTA_With_Open](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/inputCtaWithOpen.gif)

# Template Keys

[(Back to top)](#table-of-contents)

### Basic Template

Basic Template Keys | Required | Description
 ---:|:---:|:---| 
pt_id | Required | Value - `pt_basic`
pt_title | Required | Title
pt_msg | Required | Message
pt_msg_summary | Required | Message line when Notification is expanded
pt_subtitle | Optional  | Subtitle
pt_bg | Required | Background Color in HEX
pt_big_img | Optional | Image
pt_ico | Optional | Large Icon
pt_dl1 | Optional | One Deep Link (minimum)
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_json | Optional | Above keys in JSON format

### Auto Carousel Template

Auto Carousel Template Keys | Required | Description
  ---:|:---:|:--- 
pt_id | Required | Value - `pt_carousel`
pt_title | Required | Title
pt_msg | Required | Message
pt_msg_summary | Optional | Message line when Notification is expanded
pt_subtitle | Optional | Subtitle
pt_dl1 | Required | Deep Link (Max one)
pt_img1 | Required | Image One
pt_img2 | Required | Image Two
pt_img3 | Required | Image Three
pt_img`n` | Optional | Image `N`
pt_bg | Required | Background Color in HEX
pt_ico | Optional | Large Icon
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_json | Optional | Above keys in JSON format

### Manual Carousel Template

Manual Carousel Template Keys | Required | Description
  ---:|:---:|:--- 
pt_id | Required | Value - `pt_manual_carousel`
pt_title | Required | Title
pt_msg | Required | Message
pt_msg_summary | Optional | Message line when Notification is expanded
pt_subtitle | Optional | Subtitle
pt_dl1 | Required | Deep Link One
pt_dl2 | Optional | Deep Link Two
pt_dl`n` | Optional | Deep Link for the nth image
pt_img1 | Required | Image One
pt_img2 | Required | Image Two
pt_img3 | Required | Image Three
pt_img`n` | Optional | Image `N`
pt_bg | Required | Background Color in HEX
pt_ico | Optional | Large Icon
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_json | Optional | Above keys in JSON format
pt_manual_carousel_type | Optional | `filmstrip`

### Rating Template

Rating Template Keys | Required | Description
 ---:|:---:|:--- 
pt_id | Required  | Value - `pt_rating`
pt_title | Required  | Title
pt_msg | Required  | Message
pt_big_img | Optional | Image
pt_msg_summary | Optional | Message line when Notification is expanded
pt_subtitle | Optional | Subtitle
pt_default_dl | Required  | Default Deep Link for Push Notification
pt_dl1 | Required  | Deep Link for first/all star(s)
pt_dl2 | Optional | Deep Link for second star
pt_dl3 | Optional | Deep Link for third star
pt_dl4 | Optional | Deep Link for fourth star
pt_dl5 | Optional | Deep Link for fifth star
pt_bg | Required  | Background Color in HEX
pt_ico | Optional | Large Icon
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_json | Optional | Above keys in JSON format

### Product Catalog Template

Product Catalog Template Keys | Required | Description
 ---:|:---:|:--- 
pt_id | Required  | Value - `pt_product_display`
pt_title | Required  | Title
pt_msg | Required  | Message
pt_subtitle | Optional  | Subtitle
pt_img1 | Required  | Image One
pt_img2 | Required  | Image Two
pt_img3 | Required  | Image Three
pt_bt1 | Required  | Big text for first image
pt_bt2 | Required  | Big text for second image
pt_bt3 | Required  | Big text for third image
pt_st1 | Required  | Small text for first image
pt_st2 | Required  | Small text for second image
pt_st3 | Required  | Small text for third image
pt_dl1 | Required  | Deep Link for first image
pt_dl2 | Required  | Deep Link for second image
pt_dl3 | Required  | Deep Link for third image
pt_price1 | Required  | Price for first image
pt_price2 | Required  | Price for second image
pt_price3 | Required  | Price for third image
pt_bg | Required  | Background Color in HEX
pt_product_display_action | Required  | Action Button Label Text
pt_product_display_linear | Optional  | Linear Layout Template ("true"/"false")
pt_product_display_action_clr | Required  | Action Button Background Color in HEX
pt_title_clr | Optional  | Title Color in HEX
pt_msg_clr | Optional  | Message Color in HEX
pt_small_icon_clr | Optional  | Small Icon Color in HEX
pt_json | Optional  | Above keys in JSON format

### Five Icons Template

Five Icons Template Keys | Required | Description
  ---:|:---:|:--- 
pt_id | Required  | Value - `pt_five_icons`
pt_img1 | Required  | Icon One
pt_img2 | Required  | Icon Two
pt_img3 | Required  | Icon Three
pt_img4 | Optional  | Icon Four
pt_img5 | Optional  | Icon Five
pt_dl1 | Required  | Deep Link for first icon
pt_dl2 | Required  | Deep Link for second icon
pt_dl3 | Required  | Deep Link for third icon
pt_dl4 | Optional  | Deep Link for fourth icon
pt_dl5 | Optional  | Deep Link for fifth icon
pt_bg | Required  | Background Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_json | Optional | Above keys in JSON format

### Timer Template

Timer Template Keys | Required | Description
  ---:|:---:|:--- 
pt_id | Required | Value - `pt_timer`
pt_title | Required | Title
pt_title_alt | Optional | Title to show after timer expires
pt_msg | Required | Message
pt_msg_alt | Optional | Message to show after timer expires
pt_msg_summary | Optional | Message line when Notification is expanded
pt_subtitle | Optional | Subtitle
pt_dl1 | Required | Deep Link
pt_big_img | Optional | Image
pt_big_img_alt | Optional | Image to show when timer expires
pt_bg | Required | Background Color in HEX
pt_chrono_title_clr | Optional | Color for timer text in HEX
pt_timer_threshold | Required | Timer duration in seconds (minimum 10). Will be given higher priority. 
pt_timer_end | Optional | Epoch Timestamp to countdown to (for example, $D_1595871380 or 1595871380). Not needed if pt_timer_threshold is specified.
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_json | Optional | Above keys in JSON format

### Zero Bezel Template

Zero Bezel Template Keys | Required | Description
  ---:|:---:|:--- 
pt_id | Required | Value - `pt_zero_bezel`
pt_title | Required | Title
pt_msg | Required | Message
pt_msg_summary | Optional | Message line when Notification is expanded
pt_subtitle | Optional | Subtitle
pt_big_img | Required | Image
pt_small_view | Optional | Select text-only small view layout (`text_only`)
pt_dl1 | Optional | Deep Link
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_ico | Optional | Large Icon
pt_json | Optional | Above keys in JSON format

### Input Box Template

Input Box Template Keys | Required | Description
  ---:|:---:|:--- 
pt_id | Required | Value - `pt_input`
pt_title | Required | Title
pt_msg | Required | Message
pt_msg_summary | Optional | Message line when Notification is expanded
pt_subtitle | Optional | Subtitle
pt_big_img | Required | Image
pt_big_img_alt | Optional | Image to be shown after feedback is collected
pt_event_name | Optional | Name of Event to be raised
pt_event_property_<property_name_1> | Optional | Value for event property <property_name_1>
pt_event_property_<property_name_2> | Optional | Value for event property <property_name_2>
pt_event_property_<property_name_n> | Optional | Value for event property <property_name_n>
pt_input_label | Required | Label text to be shown on the input
pt_input_auto_open | Optional | Auto open the app after feedback
pt_input_feedback | Required | Feedback
pt_dl1 | Required | Deep Link
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_ico | Optional | Large Icon
pt_dismiss_on_click | Optional | Dismiss notification on click
pt_json | Optional | Above keys in JSON format


### NOTE
* `pt_title` and `pt_msg` in all the templates support HTML elements like bold `<b>`, italics `<i>` and underline `<u>`

# Developer Notes

[(Back to top)](#table-of-contents)

* Using images of 3 MB or lower are recommended for better performance under Android 11.
* A silent notification channel with importance: `HIGH` is created every time on an interaction with the Rating, Manual Carousel, and Product Catalog templates with a silent sound file. This prevents the notification sound from playing when the notification is re-rendered.
* The silent notification channel is deleted whenever the notification is dismissed or clicked.
* For Android 11 and Android 12, please use images which are less than 100kb else notifications will not be rendered as advertised.
* Due to Android 12 trampoline restrictions, the Input Box template with auto open of deeplink feature will fallback to simply raising the event for a reply.

## Image Specifications

Template | Aspect Ratios | File Type
  ---:|:---:|:--- 
Basic | 4:3 or 3:2 or 2:1 | .JPG
Auto Carousel | 3:2 (Android 11 & 12) and 4:3 (Below Android 11) | .JPG
Manual Carousel | 3:2 (Android 11 & 12) and 4:3 (Below Android 11) | .JPG
Manual Carousel-FilmStrip| 1:1 | .JPG
Rating | 4:3 | .JPG
Five Icon | 1:1 | .JPG or .PNG
Zero Bezel | 4:3 or 3:2 or 2:1 | .JPG
Timer | 3:2 (Android 11 & 12) and 4:3 (Below Android 11) | .JPG
Input Box | 4:3 or 2:1 | .JPG
Product Catalog | 1:1 | .JPG

* For Auto and Manual Carousel the image dimensions should not exceed more than 840x560 for Android 11 and Android 12 devices and with 3:2 image aspect ratio
* For images in Basic, Auto/Manual Carousel templates the image dimensions should not exceed more than 400x200 for only Android 13+ devices.
* For images in Five Icons template the image dimensions should not exceed more than 300x300 for only Android 13+ devices.
* For Product Catalog image aspect ratio should be 1:1 and image size should be less than 80kb for Android 11 and Android 12 devices
* For Zero Bezel it's recommended that if your image has any text it should be present in the middle of the image for Android 12+ devices. 
* For Android 12+ devices it's recommended that if your image has any text it should be present in the middle of the image.

## Android 12 Trampoline restrictions

With Android 12, the Rating and Product Display template push notifications do not get dismissed once the deeplink is opened.

To handle this, you'll have to add the following code to the `onActivityResumed` or `onNewIntent` of your app

#### Kotlin
```kotlin
        val payload = activity.intent?.extras
        if (payload?.containsKey("pt_id") == true && payload["pt_id"] =="pt_rating")
        {
            val nm = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(payload["notificationId"] as Int)
        }
        if (payload?.containsKey("pt_id") == true && payload["pt_id"] =="pt_product_display")
        {
            val nm = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(payload["notificationId"] as Int)
        }
```

#### JAVA
```java
    Bundle payload = activity.getIntent().getExtras();
    if (payload.containsKey("pt_id")&& payload.getString("pt_id").equals("pt_rating"))
    {
        NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE); 
        nm.cancel(payload.getInt("notificationId"));
    }
    if (payload.containsKey("pt_id")&& payload.getString("pt_id").equals("pt_product_display"))
    {
        NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE); 
        nm.cancel(payload.getInt("notificationId"));
    }
```

## Android 12 Screenshots

You can see the renditions of all the Push Templates on an Android 12 devices [here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTPUSHTEMPLATESANDROID12.md)

# Sample App

[(Back to top)](#table-of-contents)

Check out the [Sample app](sample)
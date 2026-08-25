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
implementation "${ext.push_templates}"
implementation "${ext.clevertap_android_sdk}" // 4.4.0 and above
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

## Custom Rating Template

The Custom Rating template (`pt_custom_rating`) is a separate template that builds on the classic
Rating template with campaign-configurable options:

* **2 to 5 rating positions** instead of a fixed five, via `pt_rating_count`.
* **Your own icons** per position, each with its own selected and unselected artwork.
* **A submit button**, so tapping a position only highlights it. Nothing is reported and no deep link
  opens until the user confirms, which lets them change their mind first.
* **Per-position deep links** that override the submit destination, so a low rating can open a
  feedback form while a high one opens the store listing.
* **Icons or text**, via `pt_rating_style`. Text positions render as chips and take emoji as readily
  as words, which is what fits best once there are four or five of them.
* **A confirmation message** that replaces the row after submitting, instead of the notification
  disappearing.

The classic `pt_rating` template is unchanged. Existing Rating campaigns keep rendering and behaving
exactly as before, and the two templates share no keys beyond the standard content ones.

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

Five icons template is a push notification with no text, just 5 icons which can help your users go directly to the functionality of their choice with a button's click.

If at least 3 icons are not retrieved, the library doesn't render any notification. The bifurcation of each CTA is captured in the event Notification Clicked with in the property `wzrk_c2a`.

If user clicks on any notification area except the five icons, then by default it will launch an activity intent.

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/fiveicon.png" width="412" height="100">

## Timer Template

This template features a live countdown timer. You can even choose to show different title, message, and background image after the timer expires.

Timer notification is only supported for Android O (8) and above. For OS versions below O, the library falls back to the Basic Template.

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
pt_event_property_<property_name_2> | Required | future epoch timestamp. For e.g., `\$D_1592503813`
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
pt_bg | Optional | Background Color in HEX
pt_big_img | Optional | Image
pt_big_img_alt_text | Optional | Alt Text for Image
pt_gif | Optional | GIF
pt_gif_frames | Optional | Number of frames to extract from the GIF
pt_scale_type | Optional | ScaleType for the big image in the ImageView ("center_crop"/"fit_center")
pt_media_radius | Optional | Corner radius for the big image in dp, `0`-`32`. Defaults to `0` (square corners). Forces `pt_scale_type` to `fit_center` so the rounded corners stay visible. On this template the media is rendered as a banner — see [Layout budget](#layout-budget)
pt_media_border_clr | Optional | Border color for the big image in HEX. Forces `pt_scale_type` to `fit_center` so the border stays visible
pt_media_border_width | Optional | Border width for the big image in dp, `0`-`16`. Defaults to `1`. Only used when `pt_media_border_clr` is set
pt_ico | Optional | Large Icon
pt_dl1 | Optional | One Deep Link (minimum)
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_sticky | Optional | Should the notification be sticky? ("true"/"false")
pt_dismiss | Optional | Auto dismiss the notification after a set time (value in seconds)
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
pt_img1_alt_text | Optional | Alt Text for Image One
pt_img2 | Required | Image Two
pt_img2_alt_text | Optional | Alt Text for Image Two
pt_img3 | Required | Image Three
pt_img3_alt_text | Optional | Alt Text for Image Three
pt_img`n` | Optional | Image `N`
pt_img`n`_alt_text | Optional | Alt Text for Image `N`
pt_scale_type | Optional | ScaleType for the big image in the ImageView ("center_crop"/"fit_center")
pt_media_radius | Optional | Corner radius for the big image in dp, `0`-`32`. Defaults to `0` (square corners). Forces `pt_scale_type` to `fit_center` so the rounded corners stay visible. On this template the media is rendered as a banner — see [Layout budget](#layout-budget)
pt_media_border_clr | Optional | Border color for the big image in HEX. Forces `pt_scale_type` to `fit_center` so the border stays visible
pt_media_border_width | Optional | Border width for the big image in dp, `0`-`16`. Defaults to `1`. Only used when `pt_media_border_clr` is set
pt_bg | Optional | Background Color in HEX
pt_ico | Optional | Large Icon
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_sticky | Optional | Should the notification be sticky? ("true"/"false")
pt_dismiss | Optional | Auto dismiss the notification after a set time (value in seconds)
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
pt_img1_alt_text | Optional | Alt Text for Image One
pt_img2 | Required | Image Two
pt_img2_alt_text | Optional | Alt Text for Image Two
pt_img3 | Required | Image Three
pt_img3_alt_text | Optional | Alt Text for Image Three
pt_img`n` | Optional | Image `N`
pt_img`n`_alt_text | Optional | Alt Text for Image `N`
pt_scale_type | Optional | ScaleType for the big image in the ImageView ("center_crop"/"fit_center")
pt_media_radius | Optional | Corner radius for the big image in dp, `0`-`32`. Defaults to `0` (square corners). Forces `pt_scale_type` to `fit_center` so the rounded corners stay visible. On this template the media is rendered as a banner — see [Layout budget](#layout-budget)
pt_media_border_clr | Optional | Border color for the big image in HEX. Forces `pt_scale_type` to `fit_center` so the border stays visible
pt_media_border_width | Optional | Border width for the big image in dp, `0`-`16`. Defaults to `1`. Only used when `pt_media_border_clr` is set
pt_bg | Optional | Background Color in HEX
pt_ico | Optional | Large Icon
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_sticky | Optional | Should the notification be sticky? ("true"/"false")
pt_dismiss | Optional | Auto dismiss the notification after a set time (value in seconds)
pt_json | Optional | Above keys in JSON format
pt_manual_carousel_type | Optional | `filmstrip`

### Rating Template

Rating Template Keys | Required | Description
 ---:|:---:|:--- 
pt_id | Required  | Value - `pt_rating`
pt_title | Required  | Title
pt_msg | Required  | Message
pt_big_img | Optional | Image
pt_gif | Optional | GIF
pt_gif_frames | Optional | Number of frames to extract from the GIF
pt_big_img_alt_text | Optional | Alt Text for Image
pt_scale_type | Optional | ScaleType for the big image in the ImageView ("center_crop"/"fit_center")
pt_media_radius | Optional | Corner radius for the big image in dp, `0`-`32`. Defaults to `0` (square corners). Forces `pt_scale_type` to `fit_center` so the rounded corners stay visible. On this template the media is rendered as a banner — see [Layout budget](#layout-budget)
pt_media_border_clr | Optional | Border color for the big image in HEX. Forces `pt_scale_type` to `fit_center` so the border stays visible
pt_media_border_width | Optional | Border width for the big image in dp, `0`-`16`. Defaults to `1`. Only used when `pt_media_border_clr` is set
pt_msg_summary | Optional | Message line when Notification is expanded
pt_subtitle | Optional | Subtitle
pt_default_dl | Required  | Default Deep Link for Push Notification
pt_dl1 | Required  | Deep Link for first/all star(s)
pt_dl2 | Optional | Deep Link for second star
pt_dl3 | Optional | Deep Link for third star
pt_dl4 | Optional | Deep Link for fourth star
pt_dl5 | Optional | Deep Link for fifth star
pt_bg | Optional  | Background Color in HEX
pt_ico | Optional | Large Icon
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_sticky | Optional | Should the notification be sticky? ("true"/"false")
pt_dismiss | Optional | Auto dismiss the notification after a set time (value in seconds)
pt_json | Optional | Above keys in JSON format

### Custom Rating Template

Custom Rating Template Keys | Required | Description
 ---:|:---:|:---
pt_id | Required | Value - `pt_custom_rating`
pt_title | Required | Title
pt_msg | Required | Message
pt_rating_style | Required | How the positions are drawn — `icon` or `text`
pt_rating_count | Required | Number of rating positions, `2`-`5`. A larger value is clamped to `5`; a smaller one degrades the template to a standard notification
pt_rating_icon`n` | Required (icon style) | Unselected artwork for position `n` (`1`-`pt_rating_count`). PNG or WebP with transparency, 1:1, recommended 240x240 px
pt_rating_icon`n`_sel | Optional | Selected artwork for position `n`. Falls back to `pt_rating_icon`n`` when omitted
pt_rating_label`n` | Required (text style) | Label for position `n`. Emoji count as one character. What fits depends on how many positions there are — roughly 15 characters at 2 positions, 10 at 3, 7 at 4 and 5 at 5; anything longer is ellipsised on the device
pt_rating_icon_clr | Optional | Icon style: tint applied to unselected positions in HEX, which only affects monochrome artwork. Text style: the unselected chip's fill
pt_rating_icon_sel_clr | Optional | The same for the selected position — icon tint, or the selected chip's fill
pt_rating_label_clr | Optional | Text style only: unselected label color in HEX. Defaults to the notification's message color
pt_rating_label_sel_clr | Optional | Text style only: selected label color in HEX. Defaults to the notification's title color
pt_rating_cta_label | Required | Submit button text, up to 25 characters
pt_rating_cta_dl | Required | Default destination opened on submit
pt_rating_cta_bg_clr | Optional | Submit button fill color in HEX
pt_rating_cta_border_clr | Optional | Submit button border color in HEX
pt_rating_cta_txt_clr | Optional | Submit button text color in HEX
pt_rating_cta_radius | Optional | Submit button corner radius in dp, `0`-`32`. Defaults to `8`
pt_rating_confirm_msg | Optional | Confirmation message, up to 60 characters. When set, submitting replaces the rating row and the submit button with this message and leaves the notification in the tray instead of dismissing it
pt_default_dl | Required | Destination opened when the notification body is tapped. Never raises `Rating Submitted`
pt_dl1 | Optional | Overrides the submit destination when position 1 is the selected one
pt_dl2 | Optional | Overrides the submit destination when position 2 is the selected one
pt_dl3 | Optional | Overrides the submit destination when position 3 is the selected one
pt_dl4 | Optional | Overrides the submit destination when position 4 is the selected one
pt_dl5 | Optional | Overrides the submit destination when position 5 is the selected one
pt_big_img | Optional | Image
pt_big_img_alt_text | Optional | Alt Text for Image
pt_gif | Optional | GIF
pt_gif_frames | Optional | Number of frames to extract from the GIF
pt_scale_type | Optional | ScaleType for the big image in the ImageView ("center_crop"/"fit_center")
pt_media_radius | Optional | Corner radius for the big image in dp, `0`-`32`. Defaults to `0` (square corners). Forces `pt_scale_type` to `fit_center` so the rounded corners stay visible. On this template the media is rendered as a banner — see [Layout budget](#layout-budget)
pt_media_border_clr | Optional | Border color for the big image in HEX. Forces `pt_scale_type` to `fit_center` so the border stays visible
pt_media_border_width | Optional | Border width for the big image in dp, `0`-`16`. Defaults to `1`. Only used when `pt_media_border_clr` is set
pt_msg_summary | Optional | Message line when Notification is expanded
pt_subtitle | Optional | Subtitle
pt_bg | Optional | Background Color in HEX
pt_ico | Optional | Large Icon
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_sticky | Optional | Should the notification be sticky? ("true"/"false")
pt_dismiss | Optional | Auto dismiss the notification after a set time (value in seconds)
pt_json | Optional | Above keys in JSON format

#### How a rating is reported

Tapping a position only re-renders the notification with that position highlighted — it raises no
event and opens nothing. Tapping **Submit** raises `Rating Submitted` exactly once and then opens
`pt_dl{n}` for that position if set, otherwise `pt_rating_cta_dl`. Submitting without a selection
does nothing, and a repeat tap after submitting raises nothing further.

The event carries the campaign attribution every template sends, plus:

Property | Type | Value
 ---:|:---:|:---
wzrk_c2a | String | `rating_{n}` for the selected position, as on the classic template
rating_value | Int | The selected position, `1`-`pt_rating_count`
rating_scale | Int | `pt_rating_count`, so a value can be read against the scale it came from
rating_style | String | `icon` or `text`

`rating_value` is always the position itself, with no sentiment mapping — at a scale of 2 it is 1 or
2, and turning that into "positive" or "negative" is a segmentation choice.

Color keys honour the dark mode suffix, so `pt_rating_cta_bg_clr_dark` is used when the device is in
dark mode. See [Dark Mode](#dark-mode).

#### Fallback behaviour

The template degrades to a standard notification rather than rendering a broken layout when
`pt_rating_style` is missing or unsupported, when `pt_rating_count` is missing, when fewer than two
positions have usable artwork or labels, or when the submit button is missing its label or
destination. A single position whose artwork fails to download is substituted with the built-in star
instead, and a text row missing any one of its labels falls back to the built-in stars for the whole
row.

#### Layout budget

An expanded notification is documented as having as little as 252dp of height, and the content block,
the rating row and the submit button already claim most of it. The expanded image or GIF is therefore
rendered as a **banner** on this template rather than at its own aspect ratio, so the submit button is
never pushed below the fold. Design the artwork for a wide, short crop.

Width is just as tight: the row shares roughly 296dp on a common phone, so the text style is at its
best with emoji or single short words. Labels that do not fit are ellipsised rather than wrapped —
a notification layout cannot flow items onto a second line.

Older Push Templates SDK versions that do not know `pt_custom_rating` render a standard notification.

### Product Catalog Template

Product Catalog Template Keys | Required | Description
 ---:|:---:|:--- 
pt_id | Required  | Value - `pt_product_display`
pt_title | Required  | Title
pt_msg | Required  | Message
pt_subtitle | Optional  | Subtitle
pt_img1 | Required  | Image One
pt_img1_alt_text | Optional | Alt Text for Image One
pt_img2 | Required  | Image Two
pt_img2_alt_text | Optional | Alt Text for Image Two
pt_img3 | Required  | Image Three
pt_img3_alt_text | Optional | Alt Text for Image Three
pt_scale_type | Optional | ScaleType for the big image in the ImageView ("center_crop"/"fit_center")
pt_media_radius | Optional | Corner radius for the big image in dp, `0`-`32`. Defaults to `0` (square corners). Forces `pt_scale_type` to `fit_center` so the rounded corners stay visible. On this template the media is rendered as a banner — see [Layout budget](#layout-budget)
pt_media_border_clr | Optional | Border color for the big image in HEX. Forces `pt_scale_type` to `fit_center` so the border stays visible
pt_media_border_width | Optional | Border width for the big image in dp, `0`-`16`. Defaults to `1`. Only used when `pt_media_border_clr` is set
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
pt_bg | Optional  | Background Color in HEX
pt_product_display_action | Required  | Action Button Label Text
pt_product_display_linear | Optional  | Linear Layout Template ("true"/"false")
pt_product_display_action_clr | Required  | Action Button Background Color in HEX
pt_title_clr | Optional  | Title Color in HEX
pt_msg_clr | Optional  | Message Color in HEX
pt_small_icon_clr | Optional  | Small Icon Color in HEX
pt_sticky | Optional | Should the notification be sticky? ("true"/"false")
pt_dismiss | Optional | Auto dismiss the notification after a set time (value in seconds)
pt_json | Optional  | Above keys in JSON format

### Five Icons Template

Five Icons Template Keys | Required | Description
  ---:|:---:|:--- 
pt_id | Required  | Value - `pt_five_icons`
pt_img1 | Required  | Icon One
pt_img1_alt_text | Optional | Alt Text for Icon One
pt_img2 | Required  | Icon Two
pt_img2_alt_text | Optional | Alt Text for Icon Two
pt_img3 | Required  | Icon Three
pt_img3_alt_text | Optional | Alt Text for Icon Three
pt_img4 | Optional  | Icon Four
pt_img4_alt_text | Optional | Alt Text for Icon Four
pt_img5 | Optional  | Icon Five
pt_img5_alt_text | Optional | Alt Text for Icon Five
pt_dl1 | Required  | Deep Link for first icon
pt_dl2 | Required  | Deep Link for second icon
pt_dl3 | Required  | Deep Link for third icon
pt_dl4 | Optional  | Deep Link for fourth icon
pt_dl5 | Optional  | Deep Link for fifth icon
pt_bg | Optional  | Background Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_media_radius | Optional | Corner radius applied to each icon in dp, `0`-`32`. Defaults to `0` (square corners)
pt_media_border_clr | Optional | Border color applied to each icon in HEX
pt_media_border_width | Optional | Border width applied to each icon in dp, `0`-`16`. Defaults to `1`. Only used when `pt_media_border_clr` is set
pt_sticky | Optional | Should the notification be sticky? ("true"/"false")
pt_dismiss | Optional | Auto dismiss the notification after a set time (value in seconds)
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
pt_msg_summary_alt | Optional | Message summary to show after timer expires
pt_subtitle | Optional | Subtitle
pt_dl1 | Required | Deep Link
pt_big_img | Optional | Image
pt_gif | Optional | GIF
pt_gif_frames | Optional | Number of frames to extract from the GIF
pt_big_img_alt_text | Optional | Alt Text for Image
pt_scale_type | Optional | ScaleType for the big image in the ImageView ("center_crop"/"fit_center")
pt_media_radius | Optional | Corner radius for the big image in dp, `0`-`32`. Defaults to `0` (square corners). Forces `pt_scale_type` to `fit_center` so the rounded corners stay visible. On this template the media is rendered as a banner — see [Layout budget](#layout-budget)
pt_media_border_clr | Optional | Border color for the big image in HEX. Forces `pt_scale_type` to `fit_center` so the border stays visible
pt_media_border_width | Optional | Border width for the big image in dp, `0`-`16`. Defaults to `1`. Only used when `pt_media_border_clr` is set
pt_big_img_alt | Optional | Image to show when timer expires
pt_gif_alt | Optional | GIF to show when timer expires
pt_gif_frames_alt | Optional | Number of frames to extract from the alternate GIF
pt_big_img_alt_alt_text | Optional | Alt Text for Image to show when timer expires
pt_bg | Optional | Background Color in HEX
pt_chrono_title_clr | Optional | Color for timer text in HEX. Falls back to `pt_title_clr` if absent.
pt_chrono_style | Optional | Chronometer box styling mode. One of `"solid"`, `"gradient_linear"`, or `"gradient_radial"`. If absent, no background is drawn on the chronometer box.
pt_chrono_bg_clr | Optional | Chronometer box background fill color in HEX. Required when `pt_chrono_style` is `"solid"`; if missing, no background is drawn.
pt_chrono_grad_clr1 | Optional | Gradient start color in HEX. Required (along with `pt_chrono_grad_clr2`) when `pt_chrono_style` is `"gradient_linear"` or `"gradient_radial"`.
pt_chrono_grad_clr2 | Optional | Gradient end color in HEX. Required (along with `pt_chrono_grad_clr1`) for gradient styles.
pt_chrono_grad_dir | Optional | Gradient angle in degrees (String). Applies to linear gradients only. Defaults to `"90"` (top → bottom).
pt_chrono_border_clr | Optional | Chronometer box border color in HEX. Works with all styles. No border is drawn if absent.
pt_chrono_border_width | Optional | Chronometer box border width in dp. No border is drawn if absent, even when `pt_chrono_border_clr` is set.
pt_chrono_border_radius | Optional | Chronometer box corner radius in dp. Defaults to `"6"`.
pt_timer_threshold | Required | Timer duration in seconds (minimum 10). Will be given higher priority. 
pt_timer_end | Optional | Epoch Timestamp to countdown to (for example, \$D_1595871380 or 1595871380). Not needed if pt_timer_threshold is specified.
pt_render_terminal | Optional | Should terminal notification be rendered? ("true"/"false")
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_sticky | Optional | Should the notification be sticky? ("true"/"false")
pt_dismiss | Optional | Auto dismiss the notification after a set time (value in seconds)
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
pt_gif | Optional | GIF
pt_gif_frames | Optional | Number of frames to extract from the GIF
pt_big_img_alt_text | Optional | Alt Text for Image
pt_scale_type | Optional | ScaleType for the big image in the ImageView ("center_crop"/"fit_center")
pt_media_radius | Optional | Corner radius for the big image in dp, `0`-`32`. Defaults to `0` (square corners). Forces `pt_scale_type` to `fit_center` so the rounded corners stay visible. On this template the media is rendered as a banner — see [Layout budget](#layout-budget)
pt_media_border_clr | Optional | Border color for the big image in HEX. Forces `pt_scale_type` to `fit_center` so the border stays visible
pt_media_border_width | Optional | Border width for the big image in dp, `0`-`16`. Defaults to `1`. Only used when `pt_media_border_clr` is set
pt_big_img_collapsed | Optional | Image for the collapsed view
pt_gif_collapsed | Optional | GIF for the collapsed view
pt_gif_frames_collapsed | Optional | Number of frames to extract from the GIF for the collapsed view
pt_big_img_collapsed_alt_text | Optional | Alt Text for Image in the collapsed view
pt_scale_type_collapsed | Optional | ScaleType for the image in the collapsed View ("center_crop"/"fit_center")
pt_small_view | Optional | Select text-only small view layout (`text_only`)
pt_dl1 | Optional | Deep Link
pt_title_clr | Optional | Title Color in HEX
pt_msg_clr | Optional | Message Color in HEX
pt_small_icon_clr | Optional | Small Icon Color in HEX
pt_ico | Optional | Large Icon
pt_sticky | Optional | Should the notification be sticky? ("true"/"false")
pt_dismiss | Optional | Auto dismiss the notification after a set time (value in seconds)
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
pt_big_img_alt_text | Optional | Alt Text for Image
pt_big_img_alt | Optional | Image to be shown after feedback is collected
pt_big_img_alt_alt_text | Optional | Alt Text for Image to be shown after feedback is collected
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
pt_sticky | Optional | Should the notification be sticky? ("true"/"false")
pt_dismiss | Optional | Auto dismiss the notification after a set time (value in seconds)
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

The following are the image specifications and guidelines for the Push Templates:

| Template            | Aspect Ratios (Approx.)                                                                                                                                       | Maximum File Size (OS version 12 and above) |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|
| **Standard**        | - OS version 12 and above: **3:2**<br>- OS version 11 and below: **5:3**                                                                                      |                                             |
| **Basic**           | - OS version 12 and above: **3:2**<br>- OS version 11 and below: **5:3**                                                                                      | 500 KB                                      |
| **Auto Carousel**   | - OS version 12 and above: **3:2**<br>- OS version 11 and below: **5:3**                                                                                      | 86 KB                                       |
| **Manual Carousel** | - OS version 12 and above: **3:2**<br>- OS version 11 and below: **5:3**                                                                                      | 86 KB                                       |
| **Five Icon**       | - OS version 12 and above: **1:1**<br>- OS version 11 and below: **1:1**                                                                                      | 50 KB                                       |
| **Text over Image** | - OS version 12 and above: **1:1**<br>- OS version 11 and below: **2:1**                                                                                      | 500 KB                                      |
| **Timer**           | - OS version 12 and above: **3:2**<br>- OS version 11 and below: **5:3**                                                                                      | 326 KB                                      |

## Image Guidelines

Ensure images for the following templates meet the specified size guidelines:

| Template Name              | Recommended Resolution |
|:---------------------------|:-----------------------|
| Auto Carousel Template     | 400 x 300 px           |
| Manual Carousel Template   | 240 x 180 px           |
| Five Icon Template         | 300 x 300 px           |
| Product Catalogue Template | 225 x 225 px           |

- For Text over Image Template, ensure the text is center-aligned within the image for devices running OS version 12 and above.

## GIF Guidelines

To use GIFs in the Push Templates, ensure you are using Push Templates SDK version `2.2.0` and above. Also make sure gifs meet the specified size guidelines. You can use [this tool](https://ezgif.com/resize) to resize the GIF.

| GIF Size                   | Recommended Number of Frames |
|:---------------------------|:-----------------------------|
| 180 x 120 px               | 15                           |
| 240 x 160 px               | 10                           |
| 300 x 200 px               | 6                            |

## Dark Mode
To use the Dark mode feature, ensure you are using Push Templates SDK version 2.1.0 and above.
Push templates automatically adapt to the device's theme settings, rendering notifications in dark or light mode. 
Templates support **custom color definitions** that adapt to both themes for visual consistency. Refer [here](https://developer.clevertap.com/docs/android-push-templates#dark-mode) for more details 

##  Image Scaling
To use the Image scaling feature, ensure you are using Push Templates SDK version 2.1.0 and above.
Android supports various image scaling options to control how images appear in push notifications. CleverTap optimizes image rendering to maintain visual consistency across devices while leveraging Android native scaling behavior. 
To handle scaling in a Push template, you must add the key `pt_scale_type` key and the value is set as `fit_center` or `center_crop` based on the requirement. Refer [here](https://developer.clevertap.com/docs/android-push-templates#image-scaling) for more details

## Media Border and Rounded Corners

Templates that render an expanded image or GIF support rounded corners and a border on that media.
Add any of the following keys to the payload:

Key | Description
:---|:---
`pt_media_radius` | Corner radius in **dp**, `0`-`32`. Defaults to `0` (square corners)
`pt_media_border_clr` | Border color in HEX, for example `#FF5722`. When omitted, no border is drawn
`pt_media_border_width` | Border width in **dp**, `0`-`16`. Defaults to `1`. Only used when `pt_media_border_clr` is set

### Why dp

Both values are in dp, like every other border and radius key in the payload — `pt_btn_border_width`,
`pt_btn_border_radius`, `pt_chrono_border_width`, `pt_chrono_border_radius` and
`pt_rating_cta_radius`. One value therefore means one visible result.

The SDK bakes the border into the bitmap, so it converts each dp value into that bitmap's pixel space
using the width the media is laid out at on the device. A `16` looks the same whether the campaign
image is 240x180 or 1200x800, and a `4` border is the same thickness on a 3:2 image as on a square
one.

### Notes

* `pt_media_border_clr` supports the dark mode suffix, so `pt_media_border_clr_dark` is used when the
  device is in dark mode. See [Dark Mode](#dark-mode).
* Because the corners are baked into the bitmap, a `center_crop` image view would crop them away.
  Whenever either key is active the SDK renders the image with `fit_center` and ignores
  `pt_scale_type`.
* Values outside the supported range are clamped to the nearest valid value (for example a negative
  radius becomes `0`). On a small image the radius is additionally capped at half the shortest side
  and the border at a quarter of it, past which the shape degenerates. Non-numeric values and
  unparseable colors are ignored as though the key were absent.
* The keys apply to GIFs as well as static images.
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
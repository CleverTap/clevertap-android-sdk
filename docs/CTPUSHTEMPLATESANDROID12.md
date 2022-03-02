# Template Screenshots on Android 12

## Basic Template

Basic Template is the basic push notification received on apps.

(Expanded and unexpanded example)

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/BasicAndroid12.gif" alt="Basic" width="450" height="800"/>


## Auto Carousel Template

Auto carousel is an automatic revolving carousel push notification.

(Expanded and unexpanded example)

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/AutocarouselAndroid12.gif" alt="Auto-Carousel" width="450" height="800"/>


## Manual Carousel Template

This is the manual version of the carousel. The user can navigate to the next image by clicking on the arrows.

(Expanded and unexpanded example)

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/ManualAndroid12.gif" alt="Manual-Carousel" width="450" height="800"/>

If only one image can be downloaded, this template falls back to the Basic Template

### Filmstrip Variant

The manual carousel has an extra variant called `filmstrip`. This can be used by adding the following key-value -

(Expanded and unexpanded example)

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/FilmstripAndroid12.gif" alt="Filmstrip-Carousel" width="450" height="800"/>

## Rating Template

Rating template lets your users give you feedback, this feedback is captured in the event "Rating Submitted" with in the property `wzrk_c2a`.<br/>(Expanded and unexpanded example)<br/>

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/RatingAndroid12.gif" alt="RatingAndroid12" width="450" height="800"/>

## Product Catalog Template

Product catalog template lets you show case different images of a product (or a product catalog) before the user can decide to click on the "BUY NOW" option which can take them directly to the product via deep links. This template has two variants.

### Vertical View

(Expanded and unexpanded example)

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/ProductDisplayAndroid12.gif" alt="ProductDisplayAndroid12" width="450" height="800"/>

### Linear View

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/ProdDisplayLinearAndroid12.gif" alt="ProdDisplayLinearAndroid12" width="450" height="800"/>

## Five Icons Template

Five icons template is a sticky push notification with no text, just 5 icons and a close button which can help your users go directly to the functionality of their choice with a button's click.

If at least 3 icons are not retrieved, the library doesn't render any notification. The bifurcation of each CTA is captured in the event Notification Clicked with in the property `wzrk_c2a`.

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/FiveIconAndroid12.gif" alt="FiveIconAndroid12" width="450" height="800"/>

## Timer Template

This template features a live countdown timer. You can even choose to show different title, message, and background image after the timer expires.

Timer notification is only supported for Android N (7) and above. For OS versions below N, the library falls back to the Basic Template.

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/TimerAndroid12.gif" alt="TimerAndroid12" width="450" height="800"/>

## Zero Bezel Template

The Zero Bezel template ensures that the background image covers the entire available surface area of the push notification. All the text is overlayed on the image.

The library will fallback to the Basic Template if the image can't be downloaded.

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/ZeroBezelAndroid12.gif" alt="ZeroBezelAndroid12" width="450" height="800"/>

## Input Box Template

The Input Box Template lets you collect any kind of input including feedback from your users. It has four variants.

### With CTAs

The CTA variant of the Input Box Template use action buttons on the notification to collect input from the user.

To set the CTAs use the Advanced Options when setting up the campaign on the dashboard.

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/InputctaBasicDismissAndroid12.gif" alt="InputctaBasicDismissAndroid12" width="450" height="800"/>

### CTAs with Remind Later option

This variant of the Input Box Template is particularly useful if the user wants to be reminded of the notification after sometime. Clicking on the remind later button raises an event to the user profiles, with a custom user property p2 whose value is a future time stamp. You can have a campaign running on the dashboard that will send a reminder notification at the timestamp in the event property.

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/InputctasRemindAndroid12.gif" alt="InputctasRemindAndroid12" width="450" height="800"/>

### Reply as an Event

This variant raises an event capturing the user's input as an event property. The app is not opened after the user sends the reply.

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/InputctasNoOpenAndroid12.gif" alt="InputctasNoOpenAndroid12" width="450" height="800"/>

### Reply as an Intent

This variant passes the reply to the app as an Intent. The app can then process the reply and take appropriate actions.

<img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/InputctaWithOpenAndroid12.gif" alt="InputctaWithOpenAndroid12" width="450" height="800"/>

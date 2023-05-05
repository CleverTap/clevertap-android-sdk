## CleverTap Android SDK CHANGE LOG

### Version 4.7.5 (March 6, 2023)
* Bug fixes and performance improvements.

### Version 4.7.4 (January 27, 2023)
* Bug fixes and performance improvements.

### Version 4.7.3 (January 25, 2023)
* Fixes message UI for footer in-app.
* Fixes NPE when clicked on body of InboxMessage with deep link in `CTInboxListFragment`
* Other bug fixes and performance improvements.

### Version 4.7.2 (December 16, 2022)
* Fixes a crash (`ClassCastException`) in header/footer InApp templates.

### Version 4.7.1 (December 5, 2022)
* Fixes ANR on main thread for static initialization of `SimpleDateFormat()`.
* Add Proguard rules to keep `CREATOR` instance for Parcelable classes to prevent `ClassNotFoundException` when unmarshalling: `androidx.fragment.app.FragmentManagerState`
* Made calls to `getInstallReferrer()` async to prevent ANR when called on main thread.
* Used `ConcurrentHashMap` instead of `HashMap` for storing `CleverTapAPI` instances to prevent ConcurrentModificationException when trying to access the instances concurrently.
* Made calls to `findCTPushProvider()` and `findCustomEnabledPushTypes()` async to prevent ANR when called on main thread.
* Renames `setPushPermissionNotificationResponseListener(PushPermissionResponseListener)` to `registerPushPermissionNotificationResponseListener(PushPermissionResponseListener)` . Each `PushPermissionResponseListener` instance passed in this method is now maintained in a list of the `PushPermissionResponseListener` type and the Push Primer result is notified to all the elements of this list.
* Adds `unregisterPushPermissionNotificationResponseListener(PushPermissionResponseListener)` method in `CleverTapAPI` class to unregister the  `PushPermissionResponseListener` instance to stop observing the  Push Primer result.
* Use v4.7.2, this version contains a bug which causes a crash (`ClassCastException`) in header/footer InApp templates.


### Version 4.7.0 (November 1, 2022)
* Adds below new public APIs for supporting [Android 13 notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)
  * `isPushPermissionGranted()` [Usage can be found here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/EXAMPLES.md#check-the-status-of-notification-permission-whether-its-granted-or-denied)
  * `promptPushPrimer(JSONObject)` [Usage can be found here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/EXAMPLES.md#push-primer-android-13-notification-runtime-permission)
  * `promptForPushPermission(boolean showFallbackSettings)` [Usage can be found here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/EXAMPLES.md#call-android-os-runtime-notification-dialog-without-using-push-primer)
* New `CTLocalInApp` builder class available to create half-interstitial & alert local in-apps to request notification permission [Usage can be found here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/EXAMPLES.md#ctlocalinapp-builder-methods-description)
* New callback `PushPermissionResponseListener` available which returns after user Allows/Denies notification permission [Usage can be found here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/EXAMPLES.md#available-callbacks-for-push-primer)
* From 4.7.0+ existing callback `InAppNotificationListener` will now have `onShow(CTInAppNotification)` method which needs to implemented
* Minimum Android SDK version bumped to API 19 (Android 4.4)
* Use v4.7.2, this version contains a bug which causes a crash (`ClassCastException`) in header/footer InApp templates.

### Version 4.6.9 (March 31, 2023)
#### Changes
* Renames the `itemIndex` parameter of the `onInboxItemClicked` callback with the `contentPageIndex`. It's not a breaking change.
* **[Parity with CleverTap iOS SDK]**:
  The `onInboxItemClicked` callback now provides a different value for contentPageIndex(ex-`itemIndex`) compared to before. Previously, it used to indicate the position of the clicked item within the list container of the App Inbox. However, now it indicates the page index of the content, which ranges from 0 to the total number of pages for carousel templates. For non-carousel templates, the value is always 0, as they only have one page of content.

### Version 4.6.8 (March 22, 2023)
#### Breaking Changes
* **Signature change of `onInboxItemClicked` callback**:
  It is changed from  `onInboxItemClicked(CTInboxMessage message)` to `onInboxItemClicked(CTInboxMessage message, int itemIndex, int buttonIndex)`. The `itemIndex` corresponds the index of the item clicked in the list whereas the `buttonIndex` for the App Inbox button clicked (0, 1, or 2). A value of -1 indicates the App Inbox item is clicked.

* **Behavioral change of `onInboxItemClicked` callback**:
  - Previously, the callback was raised when the App Inbox Item is clicked.
  - Now, it is also raised when the App Inbox button and Item is clicked.

#### Added
* Adds the new public API `dismissAppInbox()` via `CleverTapAPI` class to dismiss the App Inbox.

### Version 4.6.7 (March 15, 2023)
* Bug fixes and performance improvements.
* **Note:** This release is being done for Android 12 targeted users, satisfying below points.
  * Targeting Android 12 and
  * Using RenderMax and/or using Push Templates

### Version 4.6.6 (October 31, 2022)
* Fixes App Inbox bug where an Inbox message's video would not play when new Inbox messages were available

### Version 4.6.5 (October 21, 2022)
* Allows more special characters when setting custom CleverTap ID

### Version 4.6.4 (October 11, 2022)
* Bug fixes and improvements

### Version 4.6.3 (September 20, 2022)
* Fix crash in App inbox when No additional tabs are used .

### Version 4.6.2 (September 13, 2022)
* Support for exoplayer [`v2.17.1`](https://github.com/google/ExoPlayer/releases/tag/r2.17.1) . Note : this upgrade will result in minor ui changes for interstitial in app and inbox notifications that uses exoplayer
* Note: Kindly upgrade to version CleverTap Android SDK v4.6.3 and above if you face any issues with app inbox

### Version 4.6.1 (September 6, 2022)
* App inbox blue dot fix :  This release fixes the bug where new entries in inbox would continue showing blue dot when scrolled up and down . expected behaviour is to stop showing blue dot after 2 seconds.
* App inbox onMessage Click Callback : user can now pass an `InboxMessageListener` in addition to `InboxMessageButtonListener` to receive inbox item click
* Note: Kindly upgrade to version CleverTap Android SDK v4.6.3 and above if you face any issues with app inbox

### Version 4.6.0 (August 4, 2022)
* Improved push synchronization for multiple push services

### Version 4.5.2 (July 22, 2022)
* Fixes a bug for notification CTA deeplink for Android 12 and above devices - On clicking notification CTA, deeplink launches third party app instead of X app even though X app is capable of handling deeplink. For example, if X app is capable of handling https://google.com(sample link) but deeplink launches browser instead of X app.

### Version 4.5.1 (July 12, 2022)
* New Feature : You can now call `onUserLogin`, `incrementValue` and `decrementValue` method via WebView Interface.
* Improvement : Updated SSL Pinning Certificates

### Version 4.5.0 (June 3, 2022)
* `removeValueForKey()` in `CleverTapAPI` can now remove PII data like Email, Phone and Date Of Birth.
* Improved the `ActivityLifecycleCallback`’s `onPaused` logic so that it runs on the background thread to avoid any runtime issues. Fixes #221.
* Adds support to change credentials for the CleverTap Xiaomi Push SDK using `changeXiaomiCredentials`. Contribution PR #269.
* Adds support to enable/disable the CleverTap Xiaomi Push SDK using `enableXiaomiPushOn` method. CleverTap Xiaomi Push SDK can now be enabled/disabled for `ALL_DEVICES`, `XIAOMI_MIUI_DEVICES` and `NO_DEVICES`.
* Adds analytics support for upcoming CleverTap Direct Call Android SDK.
* Sets up CI/CD using Github Actions.
Note : If you are facing `ClassNotFoundException` "org.jacoco.agent.rt.internal_28bab1d.Offline" after updating to 4.5.0, Please update the SDK to v4.5.1

### Version 4.4.0 (December 20, 2021)
* Adds below new public APIs for smooth and easy integration of Custom Android Push Notifications Handling(FCM),Custom Push Amplification Handling and Push Templates
  * `CTFcmMessageHandler().createNotification(applicationContext, message)`
  * `CTFcmMessageHandler().processPushAmp(applicationContext, message)`
  * `CleverTapAPI.setNotificationHandler(notificationHandler)`
* Adds support for Firebase Cloud Messaging v21 and above

### Version 4.3.1 (November 25, 2021)
* Fixes a Strict Mode Read violation for low RAM devices

### Version 4.3.0 (November 2, 2021)
* Adds support for [apps targeting Android 12 (API 31)](https://developer.android.com/about/versions/12/behavior-changes-12)
  This version is compatible with all new Android 12 changes like Notification Trampolines, Pending Intents Mutability and Safer Component Exporting.
  For more information check out the [CleverTap documentation for Android 12 here](https://developer.clevertap.com/docs/android-push#android-12-changes)
* Deprecated `CTPushNotificationReceiver` and `CTNotificationIntentService` as a part of Notification Trampoline restrictions in Android 12 (API 31)
* Last version with support for custom FCM Sender ID for generating the FCM token
* Fixes a bug where `UTM Visited` event was not being raised on click of a direct deep link

### Version 4.2.0 (July 15, 2021)
* Adds public methods for suspending/discarding & resuming InApp Notifications
* Adds public methods to increment/decrement values set via User properties
* Adds a new public method `getCleverTapID(OnInitCleverTapIDListener)` to get CleverTap ID through `OnInitCleverTapIDListener` callback
* Deprecated `SyncListener` interface and will be removed in future versions, use `getCleverTapID(OnInitCleverTapIDListener)` instead
* Deprecated `getCleverTapAttributionIdentifier()` method and will be removed in future versions, use `getCleverTapID(OnInitCleverTapIDListener)` instead
* Adds new `CleverTapAPI.LogLevel.VERBOSE` level for debugging
* Fixes App Inbox UI for Android Tablets
* Fixes `recordScreen()` NPE crash
* Fixes a few Strict Mode Read violations that caused ANRs
* Other performance improvements and improved logging

### Version 4.1.1 (May 4, 2021)
* Adds `setFirstTabTitle` method to set the name of the first tab in App Inbox
* Adds `pushChargedEvent` to `CTWebInterface` class to allow raising Charged Event from JS
* Removes a `NoClassDefFoundError` raised in Android Kitkat (v4.4) - #168
* Removes a `NullPointerException` raised while handling `InstallReferrerClient` - #166
* Other bug fixes

### Version 4.1.0 (April 13, 2021)
* Adds support for Android 11
* Reduces the SDK size and added performance improvements
* Removes the deprecated Product Experiences (Screen AB/Dynamic Variables) related code
* Removes support for JCenter
* Fixes a bug where Xiaomi, Huawei, Baidu and other push service tokens were not switched to new profiles when using `onUserLogin`

### Version 4.0.4 (Mar 2, 2021)
* Fixed FCM token refresh issue when multiple Firebase Projects are integrated in the application.
If you're using multiple Firebase projects in your app, use this version instead of v4.0.0 ~ v4.0.3

### Version 4.0.3 (Feb 22, 2021)
* Fixed product config & other crashes ([#127](https://github.com/CleverTap/clevertap-android-sdk/issues/127) , [#132](https://github.com/CleverTap/clevertap-android-sdk/issues/132) , [#147](https://github.com/CleverTap/clevertap-android-sdk/issues/147)) 

### Version 4.0.2 (December 28, 2020)
* Fix for In-apps/Inbox not rendering in v4.0.1 on using configurable Clevertap Identities feature introduced in v4.0.1

### Version 4.0.1 (November 30, 2020)
* Adds support for configurable CleverTap identifiers.
* Adds deprecation warnings for Product A/B Tests public methods.
* Fix for multiple App Launched and App Installed events in the first session.
* Fixes crash which occurred due to wrong classification of some mobile devices as tablets #116.
* Optimized proguard rules for better obfuscation.

### Version 4.0.0 (October 1, 2020)

* Adds support for Android 10 and AndroidX support libraries.
* This is a major release, please find the list of all [changes here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTV4CHANGES.md)
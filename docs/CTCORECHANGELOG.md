## CleverTap Android SDK CHANGE LOG
### Version 7.4.0 (May 7, 2025)

#### New Features
* Introduces System App Functions (Open Url, App Rating, Push Permission Request - as mentioned [here](SystemInAppFunctions.md)) which can be triggered either as a button action within an in-app message or as a standalone campaign action in CleverTap, enriching client workflows.
* Adds support for Advanced InApp Builder templates. This feature enables easy creation of visually appealing in-app messages that seamlessly integrate with your app's look and feel. It includes support for various aspect ratios for header and footer type floating banners, triggered via in-app event-based actions.
* Resolves the following Strict Mode Violations:
    * `#669` ([#669](https://github.com/CleverTap/clevertap-android-sdk/issues/669)), `#679` ([#679](https://github.com/CleverTap/clevertap-android-sdk/issues/679)): `DiskReadViolation` occurring on SDK initialisation.
    * `#673` ([#673](https://github.com/CleverTap/clevertap-android-sdk/issues/673)): `DiskReadViolation` occurring after an `InApp` message was shown.
    * `#674` ([#674](https://github.com/CleverTap/clevertap-android-sdk/issues/674)): `IncorrectContextUseViolation` occurring when the SDK was initialised with an application context.
    * `IncorrectContextUseViolation` occurring when an `AppInbox` message consisted of an audio/video message.
    * `UntaggedSocketViolation` occurring on network requests.

### Version 7.3.1 (March 27, 2025)

#### Bug Fixes
* Fixes an issue where InApps were not displayed after an app upgrade

### Version 7.3.0 (March 11, 2025)
> ‼️ **NOTE**
Please update to 7.3.1 or above if using InApps

#### Breaking API Changes
*   **Huawei Push Integration - Updated Setup Required:** The integration process for Huawei Push has been significantly updated. If you have previously integrated the CleverTap Huawei Push SDK, you **must** follow the new steps outlined [here](https://developer.clevertap.com/docs/clevertap-huawei-push-integration) to ensure continued functionality.
*   **Baidu Push Integration - Updated Setup Required:** The integration process for Baidu Push has been updated. If you are using Baidu Push Integration, please follow the revised instructions [here](https://developer.clevertap.com/docs/baidu-push-notifications) to maintain proper functionality.
*   **Removes Provider-Specific Push Registration:** The `pushBaiduRegistrationId()` and `pushHuaweiRegistrationId()` methods have been removed from `CleverTapAPI`. This change unifies push registration under a single method. You **must** now use the `pushRegistrationToken()` method, providing the correct `PushType` constant to identify the push provider (Huawei or Baidu). See the updated integration guides for Huawei and Baidu for the correct usage.

#### Bug Fixes
* Fixes an issue in CleverTapAPI.getCleverTapID method where the listener can be possibly null resulting in a NPE.
* Fixes an issue where device font size affect InApps text size causing it to not fit correctly in the container.

### Version 7.2.2 (January 21, 2025)
> ‼️ **NOTE**
Please update to 7.3.1 or above if using InApps

This hotfix release addresses a critical issue from `v7.1.0` onwards:

#### Bug Fixes
* Fixes an issue where `Notification Clicked` event was not being raised.
* Fixes an issue where InApp was not shown in a certain flow after clicking a notification.


### Version 7.2.1 (January 16, 2025)
> ‼️ **NOTE**
A critical issue was identified in 7.2.1, please update to 7.2.2 and above

#### Bug Fixes
* Fixes `ClassCastException` from `Integer` to `Long` for server side in-apps delivery. A bug occurs when the network is turned off, and the following steps are performed:
  * send event <event_name_1> (triggers campaign <campaign_id_1>)
  * stop the app
  * turn network on and then restart the app.

### Version 7.2.0 (January 7, 2025)
> ‼️ **NOTE**
A critical issue was identified in 7.2.0, please update to 7.2.2 and above

#### New Features

* Adds support for Android 15, making it compliant with Android 15 requirements. Details [here](https://developer.android.com/about/versions/15/summary)
* Upgrades the algorithm used for [encryption of PII data](EXAMPLES.md#encryption-of-pii-data-), making it compliant with [OWASP](https://mas.owasp.org/MASTG/0x04g-Testing-Cryptography/). Uses AndroidKeyStore for securely backing up encryption key on api levels 23+.
* Updates Minimum Android SDK version to API 21 (Android 5.0)

> ⚠️ **NOTE**
After upgrading the SDK to v7.2.0+, don't downgrade in subsequent app releases. If you encounter any issues, please contact the CleverTap support team for assistance.

### Version 7.1.2 (January 29, 2025)
#### Bug Fixes
* Avoids an R8 related issue by compiling with `JDK17` to support `Unity`, `ReactNative`, `Cordova` and `Flutter` hybrid SDKs of `CleverTap`
* Fixes an issue where `Notification Clicked` event was not being raised.
* Fixes an issue where InApp was not shown in a certain flow after clicking a notification.

### Version 7.1.0 (December 24, 2024)
> ‼️ **NOTE**
A critical issue has been identified in version 7.1.0. Please update to version 7.2.2 or above. This update should not introduce any build errors in most cases. However, if you encounter any issues during the update, consider updating to version 7.1.2 instead.

#### New Features

* Adds support for triggering InApps based on first-time event filtering in multiple triggers. Now you can create campaign triggers that combine recurring and first-time events. For example: Trigger a campaign when "Charged" occurs (every time) OR "App Launched" occurs (first time only).
* Adds new user-level event log tracking system to store and manage user event history. New APIs include:
  * `getUserEventLog()`: Get details about a specific event
  * `getUserEventLogCount()`: Get count of times an event occurred
  * `getUserLastVisitTs()`: Get timestamp of user's last app visit
  * `getUserAppLaunchCount()`: Get total number of times user has launched the app
  * `getUserEventLogHistory()`: Get full event history for current user
* Adds support to hide large icon in android notification by sending wzrk_hide_large_icon key in notification payload.
    
#### API Changes

* **Deprecated:**  The old event tracking APIs tracked events at the device level rather than the user level, making it difficult to maintain accurate user-specific event histories, especially in multi-user scenarios. The following methods have been deprecated in favor of new user-specific event tracking APIs that provide more accurate, user-level analytics. These deprecated methods will be removed in future versions with prior notice:
  * `getDetails()`: Use `getUserEventLog()` instead for user-specific event details
  * `getCount()`: Use `getUserEventLogCount()` instead for user-specific event counts
  * `getFirstTime()`: Use `getUserEventLog()` instead for user-specific first occurrence timestamp
  * `getLastTime()`: Use `getUserEventLog()` instead for user-specific last occurrence timestamp
  * `getPreviousVisitTime()`: Use `getUserLastVisitTs()` instead for user-specific last visit timestamp
  * `getTotalVisits()`: Use `getUserAppLaunchCount()` instead for user-specific app launch count
  * `getHistory()`: Use `getUserEventLogHistory()` instead for user-specific event history

#### Bug Fixes
* Fixes [#671](https://github.com/CleverTap/clevertap-android-sdk/issues/671) - an `AbstractMethodError` in the AppInbox feature when using audio/video.
* Fixes issues when File type variable changes from validValue -> null


### Version 7.0.3 (November 29, 2024)

#### New Features
* Changes campaign triggering evaluation of event names, event properties, and profile properties to ignore letter case and whitespace.
* Adds support for previewing in-apps created through the new dashboard advanced builder.

### Version 7.0.2 (October 10, 2024)

#### New Features
* Adds support for custom handshake domain configuration in android manifest
* Adds support for custom code in-app templates definitions through a json scheme. Please refer to the [CustomCodeTemplates.md](CustomCodeTemplates.md) for further details.


### Version 7.0.1 (September 2, 2024)

#### Bug Fixes
* Fixes an `ArrayIndexOutOfBoundsException` in a rare `race condition` scenario due to un-synchronised list
* Fixes an issue where the Android Permission popup is not triggered from `PushPrimer` when using `InAppType.HALF_INTERSTITIAL`

### Version 7.0.0 (August 5, 2024)

#### New Features

* Adds support for triggering InApps based on user attribute changes.
* Adds support for custom code templates for InApp notifications. Please refer to the [CustomCodeTemplates.md](CustomCodeTemplates.md) for further details.
* Removes character limit of maximum 3 lines from AppInbox messages.
* Adds support for File Types for Variables. Please refer to [Variables.md](Variables.md#define-file-variables) for further details.
* Adds new API 
  - `clearFileResources(boolean expiredOnly)`
      - This API allows you to delete all types of files which are preloaded for SDK features like Custom In-App Templates, App Functions, Variables etc.

#### Bug Fixes

* Fixes a race-condition bug where the App Launch event was triggered twice when using a custom CleverTap ID.
* Fixes an ANR caused by extremely old InApp campaigns.
* Fixes an issue where incorrect callbacks were sent for InApps when the phone was rotated.
* Fixes an issue where an InApp was displayed even after all the campaigns were stopped.
* Fixes an issue where the InApp image was not shown when the phone was rotated to landscape.
* Fixes an issue where certain URLs loaded incorrectly in custom HTML InApp templates.

#### Dependency Update

* Adds support for `AndroidX Media3` in lieu of the [deprecation](https://developer.android.com/media/media3/exoplayer/migration-guide) of `ExoPlayer`. While Clevertap continues to support `ExoPlayer`, migration is recommended. For migration refer [here](EXAMPLES.md#migrating-from-exoplayer-to-androidx-media3).


### Version 6.2.1 (April 11, 2024)
This hotfix release addresses the following issue in `v6.2.0`:

#### Bug Fixes
* Fixes a crash `IllegalArgumentException` caused by `allowedPushType` XPS enum.

### Version 6.2.0 (April 3, 2024)
> ⚠️ **NOTE**
6.2.0 produces a crash, please update to 6.2.1 and above.

#### Bug Fixes
* Extends the push primer callback to notify permission denial when cancel button is clicked on `PromptForSettings` alert dialog.
* Fixes [#576](https://github.com/CleverTap/clevertap-android-sdk/issues/576) - a crash due to `ClassNotFoundException` for `CTBackgroundJobService`. 
* Fixes a crash due to `NullPointerException` related to `deviceInfo.deviceId`.
* Fixes an ANR related to `isMainProcess` check.
* Fixes an ANR due to eager initialisation of `CtApi` triggered by DeviceId generation.

#### Breaking API Changes
* Removes all Xiaomi related public methods as the Xiaomi SDK has been discontinued. Details [here](https://developer.clevertap.com/docs/discontinuation-of-xiaomi-push-service).

### Version 6.1.1 (February 27, 2024)

#### Bug Fixes
* Fixes an issue of incorrect endpoint in the case of network handshake.
* Fixes a bug in Client Side InApps with regards to frequency limits.

For developers with [BACKGROUND_SYNC](https://developer.clevertap.com/docs/android-push#pull-notification) enabled in their previous app version and now upgrading to _clevertap-android-sdk v6.1.0_ or _clevertap-android-sdk v6.1.1_ , please add this to your `AndroidManifest.xml` to avoid `ClassNotFoundException` related crashes

```xml
<service 
    android:name="com.clevertap.android.sdk.pushnotification.amp.CTBackgroundJobService"
    android:exported="false"
    android:enabled="false"
    tools:ignore="MissingClass"/>
```

### Version 6.1.0 (February 21, 2024)
> ⚠️ **NOTE**
Please update to 6.1.1 and above

#### New Features

* Supports Android 14, made it compliant with Android 14 requirements. Details [here](https://developer.android.com/about/versions/14/summary)
* Upgrades AGP to 8.2.2 for building the SDK and adds related consumer proguard rules
* Deprecates Xiaomi public methods as we are sunsetting SDK. Details [here](https://dev.mi.com/distribute/doc/details?pId=1555).
* Adds Accessibility ids for UI components of SDK
* Migrates JobScheduler to WorkManager for Pull Notifications.

#### Breaking API Changes

* **CTPushAmpWorker breaks custom WorkerFactory implementation of an App**:
    * If you are using custom `WorkFactory` implementation of `WorkManager` then make sure that you
      correctly handle workers defined by CleverTap SDK and other third party dependencies.
    * You must return `null` from `createWorker()` for any unknown workerClassName. Please check
      implementation provided in the
      blog [here](https://medium.com/androiddevelopers/customizing-workmanager-fundamentals-fdaa17c46dd2)

#### Bug Fixes

* Fixes InApps crash in a rare activity destroyed race condition
* Fixes Potential ANR in a race condition of SDK initialisation in multithreaded setup
* Fixes [#456](https://github.com/CleverTap/clevertap-android-sdk/issues/428) - Build issues due to AGP 8

### Version 6.0.0 (January 15, 2024)

#### New Features

* Adds support for client-side in-apps.
* Adds new API
  - `clearInAppResources(boolean expiredOnly)`
      - This API allows you to delete images and gifs which are preloaded for InApps in `client-side` mode.


#### Bug Fixes

* Fixes no empty message for app inbox without tabs
* Removes onClickListener for Image of Cover InApp

#### Dependency Update

* Adds support for exoplayer `v2.19.1`

### Version 5.2.2 (December 22, 2023)

#### New Features

* Migrates SDK dependency management to Version Catalog

#### Bug Fixes

* Fixes a bug where JavaScript was not working for custom html InApp header/footer templates.
* Fixes an **NPE** related to AppInbox APIs.
* Fixes a **ClassCastException** in `defineVariable` API of Product Experiences.
* Fixes a resource name conflict with the firebase library in `fcm_fallback_notification_channel_label`
* Fixes the scope of `CTInboxMessageContent.java` to allow access to its methods.
* Fixes a StrictMode Violation spawning from `ctVariables.init()`.
* Removes use of lossy conversions leading to an issue in PushTemplates.
* Handles an edge case related to migration of encryption level when local db is out of memory

### Version 5.2.1 (October 12, 2023)

#### New Features

- Adds Custom Proxy Domain functionality for Push Impressions and Events raised from CleverTap SDK. Please refer to [EXAMPLES.md](EXAMPLES.md#integrate-custom-proxy-domain) file to read more on how to
  configure custom proxy domains.
- Adds new API,
  * `setLocale(String locale)`
      - This API allows you to set a custom locale for the required clevertap instance. Different instances can have different locales
- Adds support for Integration Debugger

### Version 5.2.0 (August 10, 2023)

#### New Features

* Adds support for encryption of PII data wiz. Email, Identity, Name and Phone. 
  Please refer to [EXAMPLES.md](EXAMPLES.md#encryption-of-pii-data) file to read more on how to
  enable/disable encryption.
* Adds support for custom KV pairs common to all inbox messages in AppInbox.

#### Bug Fixes
* Fixes a bug where addMultiValueForKey and addMultiValuesForKey were overwriting the 
  current values of the user properties instead of appending it.
* Fixes [#393](https://github.com/CleverTap/clevertap-android-sdk/issues/393) - push permission flow 
  crash when context in CoreMetadata is null.

### Version 5.1.0 (June 28, 2023)

> ⚠️ **NOTE**

```
Please remove the integrated Rendermax SDK before you upgrade to Android SDK v5.1.0
```

#### New Features

* Adds new APIs,
    * `getNotificationBitmapWithTimeout(
      Context context, Bundle bundle, String bitmapSrcUrl,
      boolean fallbackToAppIcon, long timeoutInMillis)`
        - This API allows you to retrieve a notification bitmap from the specified `bitmapSrcUrl`
          with a specified timeout. In case the bitmap retrieval fails, you can choose to fallback
          to the app icon by setting the `fallbackToAppIcon` parameter. This API provides more
          control over the bitmap retrieval process for custom rendering.
    * `getNotificationBitmapWithTimeoutAndSize(
      Context context, Bundle bundle, String bitmapSrcUrl,
      boolean fallbackToAppIcon, long timeoutInMillis, int sizeInBytes)`
        - This API extends the functionality of the previous one by additionally allowing you to
          specify the desired size in bytes for the retrieved bitmap. This is useful when you need
          to limit the size of the bitmap to optimize memory usage.
          By utilizing these new APIs, you can enhance the push delivery experience for custom
          rendering and ensure efficient handling of notification bitmaps in your Android app.
* Adds support for developer defined default notification channel. Please refer to
  the [EXAMPLES.md](EXAMPLES.md#push-notifications) file to read more on how to setup default
  channel in your app.Also please note that this is only supported for clevertap core notifications.
  Support for push templates will be released soon.
* RenderMax Push SDK functionality is now supported directly within the CleverTap Core SDK.
* Adds interface for `Leanplum` APIs. This interface wraps `CleverTapAPI` methods inside `Leanplum`
  APIs to ensure a smoother migration experience.

#### API Changes

* Adds `SCCampaignOptOut` Event to Restricted Events Name List for **internal use**.
* Adds custom sdk versions to `af` field for **internal use**.

#### Breaking API Changes

* **CTFlushPushImpressionsWork breaks custom WorkerFactory implementation of an App**:
    * If you are using custom `WorkFactory` implementation of `WorkManager` then make sure that you
      correctly handle workers defined by CleverTap SDK and other third party dependencies.
    * You must return `null` from `createWorker()` for any unknown workerClassName. Please check
      implementation provided in the
      blog [here](https://medium.com/androiddevelopers/customizing-workmanager-fundamentals-fdaa17c46dd2)

* **Behavioral change of `createNotification` methods**:
    * The following APIs now run on the caller's thread. Make sure to call it
      in `onMessageReceive()` of messaging service:
        - `CTFcmMessageHandler().createNotification(getApplicationContext(), message)`
        - `CleverTapAPI.createNotification(getApplicationContext(), extras)`
        - `CTXiaomiMessageHandler().createNotification(getApplicationContext(), message)`
        - `CTHmsMessageHandler().createNotification(getApplicationContext(), message)` - **This API
          should always be called on a background thread.**

#### Bug Fixes

* Fixes [#428](https://github.com/CleverTap/clevertap-android-sdk/issues/428) - Race-condition when
  detecting if an in-app message should show.
* Fixes Push primer alert dialog freeze behavior, which became unresponsive when clicked outside the
  window.

### Version 5.0.0 (May 5, 2023)

#### New Features

* Adds support for Remote Config Variables. Please refer to the [Variables.md](Variables.md) file to
  read more on how to integrate this to your app.
* Adds new APIs, `markReadInboxMessagesForIDs(ArrayList<String> messageIDs)`
  and `deleteInboxMessagesForIDs(ArrayList<String> messageIDs)` to mark read and delete an array of
  Inbox Messages.

#### API Changes

* **Deprecated:** The following methods and classes related to Product Config and Feature Flags have
  been marked as deprecated in this release, instead use new remote config variables feature. These
  methods and classes will be removed in the future versions with prior notice.
    * Product config
        - `productConfig()`
        - `productConfig().setDefaults()`
        - `productConfig().fetch()`
        - `productConfig().fetch(intervalInSeconds)`
        - `productConfig().activate()`
        - `productConfig().fetchAndActivate()`
        - `setCTProductConfigListener()`
        - `onInit()`
        - `onFetched()`
        - `onActivated()`
        - `productConfig().setMinimumFetchIntervalInSeconds(seconds)`
        - `productConfig().getBoolean(key)`
        - `productConfig().getDouble(key)`
        - `productConfig().getLong(key)`
        - `productConfig().getString(key)`
        - `productConfig().reset()`
        - `productConfig().getLastFetchTimeStampInMillis()`
    * Feature flags
        - `featureFlag()`
        - `setCTFeatureFlagsListener()`
        - `featureFlagsUpdated()`
        - `featureFlag().get(key,defaultVal)`

#### Breaking API Changes
* **Signature change of `onInboxItemClicked` callback**:
  It is changed from `onInboxItemClicked(CTInboxMessage message)` to `onInboxItemClicked(CTInboxMessage message, int contentPageIndex, int buttonIndex)`. The `contentPageIndex` corresponds to the page index of the content, which ranges from 0 to the total number of pages for carousel templates. For non-carousel templates, the value is always 0, as they only have one page of content. The `buttonIndex` represents the index of the App Inbox button clicked (0, 1, or 2). A value of -1 indicates the App Inbox item is clicked.

* **Behavioral change of `onInboxItemClicked` callback**:
  - Previously, the callback was raised when the App Inbox Item is clicked.
  - Now, it is also raised when the App Inbox button and Item is clicked.

#### Bug Fixes
* Fixes a bug where App Inbox was not respecting the App Inbox background color when no tabs are provided.
* Fixes the non-EU retry mechanism bug

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
* Adds below new public APIs for smooth and easy integration of Custom Android Push Notifications Handling(FCM),Custom Pull Notifications Handling and Push Templates
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
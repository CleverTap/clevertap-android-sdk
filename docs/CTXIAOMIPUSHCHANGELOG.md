## ⚠️ Deprecation Notice
> Xiaomi Corporation made a significant announcement recently, notifying users about discontinuing the Mi Push service beyond Mainland China due to operational concerns. You might have already received communication regarding this matter.
Read the official announcement from the Xiaomi Corporation [here](https://dev.mi.com/distribute/doc/details?pId=1555).
With the Mi Push service's closure, CleverTap will cease offering Mi Push support for Xiaomi devices. After the shutdown, Xiaomi devices will still receive push notifications through Firebase Cloud Messaging (FCM).

## CleverTap Xiaomi Push SDK CHANGE LOG

### Version 1.5.4 (October 12, 2023)
* Fixes an issue related to push impressions leading to a profile split

### Version 1.5.3 (August 10, 2023)
* Updated Xiaomi Push SDK to v5.1.5
* Supports CleverTap Android SDK v5.2.0

### Version 1.5.2 (November 1, 2022)
* Updated Xiaomi Push SDK to v5.1.1 which supports Android 13
* Supports CleverTap Android SDK v4.7.0

### Version 1.5.1 (October 11, 2022)
* Support for providing region to MiPushClient via MultiInstance too

### Version 1.5.0 (September 6, 2022)
* Supporting Xiaomi Regions
  * This Release makes the CT XPS SDK compatible with the latest [Xiaomi Privacy Upgrades](https://dev.mi.com/console/doc/detail?pId=2761)
  * **[Breaking Change]** This release makes it mandatory for Clients to include the Xiaomi Push Library as a part of their gradle dependencies. Check the [integration doc](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTXIAOMIPUSH.md)for more info
  * **[Breaking Change]**  This release also makes it mandatory for clients to support  `minSdkVersion  19` as compared to previous  `minSdkVersion  16`
  * For Clients using the default implementation of CT Xiaomi SDK , they would not require any additional changes  in their codebase apart from  the above mentioned gradle and dependency changes
  * **[Breaking Change]** For Clients using a custom implementation of Xiaomi Push Receiver, they will also need to update the code to pass Region in `clevertapApi.pushXiaomiRegistrationId(regId, region, register)` function.

### Version 1.4.0 (June 3, 2022)
* Supports the `enableXiaomiPushOn` method in the core SDK. CleverTap Xiaomi Push SDK can now be enabled/disabled for `ALL_DEVICES`, `XIAOMI_MIUI_DEVICES` and `NO_DEVICES`
* Supports CleverTap Android SDK v4.5.0

### Version 1.3.0 (March 2, 2022)
* Updated Xiaomi Push SDK to v4.8.6

### Version 1.2.0 (December 20, 2021)
* Adds below new public APIs for smooth and easy integration of Custom Android Push Notifications Handling(XPS),Custom Pull Notifications Handling and Push Templates.
  * `CTXiaomiMessageHandler().createNotification(applicationContext,message)`
  * `CTXiaomiMessageHandler().processPushAmp(applicationContext,message)`
* Supports CleverTap Android SDK v4.4.0

### Version 1.1.0 (November 2, 2021)
* Updated Xiaomi Push SDK to v4.8.2
* Supports CleverTap Android SDK v4.3.0

### Version 1.0.2 (May 4, 2021)
* Fixes the "unspecified" error during Gradle build
* Please do not use v1.0.1 and use this version instead

### Version 1.0.1 (April 13, 2021)
* Updated Xiaomi Push SDK to v3.8.9
* Supports CleverTap Android SDK v4.1.0

### Version 1.0.0 (October 1, 2020)
* Initial release! 🎉
* Supports CleverTap Android SDK v4.0.0
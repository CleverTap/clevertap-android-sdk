## CleverTap Xiaomi Push SDK CHANGE LOG

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
* Adds below new public APIs for smooth and easy integration of Custom Android Push Notifications Handling(XPS),Custom Push Amplification Handling and Push Templates.
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
## CleverTap Huawei Push SDK CHANGE LOG

### Version 1.5.1 (September 26, 2025)
* Adds support for custom initialization of `AGConnect` using `agconnect-services.json` from the assets folder.

### Version 1.5.0 (March 11, 2025)

#### Breaking API Changes
*   **Huawei Push Integration - Updated Setup Required:** The integration process for Huawei Push has been significantly updated. If you have previously integrated the CleverTap Huawei Push SDK, you **must** follow the new steps outlined [here](https://developer.clevertap.com/docs/clevertap-huawei-push-integration) to ensure continued functionality.
*   **Pluggable Huawei Push Integration:** Huawei Push can now be integrated as a pluggable component, allowing for more flexibility and avoiding initialization with reflection pre-emptively in the core SDK.

### Version 1.4.0 (January 7, 2025)
* Adds support for Android 15, making it compliant with Android 15 requirements. Details [here](https://developer.android.com/about/versions/15/summary)
* Updates Minimum Android SDK version to API 21 (Android 5.0)

### Version 1.3.4 (February 21, 2024)
* Supports Android 14, made it compliant with Android 14 requirements. Details [here](https://developer.android.com/about/versions/14/summary)
* Upgrades AGP to 8.2.2 for building the SDK and adds related consumer proguard rules

### Version 1.3.3 (August 10, 2023)
* Updated Huawei Push SDK to v6.11.0.300
* Supports CleverTap Android SDK v5.2.0

### Version 1.3.2 (November 1, 2022)
* Updated Huawei Push SDK to v6.7.0.300 which supports Android 13
* Supports CleverTap Android SDK v4.7.0
* Minimum Android SDK version bumped to API 19 (Android 4.4)

### Version 1.3.1 (September 6, 2022)
* Updated Huawei Push SDK to v6.5.0.300

### Version 1.3.0 (April 26, 2022)
* Updated Huawei Push SDK to v6.3.0.304

### Version 1.2.0 (December 20, 2021)
* Adds below new public APIs for smooth and easy integration of Custom Android Push Notifications Handling(HMS),Custom Pull Notifications Handling and Push Templates.
  * `CTHmsMessageHandler().createNotification(applicationContext,message)`
  * `CTHmsMessageHandler().processPushAmp(applicationContext,message)`
* Supports CleverTap Android SDK v4.4.0

### Version 1.1.0 (November 2, 2021)
* Updated Huawei Push SDK to v6.1.0.300
* Supports CleverTap Android SDK v4.3.0

### Version 1.0.2 (July 15, 2021)
* Updated Huawei Push SDK to v5.3.0.304
* Supports CleverTap Android SDK v4.2.0

### Version 1.0.1 (April 13, 2021)
* Updated Huawei Push SDK to v5.1.1.301
* Supports CleverTap Android SDK v4.1.0

### Version 1.0.0 (October 1, 2020)
* Initial release! 🎉
* Supports CleverTap Android SDK v4.0.0
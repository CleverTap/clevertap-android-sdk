<p align="center">
  <img src="https://github.com/CleverTap/clevertap-ios-sdk/blob/master/docs/images/clevertap-logo.png" height="220"/>
</p>

# RenderMax by CleverTap

RenderMax SDK delivers and renders notifications on the user's device even if the FCM delivery fails or the device is optimized for battery consumption.

# Table of contents

- [Installation](#installation)
- [Developer Notes](#developer-notes)
- [Proguard](#proguard)
- [Changelog](#changelog)

# Installation

[(Back to top)](#table-of-contents)

To use CleverTap's RenderMax SDK with your app, add the following code snippet in the `build.gradle` of the app.

```groovy
    dependencies {
         implementation "com.clevertap.android:clevertap-rendermax-sdk:1.0.2"
    }
```

# Developer Notes

[(Back to top)](#table-of-contents)

* The RenderMax SDK is supported for Android SDK `v4.6.6`, React Native SDK `v0.9.3`, Flutter SDK `v1.5.5` and above.
* If the app is custom rendering the push notification and not passing the payload to CleverTap SDK, add the following code before you render the notification:

  ```
  CleverTapAPI.processPushNotification(getApplicationContext(),extras);
  ```

# Proguard

[(Back to top)](#table-of-contents)

RenderMax SDK is distributed as obfuscated package and for it's smooth working with proguard you must add necessary rules.
The good news is that, Proguard rules for RenderMax SDK is provided out of the box through `consumer-rules.pro` which gets merged with App's Proguard rule so you don't have to do anything here.
Thanks to `consumer-rules.pro`. You can check RenderMax Proguard rules in your `/build/outputs/mapping/configuration.txt` by searching RenderMax keyword.

# Changelog

[(Back to top)](#table-of-contents)

Changelog can be found [here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTRENDERMAXCHANGELOG.md)
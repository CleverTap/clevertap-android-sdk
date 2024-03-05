# CleverTap Android SDK v4.0.0 Changes

## ⍗ Table of contents

* [Migration](#%EF%B8%8F-migration)
* [Dependencies](#ℹ%EF%B8%8F-dependencies)
    * [Core dependencies](#-core-dependencies)
    * [Firebase dependencies](#-firebase-dependencies)
    * [InApp Notification dependencies](#-inapp-notification-dependencies)
    * [App Inbox dependencies](#-app-inbox-dependencies)
* [Breaking changes](#%EF%B8%8F-breaking-changes)
    * [Firebase Messaging changes](#-firebase-messaging-changes)
    * [Push Notification changes](#-push-notification-changes)
    * [Pull Notifications changes](#-push-amplification-changes)
* [Questions](#-questions)

## ♻️ Migration

CleverTap Android SDK v4.0.0 has migrated to Android 10 & AndroidX libraries, please follow this document to know which AndroidX libraries to include in your app to support CleverTap SDK

## ℹ️ Dependencies

* The overall minSDKVersion for CleverTap Android SDK is 16 (Android 4.1 JellyBean)
* Product A/B Tests (Visual Editor & Dynamic Variables) is now supported for API level 18 (Android 4.3.1 JellyBean ) & above. 
* CleverTap Huawei SDK needs a minSDKVersion of 17 (Android 4.2 JellyBean)

### 💻 Core dependencies

Replace the `support-v4` dependency with `androidx.core` to start using AndroidX dependencies and add `android.useAndroidX=true` in the `gradle.properties` file.

Old Dependency | New Dependency
---:|:---
`implementation 'com.clevertap.android:clevertap-android-sdk:3.9.1'`| `implementation 'com.clevertap.android:clevertap-android-sdk:4.0.0'`
`implementation 'com.android.support:support-v4:28.0.0'` | `implementation 'androidx.core:core:1.3.0'`
`implementation 'com.android.support:support-v4:28.0.0'`| `implementation 'androidx.fragment:fragment:1.1.0'`
`implementation 'com.android.installreferrer:installreferrer:1.0'` | `implementation 'com.android.installreferrer:installreferrer:2.1'`



### 🔥 Firebase dependencies

CleverTap Android SDK v4.0.0 now supports the latest Firebase Cloud Messaging version.

Old Dependency | New Dependency
---:|:---
`implementation 'com.google.firebase:firebase-messaging:17.3.3'` | `implementation 'com.google.firebase:firebase-messaging:20.2.4'`

### 📠 InApp Notification dependencies

InApp Notifications use Fragments which were a part of `support-v4` dependency, which is now replaced with `androidx.fragment` dependency.

Interstitial InApp Notification templates support Audio and Video with the help of ExoPlayer. To enable Audio/Video in your Interstitial InApp Notifications, change the following dependencies in your `build.gradle` file :

Old Dependency | New Dependency
---:|:---
`implementation 'com.android.support:support-v4:28.0.0'` | `implementation 'androidx.fragment:fragment:1.1.0'`
`implementation 'com.google.android.exoplayer:exoplayer:2.8.4'` | `implementation 'com.google.android.exoplayer:exoplayer:2.11.5'` 
`implementation 'com.google.android.exoplayer:exoplayer-hls:2.8.4'` | `implementation 'com.google.android.exoplayer:exoplayer-hls:2.11.5'`
`implementation 'com.google.android.exoplayer:exoplayer-ui:2.8.4'` | `implementation 'com.google.android.exoplayer:exoplayer-ui:2.11.5'`

### 📥 App Inbox dependencies

App Inbox used Recycler View, TabLayout and other classes which were a part of the `support:design` & `appcompat-v7` dependencies, which have now been replaced with multiple separate dependencies.
We have also updated the Glide & Exoplayer dependencies (mentioned above) which is used as a part of App Inbox

Please find the changes in the dependencies for App Inbox here :

Old Dependency | New Dependency
---:|:---
`implementation 'com.android.support:appcompat-v7:28.0.0'` | `implementation 'androidx.appcompat:appcompat:1.2.0'`
`implementation 'com.android.support:design:28.0.0'` | `implementation 'androidx.recyclerview:recyclerview:1.1.0'`
`implementation 'com.android.support:design:28.0.0'` | `implementation 'androidx.viewpager:viewpager:1.0.0'`
`implementation 'com.android.support:design:28.0.0'` | `implementation 'com.google.android.material:material:1.2.1'`
`implementation 'com.github.bumptech.glide:glide:4.9.0'` | `implementation 'com.github.bumptech.glide:glide:4.11.0'`

## ⚒️ Breaking changes

As part of the major release we have introduced a few breaking changes in the CleverTap Android SDK v4.0.0.

These changes were needed to improve the quality and efficiency of the SDK to serve you better.

Below is a list of all the files/package names which have changed as a part of this release

### 🔥 Firebase Messaging changes

We have changed the package name of our `FcmMessageListenerService` class and removed the `FcmTokenListenerService` class as a part of our upgrade to support the latest version of the Firebase Messaging Library.

Old `AndroidManifest.xml` entries

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

New `AndroidManifest.xml` entries

```xml
<service android:name="com.clevertap.android.sdk.pushnotification.fcm.FcmMessageListenerService">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

### 📲 Push Notification changes

We've changed the package name of the following classes -

* `CTNotificationIntentService`
* `CTPushListener`
* `NotificationInfo`

Old `AndroidManifest.xml` entries

```xml
<service
    android:name="com.clevertap.android.sdk.CTNotificationIntentService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.clevertap.PUSH_EVENT" />
    </intent-filter>
</service>
``` 

New `AndroidManifest.xml` entries

```xml
<service
    android:name="com.clevertap.android.sdk.pushnotification.CTNotificationIntentService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.clevertap.PUSH_EVENT" />
    </intent-filter>
</service>
```

`com.clevertap.android.sdk.CTPushListener` has been renamed to `com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener`

`com.clevertap.android.sdk.NotificationInfo` has been renamed to `com.clevertap.android.sdk.pushnotification.NotificationInfo`

### 📲 Pull Notifications changes

The following `AndroidManifest.xml` entries are no longer needed to be added -

```xml
<!--use CTBackgroundIntentService to target users below Android 21 (Lollipop)-->
<service
    android:name="com.clevertap.android.sdk.CTBackgroundIntentService"
    android:exported="false">
        <intent-filter>
            <action android:name="com.clevertap.BG_EVENT"/>
        </intent-filter>
</service>

<!--use CTBackgroundJobService to target users on and above Android 21 (Lollipop)-->
<service
    android:name="com.clevertap.android.sdk.CTBackgroundJobService"
    android:permission="android.permission.BIND_JOB_SERVICE"
    android:exported="false"/>
```

To enable Pull Notifications only the following entry is required in the `AndroidManifest.xml` file -

```xml
<meta-data
    android:name="CLEVERTAP_BACKGROUND_SYNC"
    android:value="1"/>
```

## 🤝 Questions
[(Back to top)](#-table-of-contents)

If your question is not found in FAQ and you have other questions or concerns, you can reach out to the CleverTap support team by raising an issue from the CleverTap Dashboard.



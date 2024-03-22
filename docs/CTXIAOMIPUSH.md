<p align="center">
  <img src="https://github.com/CleverTap/clevertap-ios-sdk/blob/master/docs/images/clevertap-logo.png" height="220"/>
</p>

## ⚠️ Deprecation Notice
> Xiaomi Corporation made a significant announcement recently, notifying users about discontinuing the Mi Push service beyond Mainland China due to operational concerns. You might have already received communication regarding this matter.
Read the official announcement from the Xiaomi Corporation [here](https://dev.mi.com/distribute/doc/details?pId=1555).
With the Mi Push service's closure, CleverTap will cease offering Mi Push support for Xiaomi devices. After the shutdown, Xiaomi devices will still receive push notifications through Firebase Cloud Messaging (FCM).

## 👋 Introduction
[(Back to top)](#-table-of-contents)

CleverTap Xiaomi Push SDK provides an out of the box service to use the Xiaomi Push SDK.

## ®️ Register
[(Back to top)](#-table-of-contents)

The first step to access the Xiaomi cloud push is registered as a Xiaomi developer on the [Xiaomi Website](https://dev.mi.com/console/).

## 🔨 Create an Application
[(Back to top)](#-table-of-contents)

Once you login to the console, click on Create App and enter the required details.

<p align="center">
  <img src="https://files.readme.io/27d3874-Xiaomi_push_Developer_console.png"/>
</p>

## 📱 App Details
[(Back to top)](#-table-of-contents)

Once the App is created on your console, click on the App name to get your Package Name/App ID/ App Key/ App Secret. Among these, the AppID and AppKey are the client’s identity, used when the client SDK initializes; the AppSecret is authenticated for sending a message at the server-side.

<p align="center">
  <img src="https://files.readme.io/ee0d481-Xiaomi_Push_API_Key.png"/>
</p>

Click on [Mi Push Console](http://admin.xmpush.global.xiaomi.com/) and click on Enable Push to enable push services for your app.

<p align="center">
  <img src="https://files.readme.io/14ff6c4-Xiaomi_Push_MI_Console.png"/>
</p>

## 🚀 Integration
[(Back to top)](#-table-of-contents)

* Download the Mi push Library from [here](https://github.com/CleverTap/clevertap-android-sdk/releases/tag/corev4.7.0_xpsv1.5.2_geofencev1.2.0_hmsv1.3.2_ptv1.0.6) and add it in your app's lib folder (`app/libs`)

* If you are using obfuscation for your builds, you might need to add the following lines in proguard rules, as required by [Xiaomi SDK](https://dev.mi.com/console/doc/detail?pId=1244):

```text
#Change xxx.DemoMessageRreceiver to the full class name defined in your app
-keep class xxx.DemoMessageReceiver {*;}

#SDK has been obfuscated and compressed to avoid class not found error due to re-obfuscation.
-keep class com.xiaomi.**

#If the compiling Android version you are using is 23, you can prevent getting a false warning which makes it impossible to compile.
-dontwarn com.xiaomi.push.**

-keep class com.xiaomi.mipush.sdk.MiPushMessage {*;}
-keep class com.xiaomi.mipush.sdk.MiPushCommandMessage {*;}
-keep class com.xiaomi.mipush.sdk.PushMessageReceiver {*;}
-keep class com.xiaomi.mipush.sdk.MessageHandleService {*;}
-keep class com.xiaomi.push.service.XMJobService {*;}
-keep class com.xiaomi.push.service.XMPushService {*;}
-keep class com.xiaomi.mipush.sdk.PushMessageHandler {*;}
-keep class com.xiaomi.push.service.receivers.NetworkStatusReceiver {*;}
-keep class com.xiaomi.push.service.receivers.PingReceiver {*;}
-keep class com.xiaomi.mipush.sdk.NotificationClickedActivity {*;}
```


* Add the CleverTap Xiaomi Push dependency and Mi Push Dependency in your app’s `build.gradle`

```groovy
    implementation "com.clevertap.android:clevertap-xiaomi-sdk:1.5.4"
    implementation fileTree(include: ["*.jar", "*.aar"], dir: "libs")// or implementation files("libs/MiPush_SDK_Client_5_0_6-G_3rd.aar") for including only MiPush_SDK_Client_5_0_6 aar file
```

* Add the following to your app’s `AndroidManifest.xml` file

```xml

<meta-data
    android:name="CLEVERTAP_XIAOMI_APP_KEY"
    android:value="@string/xiaomi_app_key" />

<meta-data
    android:name="CLEVERTAP_XIAOMI_APP_ID"
    android:value="@string/xiaomi_app_id" />

```

* Add the following to your app’s `strings.xml` file

```xml

<string name="xiaomi_app_key">Your Xiaomi App Key</string>
<string name="xiaomi_app_id">Your Xiaomi App ID</string>
 
```

* From CleverTap Android SDK v4.5.0 and CleverTap Xiaomi Push SDK v1.4.0 onwards
    * Method to change credentials for the CleverTap Xiaomi Push SDK `CleverTapAPI.changeXiaomiCredentials(String xiaomiAppID, String xiaomiAppKey)`. This needs to be added before `CleverTapAPI` instance creation.

    * Method to run Xiaomi Push SDK on all devices, Xiaomi only devices or turn off push on all devices.

    ```java

    // possible values are PushConstants.ALL_DEVICES, PushConstants.XIAOMI_MIUI_DEVICES, PushConstants.NO_DEVICES
    // default is PushConstants.ALL_DEVICES
    CleverTapAPI.enableXiaomiPushOn(PushConstants.XIAOMI_MIUI_DEVICES);

    ```

    This needs to be added before `CleverTapAPI` instance creation.
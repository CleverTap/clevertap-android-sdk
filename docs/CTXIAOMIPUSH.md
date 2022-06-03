<p align="center">
  <img src="https://github.com/CleverTap/clevertap-ios-sdk/blob/master/docs/images/clevertap-logo.png" height="220"/>
</p>

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

* Add the CleverTap Xiaomi Push dependency in app’s `build.gradle`

```groovy
    implementation "com.clevertap.android:clevertap-xiaomi-sdk:1.4.0"
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
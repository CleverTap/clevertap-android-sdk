<p align="center">
  <img src="https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/clevertap-logo.png" width="300"/>
</p>

# CleverTap Android SDKs
[![Build Status](https://app.bitrise.io/app/09efc6b9404a6341/status.svg?token=TejL3E1NHyTiR5ajHKGJ6Q)](https://app.bitrise.io/app/09efc6b9404a6341)
[![codebeat badge](https://codebeat.co/badges/49b05fa0-4228-443c-9f1c-d11efa6d2ef8)](https://codebeat.co/projects/github-com-clevertap-clevertap-android-sdk-master)
[ ![Download](https://api.bintray.com/packages/clevertap/Maven/CleverTapAndroidSDK/images/download.svg) ](https://bintray.com/clevertap/Maven/CleverTapAndroidSDK/_latestVersion)

## ⍗ Table of contents
* [Introduction](#-introduction)
* [Installation](#-installation)
	* [Dependencies](#-dependencies)
* [Integration](#-integration)
* [Initialization](#-initialization)
* [Example Usage](#-example-usage)
* [CleverTap Geofence SDK](#-clevertap-geofence-sdk)
* [CleverTap Xiaomi Push SDK](#-clevertap-xiaomi-push-sdk)
* [CleverTap Huawei Push SDK](#-clevertap-huawei-push-sdk)
* [License](#-license)

## 👋 Introduction
[(Back to top)](#-table-of-contents)

The CleverTap Android SDK for Mobile Customer Engagement and Analytics solutions

CleverTap brings together real-time user insights, an advanced segmentation engine, and easy-to-use marketing tools in one mobile marketing platform — giving your team the power to create amazing experiences that deepen customer relationships. Our intelligent mobile marketing platform provides the insights you need to keep users engaged and drive long-term retention and growth.

For more information check out our  [website](https://clevertap.com/ "CleverTap")  and  [documentation](https://developer.clevertap.com/docs/ "CleverTap Technical Documentation").

To get started, sign up [here](https://clevertap.com/live-product-demo/)

## 🎉 Installation
[(Back to top)](#-table-of-contents)

We publish the SDK to `jcenter` and `mavenCentral` as an `AAR` file. Just declare it as dependency in your `build.gradle` file.

```groovy
    dependencies {      
         implementation "com.clevertap.android:clevertap-android-sdk:4.0.1"
    }
```


Alternatively, you can download and add the AAR file included in this repo in your Module libs directory and tell gradle to install it like this:
    
 ```groovy
    dependencies {      
        implementation (name: "clevertap-android-sdk-4.0.1", ext: 'aar')
    }
```


### 📖 Dependencies
[(Back to top)](#-table-of-contents)

Add the Firebase Messaging library and Android Support Library v4 as dependencies to your Module `build.gradle` file.

```groovy
     dependencies {      
         implementation "com.clevertap.android:clevertap-android-sdk:4.0.1"
         implementation "androidx.core:core:1.3.0"
         implementation "com.google.firebase:firebase-messaging:20.2.4"
         implementation "com.google.android.gms:play-services-ads:19.4.0" // Required only if you enable Google ADID collection in the SDK (turned off by default).
         implementation "com.android.installreferrer:installreferrer:2.1" // Mandatory for v3.6.4 and above
     }
```

Also be sure to include the `google-services.json` classpath in your Project level `build.gradle` file:

```groovy
    // Top-level build file where you can add configuration options common to all sub-projects/modules.         
        
    buildscript {       
         repositories {      
             google()
             jcenter()       

             // if you are including the aar file manually in your Module libs directory add this:
             flatDir {
                dirs 'libs'
            }
        
         }       
         dependencies {      
             classpath "com.android.tools.build:gradle:4.0.1" 
             classpath "com.google.gms:google-services:4.3.3"        
        
             // NOTE: Do not place your application dependencies here; they belong       
             // in the individual module build.gradle files      
         }       
    }
```

Add your FCM generated `google-services.json` file to your project and add the following to the end of your `build.gradle`:

```groovy
apply plugin: 'com.google.gms.google-services'
```
Interstitial InApp Notification templates support Audio and Video with the help of ExoPlayer. To enable Audio/Video in your Interstitial InApp Notifications, add the following dependencies in your `build.gradle` file :
    
```groovy
    implementation "com.google.android.exoplayer:exoplayer:2.11.5"
    implementation "com.google.android.exoplayer:exoplayer-hls:2.11.5"
    implementation "com.google.android.exoplayer:exoplayer-ui:2.11.5"
```  

Once you've updated your module `build.gradle` file, make sure you have specified `jcenter()` and `google()` as a repositories in your project `build.gradle` and then sync your project in File -> Sync Project with Gradle Files.

## 🎉 Integration
[(Back to top)](#-table-of-contents)

### Add Your CleverTap Account Credentials
    
Add the following inside the `<application></application>` tags of your AndroidManifest.xml:
   
   ```xml
   <meta-data  
        android:name="CLEVERTAP_ACCOUNT_ID"  
        android:value="Your CleverTap Account ID"/>  
   <meta-data  
        android:name="CLEVERTAP_TOKEN"  
        android:value="Your CleverTap Account Token"/>
   ```

   Replace "Your CleverTap Account ID" and "Your CleverTap Account Token" with actual values from your CleverTap [Dashboard](https://dashboard.clevertap.com) -> Settings -> Integration -> Account ID, SDK's.

### Setup the Lifecycle Callback - **IMPORTANT**

Add the `android:name` property to the `<application>` tag in your AndroidManifest.xml:
    
```xml
    <application
        android:label="@string/app_name"
        android:icon="@drawable/ic_launcher"
        android:name="com.clevertap.android.sdk.Application">
```
    
**Note:** If you've already got a custom Application class, call `ActivityLifecycleCallback.register(this);` before `super.onCreate()` in your custom Application class.
    
**Note:** The above step is extremely important and enables CleverTap to track notification opens, display in-app notifications, track deep links, and other important user behavior.

## 🚀 Initialization
[(Back to top)](#-table-of-contents)

By default the library creates a shared default instance based on the Account ID and Account Token included in your AndroidManifest.xml.   To access this default shared singleton instance in your code call -
```java
CleverTapAPI.getDefaultInstance(context)
```

```java
CleverTapAPI clevertap = CleverTapAPI.getDefaultInstance(getApplicationContext());
```
    
**Creating multiple instances of the SDK**
    
Starting with version 3.2.0 of the SDK, you can create additional CleverTap instances to send data to multiple CleverTap accounts from your app.  To create an additional instance:
    
* Create a CleverTapInstanceConfig object. This object can be created and configured as follows:
    
```java
    CleverTapInstanceConfig clevertapAdditionalInstanceConfig =  CleverTapInstanceConfig.createInstance(context, "ADDITIONAL_CLEVERTAP_ACCOUNT_ID", "ADDITIONAL_CLEVERTAP_ACCOUNT_TOKEN");
    clevertapAdditionalInstanceConfig.setDebugLevel(CleverTapAPI.LogLevel.DEBUG); // default is CleverTapAPI.LogLevel.INFO
    clevertapAdditionalInstanceConfig.setAnalyticsOnly(true); // disables the user engagement features of the instance, default is false
    clevertapAdditionalInstanceConfig.useGoogleAdId(true); // enables the collection of the Google ADID by the instance, default is false
```

* Then to create and subsequently access the additional CleverTap instance, call CleverTapAPI.instanceWithConfig with the CleverTapInstanceConfig object you created. 

```java
CleverTapAPI clevertapAdditionalInstance = CleverTapAPI.instanceWithConfig(clevertapAdditionalInstanceConfig);
```

**Note:**  All configuration to the CleverTapInstanceConfig object must be done prior to calling CleverTapAPI.instanceWithConfig.  Subsequent changes to the CleverTapInstanceConfig object will have no effect on the additional CleverTap instance created.

## 𝌡 Example Usage  
[(Back to top)](#-table-of-contents)
 
See the [usage examples here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/EXAMPLES.md). Also, see the [example project](https://github.com/CleverTap/clevertap-android-sdk/tree/master/sample), included with this repo. 

See our [full documentation here](https://developer.clevertap.com/docs/android) for more information on Events and Profile Tracking, Push Notifications, In-App messages, Install Referrer tracking and app personalization.

## 📍 CleverTap Geofence SDK
[(Back to top)](#-table-of-contents)

CleverTap Android Geofence SDK provides **Geofencing capabilities** to CleverTap Android SDK by using the Play Services Location library.
To find the integration steps for CleverTap Geofence SDK, click [here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTGEOFENCE.md)

## 📲 CleverTap Xiaomi Push SDK
[(Back to top)](#-table-of-contents)

CleverTap Xiaomi Push SDK provides an out of the box service to use the Xiaomi Push SDK. Find the integration steps for the CleverTap Xiaomi Push SDK [here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTXIAOMIPUSH.md)

## 📲 CleverTap Huawei Push SDK
[(Back to top)](#-table-of-contents)

CleverTap Huawei Push SDK provides an out of the box service to use the Huawei Messaging Service. Find the integration steps for the CleverTap Huawei Push SDK [here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTHUAWEIPUSH.md)


## 📄 License
[(Back to top)](#-table-of-contents)
CleverTap Android SDK is MIT licensed, as found in the [LICENSE](https://github.com/CleverTap/clevertap-android-sdk/blob/master/LICENSE) file.

[![CleverTap Logo](http://staging.support.wizrocket.com.s3-website-eu-west-1.amazonaws.com/images/CleverTap_logo.png)](http:www.clevertap.com)

# CleverTap Android SDK  

The CleverTap Android SDK for App Personalization and Engagement  

CleverTap is the next generation app engagement platform. It enables marketers to identify, engage and retain users and provides developers with unprecedented code-level access to build dynamic app experiences for multiple user groups. CleverTap includes out-of-the-box prescriptive campaigns, omni-channel messaging, uninstall data and the industry's largest FREE messaging tier.

For more information check out our [website](https://clevertap.com "CleverTap") and [documentation](http://support.clevertap.com "CleverTap Technical Documentation").

## Getting Started

1. Sign Up

    [Sign up](https://clevertap.com/sign-up) for a free account.  

2.  Install the SDK
### Android Studio / Gradle     
        
    We publish the sdk to jcenter and mavenCentral as an `aar` file. Just declare it as dependency in your `build.gradle` file.     
        
        dependencies {      
            compile 'com.clevertap.android:clevertap-android-sdk:3.1.1'     
        }       
        
   Then add the below Google Play Services (or Firebase Messaging, depending on if you use GCM or FCM) libraries and Android Support Library v4 as dependencies to your Module `build.gradle` file.                 
        
        dependencies {      
            compile 'com.clevertap.android:clevertap-android-sdk:3.1.1'     
            compile 'com.android.support:support-v4:23.4.0+'        
            compile 'com.google.android.gms:play-services-gcm:9.0.2+' // if using GCM, omit if using FCM       
            compile 'com.google.firebase:firebase-messaging:9.0.2+' // if using FCM, omit if using GCM
        }       
        
    Also be sure to include the google-services classpath in your Project level `build.gradle` file:        
        
        
        // Top-level build file where you can add configuration options common to all sub-projects/modules.         
        
        buildscript {       
            repositories {      
                jcenter()       
        
        
            }       
            dependencies {      
                classpath 'com.android.tools.build:gradle:2.1.2'        
                classpath 'com.google.gms:google-services:3.0.0'        
        
                // NOTE: Do not place your application dependencies here; they belong       
                // in the individual module build.gradle files      
            }       
        }       
        
    If using FCM, please add your FCM generated google-services.json file to your project and add the following to the end of your build.gradle:

        apply plugin: 'com.google.gms.google-services'

    Once you've updated your `build.gradle` file, make sure you have specified jcenter() or mavenCentral() as a repository in your `build.gradle` and then sync your project in Tools -> Android -> Sync Project With Gradle Files.     
 

    ### Manual Install

    Copy the included CleverTapAndroidSDK.jar file to your projects libs directory. Add this JAR file as a dependency for your Android app project.

3. Add Your CleverTap Account Credentials

    add the following inside the `<application></application>` tags of your AndroidManifest.xml:  
    
        <meta-data  
            android:name="CLEVERTAP_ACCOUNT_ID"  
            android:value="Your CleverTap Account ID"/>  
        <meta-data  
            android:name="CLEVERTAP_TOKEN"  
            android:value="Your CleverTap Account Token"/>

    Replace "Your CleverTap Account ID" and "Your CleverTap Account Token" with actual values from your CleverTap [Dashboard](https://dashboard.clevertap.com) -> Settings -> Integration -> Account ID, SDK's.

4.  Setup the Lifecycle Callback - **IMPORTANT**

    Add the "android:name" property to the `<application>` tag:

        <application
            android:label="@string/app_name"
            android:icon="@drawable/ic_launcher"
            android:name="com.clevertap.android.sdk.Application">

    Note: If you've already got a custom Application class, call `ActivityLifecycleCallback.register(this);` before `super.onCreate()` in your custom Application class.

    Note: The above step is extremely important and enables CleverTap to track notification opens, display in-app notifications, track deep links, and other important user behavior.

5.  Initialize the Library

    The instance returned by `CleverTapAPI.getInstance()` is the same on every call.  In the onCreate() of your main activity:

        CleverTapAPI ct;
        try {
          ct = CleverTapAPI.getInstance(getApplicationContext());
        } catch (CleverTapMetaDataNotFoundException e) {
          // handle appropriately
        } catch (CleverTapPermissionsNotSatisfied e) {
          // handle appropriately
        }

    CleverTapMetaDataNotFoundException is thrown when you haven't specified your CleverTap Account ID and/or the Account Token in your AndroidManifest.xml. CleverTapPermissionsNotSatisfied is thrown when you haven't requested the required permissions in your AndroidManifest.xml

6.  Checkout the Full Documentation

    Please see our [full documentation here](https://support.clevertap.com/integration/android-sdk/) for more information on permissions, as well as configuration for Push Notifications, In-app messages, install referrer tracking and app personalization.

## Example Usage
Checkout the example StarterProject.


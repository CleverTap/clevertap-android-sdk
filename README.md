[![CleverTap Logo](http://staging.support.wizrocket.com.s3-website-eu-west-1.amazonaws.com/images/CleverTap_logo.png)](http:www.clevertap.com)

# CleverTap Android SDK  

The CleverTap Android SDK for App Personalization and Engagement  

CleverTap is the next generation app engagement platform. It enables marketers to identify, engage and retain users and provides developers with unprecedented code-level access to build dynamic app experiences for multiple user groups. CleverTap includes out-of-the-box prescriptive campaigns, omni-channel messaging, uninstall data and the industry's largest FREE messaging tier.

For more information check out our [website](http://www.clevertap.com "CleverTap") and [documentation](http://support.wizrocket.com "CleverTap Technical Documentation").

## Getting Started

1. Sign Up

    [Sign up](http://www.clevertap.com/sign-up.html) for a free account.  

2.  Install the SDK

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

    The CleverTapMetaDataNotFoundException is thrown when you haven't specified your CleverTap Account ID and/or the Account Token in your AndroidManifest.xml. The CleverTapPermissionsNotSatisfiedException is thrown when you haven't requested the required permissions in your AndroidManifest.xml

6.  Checkout the Full Documentation

    Please see our [full documentation here](https://support.clevertap.com/integration/android-sdk/) for more information on permissions, as well as configuration for Push Notifications, In-app messages, install referrer tracking and app personalization.

## Example Usage
Checkout the example StarterProject.


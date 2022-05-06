package com.clevertap.android.sdk.validation

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class ManifestValidatorTest :BaseTestCase(){
//    Manifest validator only generates logs using a static logger, so it can't be tested for its working.
//    ----  --------  ----  ------  --------  ----  ------  --------  ----  ------  --------  ----  ----

//    lateinit var ctx: Context
//    lateinit var config: CleverTapInstanceConfig
//    lateinit var deviceInfo: DeviceInfo
//
//    private val validationResultStack = ValidationResultStack()
//    lateinit var pushProviders: PushProviders
//    private val ourApplicationClassName = "com.clevertap.android.sdk.Application"
//    override fun setUp() {
//        super.setUp()
//        ctx = appCtx
//        deviceInfo = Mockito.mock(DeviceInfo::class.java)
//        pushProviders = Mockito.mock(PushProviders::class.java)
//
//        val logger = Mockito.mockStatic(Logger::class.java)
//        logger.reset()
//
//
//        "case1,2".let {
//            //1. when application does not have internet permission , following log is generated : Missing Permission: android.permission.INTERNET"
//            //2. when application does have internet permission , log is not generated
//
//            // there is no way to test this, so skipping it.
//            assumptions {}
//            ManifestValidator.validate(ctx,deviceInfo,pushProviders)
//            verificationAndAssertions {}
//        }
//
//        "case_all".let {//checkSDKVersion(deviceInfo);
//            // deviceInfo.getSdkVersion() is getting called to generate an sdk version log
//        }
//
//        "case3".let {//validationApplicationLifecyleCallback(context);
//            //3: if  ActivityLifecycleCallback.registered is false  && CleverTapAPI.isAppForeground() is false,
//            //   then private function checkApplicationClass() is called, else no change
//            //-----3.1  if appName(from context) == null OR appName.isEmpty() , then log is generated
//            //-----3.2 if appName == "com.clevertap.android.sdk.Application" , another log is generated
//            //-----3.3 otherwise , another log is generated
//            //
//            assumptions {  }
//            ManifestValidator.validate(ctx,deviceInfo,pushProviders)
//            verificationAndAssertions {  }
//        }
//
//        "case4".let { //checkReceiversServices(context, pushProviders);
//            //4: if the app has specific classes (CTGeofenceReceiver,CTLocationUpdateReceiver,CTGeofenceBootReceiver, etc,) then it will generate specific logs
//            assumptions {  }
//            ManifestValidator.validate(ctx,deviceInfo,pushProviders)
//            verificationAndAssertions {  }
//        }
//
//
//        "case 5".let { //if (!TextUtils.isEmpty(ManifestInfo.getInstance(context).getFCMSenderId())){
//            // if fcm sender id is empty (via ManifestInfo.getInstance(context).getFCMSenderId()), then a log is generated
//        }
//    }
}
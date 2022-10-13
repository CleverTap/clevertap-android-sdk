@file:JvmName("CTXtensions")

package com.clevertap.android.sdk

import android.content.Context
import android.os.Build.VERSION

fun Context.isPackageAndOsTargetsAbove(apiLevel: Int) =
    VERSION.SDK_INT > apiLevel && targetSdkVersion > apiLevel

val Context.targetSdkVersion
    get() = applicationContext.applicationInfo.targetSdkVersion
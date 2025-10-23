package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.cryption.ICryptHandler
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.validation.ValidationResultStack

/**
 * Core infrastructure used by all features
 * Contains fundamental components like context, config, device info, executors, etc.
 */
internal data class CoreFeature(
    val context: Context,
    val config: CleverTapInstanceConfig,
    val deviceInfo: DeviceInfo,
    val coreMetaData: CoreMetaData,
    val executors: CTExecutors,
    val mainLooperHandler: MainLooperHandler,
    val validationResultStack: ValidationResultStack,
    val cryptHandler: ICryptHandler,
    val clock: Clock
)

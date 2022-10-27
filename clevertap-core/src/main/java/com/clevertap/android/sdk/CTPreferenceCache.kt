package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.task.CTExecutorFactory

class CTPreferenceCache {

    fun isFirstTimeRequest() = firstTimeRequest
    fun setFirstTimeRequest(fTR : Boolean) {
        firstTimeRequest = fTR
    }

    companion object {

        @Volatile
        private var INSTANCE: CTPreferenceCache? = null

        @JvmField
        var firstTimeRequest = true

        @JvmStatic
        fun getInstance(context: Context, config: CleverTapInstanceConfig): CTPreferenceCache =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildCache(context, config).also { INSTANCE = it }
            }

        private fun buildCache(context: Context, config: CleverTapInstanceConfig): CTPreferenceCache {
            CTExecutorFactory.executors(config).ioTask<Void>().execute("buildCache") {
                firstTimeRequest = StorageHelper.getBoolean(
                    context,
                    InAppController.IS_FIRST_TIME_PERMISSION_REQUEST, true
                )
                null
            }
            return CTPreferenceCache()
        }

        @JvmStatic
        fun updateCacheToDisk(context: Context, config: CleverTapInstanceConfig) {
            CTExecutorFactory.executors(config).ioTask<Void>().execute("updateCacheToDisk") {
                StorageHelper.putBooleanImmediate(
                    context, InAppController.IS_FIRST_TIME_PERMISSION_REQUEST,
                    firstTimeRequest
                )
                null
            }
        }
    }
}
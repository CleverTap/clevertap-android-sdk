package com.clevertap.android.sdk.db.dao

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface UninstallTimestampDAO {
    @WorkerThread
    fun storeUninstallTimestamp()
    
    @WorkerThread
    fun getLastUninstallTimestamp(): Long
}

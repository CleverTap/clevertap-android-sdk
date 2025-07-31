package com.clevertap.android.sdk.db.dao

import androidx.annotation.WorkerThread

interface UninstallTimestampDAO {
    @WorkerThread
    fun storeUninstallTimestamp()
    
    @WorkerThread
    fun getLastUninstallTimestamp(): Long
}

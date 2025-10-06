package com.clevertap.android.sdk.db.dao

import androidx.annotation.WorkerThread

interface PushNotificationDAO {
    @WorkerThread
    fun storePushNotificationId(id: String, ttl: Long)
    
    @WorkerThread
    fun fetchPushNotificationIds(): Array<String>
    
    @WorkerThread
    fun doesPushNotificationIdExist(id: String): Boolean
    
    @WorkerThread
    fun updatePushNotificationIds(ids: Array<String>)
    
    @WorkerThread
    fun cleanUpPushNotifications()
}

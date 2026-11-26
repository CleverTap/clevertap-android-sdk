package com.clevertap.android.sdk.db.dao

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface PushNotificationDAO {
    @WorkerThread
    fun storePushNotificationId(id: String, ttlInSeconds: Long)
    
    @WorkerThread
    fun fetchPushNotificationIds(): Array<String>
    
    @WorkerThread
    fun doesPushNotificationIdExist(id: String): Boolean
    
    @WorkerThread
    fun updatePushNotificationIds(ids: Array<String>)
    
    @WorkerThread
    fun cleanUpPushNotifications()
}

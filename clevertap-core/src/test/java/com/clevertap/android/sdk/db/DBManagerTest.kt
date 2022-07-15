package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DBManagerTest: BaseTestCase() {

    private lateinit var dbManager:DBManager
    private lateinit var instanceConfig: CleverTapInstanceConfig
    private lateinit var lockManager:CTLockManager
    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx,"accountId","accountToken")
        lockManager = CTLockManager()
        dbManager = DBManager(instanceConfig,lockManager)
    }

    @Test
    fun loadDBAdapter() {
    }

    @Test
    fun clearQueues() {
    }

    @Test
    fun getPushNotificationViewedQueuedEvents(): Unit {
    }

    @Test
    fun getQueueCursor(): Unit {
    }

    @Test
    fun getQueuedDBEvents(): Unit {
    }

    @Test
    fun getQueuedEvents(): Unit {

    }

    @Test
    fun queueEventToDB() {
    }

    @Test
    fun queuePushNotificationViewedEventToDB() {
    }

    @Test
    fun updateCursorForDBObject() {
    }
}
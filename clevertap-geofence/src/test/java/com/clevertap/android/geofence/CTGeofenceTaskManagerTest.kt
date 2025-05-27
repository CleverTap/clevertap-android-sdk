package com.clevertap.android.geofence

import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.geofence.interfaces.CTGeofenceTask.OnCompleteListener
import org.awaitility.Awaitility.await
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.Future

class CTGeofenceTaskManagerTest : BaseTestCase() {

    @Test
    fun testGetInstance() {
        val instance = CTGeofenceTaskManager.getInstance()
        Assert.assertNotNull(instance)

        val instance1 = CTGeofenceTaskManager.getInstance()
        Assert.assertSame(instance, instance1)
    }

    @Test
    fun testPostAsyncSafelyRunnable() {

        val isFinish = arrayOf(false)
        val future = CTGeofenceTaskManager.getInstance().postAsyncSafely("") { isFinish[0] = true }

        await().until { isFinish[0] }

        Assert.assertNotNull(future)
    }

    @Test
    fun testPostAsyncSafelyRunnableFlatCall() {

        // when called multiple times from same thread

        val isFinish = arrayOf(false, false)
        val flatFuture = arrayOfNulls<Future<*>?>(2)
        flatFuture[0] =
            CTGeofenceTaskManager.getInstance().postAsyncSafely("") { isFinish[0] = true }

        flatFuture[1] =
            CTGeofenceTaskManager.getInstance().postAsyncSafely("nested") { isFinish[1] = true }

        await().until { isFinish[0] && isFinish[1] }

        Assert.assertNotNull(flatFuture[0])
        Assert.assertNotNull(flatFuture[1])
    }

    @Test
    fun testPostAsyncSafelyRunnableNestedCall() {

        // when called multiple times from same thread

        val isFinish = arrayOf(false)
        val nestedFuture = arrayOfNulls<Future<*>?>(1)
        val future = CTGeofenceTaskManager.getInstance().postAsyncSafely("") {
            nestedFuture[0] =
                CTGeofenceTaskManager.getInstance().postAsyncSafely("nested") { isFinish[0] = true }
        }

        await().until { isFinish[0] }

        Assert.assertNotNull(future)
        Assert.assertNull(nestedFuture[0])
    }

    @Test
    fun testPostAsyncSafelyTask() {

        val isFinish = arrayOf(false, false)
        val future = CTGeofenceTaskManager.getInstance().postAsyncSafely(
            "", object : CTGeofenceTask {
                override fun execute() {
                    isFinish[0] = true
                }

                override fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
                    // no-op
                }
            })

        await().until { isFinish[0] }

        Assert.assertNotNull(future)
    }

    @Test
    fun testPostAsyncSafelyTaskFlatCall() {

        // when called multiple times from same thread

        val isFinish = arrayOf(false, false)
        val flatFuture = arrayOfNulls<Future<*>?>(2)
        flatFuture[0] =
            CTGeofenceTaskManager.getInstance().postAsyncSafely("", object : CTGeofenceTask {
                override fun execute() {
                    isFinish[0] = true
                }

                override fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
                    // no-op
                }

            })

        flatFuture[1] =
            CTGeofenceTaskManager.getInstance().postAsyncSafely("nested", object : CTGeofenceTask {
                override fun execute() {
                    isFinish[1] = true
                }

                override fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
                    // no-op
                }
            })

        await().until { isFinish[0] && isFinish[1] }

        Assert.assertNotNull(flatFuture[0])
        Assert.assertNotNull(flatFuture[1])
    }

    @Test
    fun testPostAsyncSafelyTaskNestedCall() {

        // when called multiple times from same thread
        val isFinish = arrayOf(false)
        val nestedFuture = arrayOfNulls<Future<*>?>(1)
        val future = CTGeofenceTaskManager.getInstance().postAsyncSafely(
            "", object : CTGeofenceTask {
                override fun execute() {

                    nestedFuture[0] = CTGeofenceTaskManager.getInstance()
                        .postAsyncSafely("nested", object : CTGeofenceTask {
                            override fun execute() {
                                isFinish[0] = true
                            }

                            override fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
                                // no-op
                            }
                        })

                }

                override fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
                    // no-op
                }
            })

        await().until { isFinish[0] }

        Assert.assertNotNull(future)
        Assert.assertNull(nestedFuture[0])
    }

    @Test
    fun testPostAsyncSafelyTaskRunnableNestedCall() {

        // when task and runnable called from same thread

        val isFinish = arrayOf(false)
        val nestedFuture = arrayOfNulls<Future<*>?>(1)
        val future = CTGeofenceTaskManager.getInstance().postAsyncSafely(
            "", object : CTGeofenceTask {
                override fun execute() {

                    nestedFuture[0] = CTGeofenceTaskManager.getInstance().postAsyncSafely(
                        "nested"
                    ) { isFinish[0] = true }
                }

                override fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
                    // no-op
                }
            })

        await().until { isFinish[0] }

        Assert.assertNotNull(future)
        Assert.assertNull(nestedFuture[0])
    }
}

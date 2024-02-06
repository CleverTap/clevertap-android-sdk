package com.clevertap.android.geofence;

import static org.awaitility.Awaitility.await;

import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import java.util.concurrent.Future;
import org.junit.*;
public class CTGeofenceTaskManagerTest extends BaseTestCase {


    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testGetInstance() {
        CTGeofenceTaskManager instance = CTGeofenceTaskManager.getInstance();
        Assert.assertNotNull(instance);

        CTGeofenceTaskManager instance1 = CTGeofenceTaskManager.getInstance();
        Assert.assertSame(instance, instance1);
    }

    @Test
    public void testPostAsyncSafelyRunnable() {

        final boolean[] isFinish = {false};
        Future<?> future = CTGeofenceTaskManager.getInstance().postAsyncSafely("", () -> isFinish[0] = true);

        await().until(() -> isFinish[0]);

        Assert.assertNotNull(future);
    }

    @Test
    public void testPostAsyncSafelyRunnableFlatCall() {

        // when called multiple times from same thread

        final boolean[] isFinish = {false, false};
        final Future<?>[] flatFuture = {null, null};
        flatFuture[0] = CTGeofenceTaskManager.getInstance().postAsyncSafely("", () -> isFinish[0] = true);

        flatFuture[1] = CTGeofenceTaskManager.getInstance().postAsyncSafely("nested", () -> isFinish[1] = true);

        await().until(() -> isFinish[0] && isFinish[1]);

        Assert.assertNotNull(flatFuture[0]);
        Assert.assertNotNull(flatFuture[1]);
    }

    @Test
    public void testPostAsyncSafelyRunnableNestedCall() {

        // when called multiple times from same thread

        final boolean[] isFinish = {false};
        final Future<?>[] nestedFuture = {null};
        Future<?> future = CTGeofenceTaskManager.getInstance().postAsyncSafely("",
                () -> nestedFuture[0] = CTGeofenceTaskManager.getInstance().postAsyncSafely("nested",
                        () -> isFinish[0] = true));

        await().until(() -> isFinish[0]);

        Assert.assertNotNull(future);
        Assert.assertNull(nestedFuture[0]);
    }

    @Test
    public void testPostAsyncSafelyTask() {

        final boolean[] isFinish = {false};
        Future<?> future = CTGeofenceTaskManager.getInstance().postAsyncSafely("", new CTGeofenceTask() {
            @Override
            public void execute() {
                isFinish[0] = true;
            }

            @Override
            public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
                // no-op
            }
        });

        await().until(() -> isFinish[0]);

        Assert.assertNotNull(future);
    }

    @Test
    public void testPostAsyncSafelyTaskFlatCall() {

        // when called multiple times from same thread

        final boolean[] isFinish = {false, false};
        final Future<?>[] flatFuture = {null, null};
        flatFuture[0] = CTGeofenceTaskManager.getInstance().postAsyncSafely("", new CTGeofenceTask() {
            @Override
            public void execute() {
                isFinish[0] = true;
            }

            @Override
            public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
                // no-op
            }

        });

        flatFuture[1] = CTGeofenceTaskManager.getInstance().postAsyncSafely("nested", new CTGeofenceTask() {
            @Override
            public void execute() {
                isFinish[1] = true;
            }

            @Override
            public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
                // no-op
            }
        });

        await().until(() -> isFinish[0] && isFinish[1]);

        Assert.assertNotNull(flatFuture[0]);
        Assert.assertNotNull(flatFuture[1]);
    }

    @Test
    public void testPostAsyncSafelyTaskNestedCall() {

        // when called multiple times from same thread

        final boolean[] isFinish = {false};
        final Future<?>[] nestedFuture = {null};
        Future<?> future = CTGeofenceTaskManager.getInstance().postAsyncSafely("", new CTGeofenceTask() {
            @Override
            public void execute() {

                nestedFuture[0] = CTGeofenceTaskManager.getInstance().postAsyncSafely("nested", new CTGeofenceTask() {
                    @Override
                    public void execute() {
                        isFinish[0] = true;
                    }

                    @Override
                    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
                        // no-op
                    }
                });

            }

            @Override
            public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
                // no-op
            }
        });

        await().until(() -> isFinish[0]);

        Assert.assertNotNull(future);
        Assert.assertNull(nestedFuture[0]);
    }

    @Test
    public void testPostAsyncSafelyTaskRunnableNestedCall() {

        // when task and runnable called from same thread

        final boolean[] isFinish = {false};
        final Future<?>[] nestedFuture = {null};
        Future<?> future = CTGeofenceTaskManager.getInstance().postAsyncSafely("", new CTGeofenceTask() {
            @Override
            public void execute() {

                nestedFuture[0] = CTGeofenceTaskManager.getInstance().postAsyncSafely("nested",
                        () -> isFinish[0] = true);

            }

            @Override
            public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
                // no-op
            }
        });

        await().until(() -> isFinish[0]);

        Assert.assertNotNull(future);
        Assert.assertNull(nestedFuture[0]);
    }

}

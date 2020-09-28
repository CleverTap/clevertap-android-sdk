package com.clevertap.android.geofence;

import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.clevertap.android.geofence.interfaces.CTLocationAdapter;
import com.clevertap.android.geofence.interfaces.CTLocationCallback;
import com.clevertap.android.geofence.interfaces.CTLocationUpdatesListener;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Future;

/**
 * A {@link ListenableWorker} which fetches last known location from OS in foreground as well as in background
 * and in killed state. This will be active when location fetch mode set by client is
 * {@link CTGeofenceSettings#FETCH_LAST_LOCATION_PERIODIC}<br>
 * Frequency of location updates depends on {@link CTGeofenceSettings.Builder#setInterval(long)}
 */
public class BackgroundLocationWork extends ListenableWorker {

    public BackgroundLocationWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Fetches last known location from OS and delivers it to APP through {@link CTLocationUpdatesListener}
     * <br>
     * Fetched Location will be passed to {@link CTGeofenceAPI#processTriggeredLocation(Location)} to send it to
     * server for latest geofence list
     */
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Handling work in BackgroundLocationWork...");

        ListenableFuture<Result> listenableFuture = CallbackToFutureAdapter
                .getFuture(new CallbackToFutureAdapter.Resolver<Result>() {
                    @Nullable
                    @Override
                    public Object attachCompleter(@NonNull final CallbackToFutureAdapter.Completer<Result> completer)
                            throws Exception {

                        final CTLocationCallback ctLocationCallback = new CTLocationCallback() {
                            @Override
                            public void onLocationComplete(Location location) {

                                // running on bg thread

                                CleverTapAPI cleverTapApi = CTGeofenceAPI.getInstance(getApplicationContext())
                                        .getCleverTapApi();
                                Future<?> future = null;

                                if (cleverTapApi != null && location != null) {
                                    future = CTGeofenceAPI.getInstance(getApplicationContext())
                                            .processTriggeredLocation(location);
                                }

                                try {
                                    Utils.notifyLocationUpdates(getApplicationContext(), location);

                                    if (future != null) {
                                        future.get();
                                    }
                                } catch (Exception e) {
                                    CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                            "Exception while processing geofence receiver intent");
                                    if (cleverTapApi != null) {
                                        cleverTapApi.pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                                                "Exception while processing geofence receiver intent");
                                    }
                                    e.printStackTrace();
                                }

                                completer.set(Result.success());

                                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                        "BackgroundLocationWork is finished");
                            }
                        };

                        CTGeofenceTaskManager.getInstance().postAsyncSafely("ProcessLocationWork",
                                new Runnable() {
                                    @Override
                                    public void run() {

                                        if (!Utils.initCTGeofenceApiIfRequired(getApplicationContext())) {
                                            // if init fails then return without doing any work
                                            completer.set(Result.success());
                                            return;
                                        }

                                        CTLocationAdapter ctLocationAdapter = CTGeofenceAPI
                                                .getInstance(getApplicationContext()).getCtLocationAdapter();

                                        if (ctLocationAdapter != null) {
                                            ctLocationAdapter.getLastLocation(ctLocationCallback);
                                        } else {
                                            completer.set(Result.success());
                                        }

                                    }
                                });

                        return ctLocationCallback;
                    }
                });

        return listenableFuture;

    }

}

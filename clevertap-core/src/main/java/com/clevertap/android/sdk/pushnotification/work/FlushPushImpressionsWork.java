package com.clevertap.android.sdk.pushnotification.work;

import static android.content.Context.BATTERY_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.events.EventGroup;
import com.clevertap.android.sdk.events.EventQueueManager;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import java.util.HashMap;

public class FlushPushImpressionsWork extends Worker {

    public static final String TAG = "FlushPushImpressionsWork";

    public FlushPushImpressionsWork(@NonNull final Context context,
            @NonNull final WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public Result doWork() {

        Logger.i(TAG,"starting FlushPushImpressionsWork...");

        Context applicationContext = getApplicationContext();
        for (CleverTapAPI instance : CleverTapAPI.getAvailableInstances(applicationContext)) {
            if (instance == null || instance.getCoreState().getConfig().isAnalyticsOnly()) {
                Logger.d("Instance is either null or Analytics Only not flushing push impressions!");
                continue;
            }
            if (checkIfStopped()) return Result.success();

            Logger.i(TAG,"Flushing queue for push impressions on ct instance = "+instance);
            instance.getCoreState().getBaseEventQueueManager().flushQueueSync(applicationContext,
                    EventGroup.PUSH_NOTIFICATION_VIEWED, Constants.D_SRC_PI_WM);
            BatteryManager bm = (BatteryManager) applicationContext.getSystemService(BATTERY_SERVICE);
            int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

            HashMap<String,Object> map = new HashMap<>();
            map.put("time",System.currentTimeMillis()/1000);
            map.put("batteryLevel", batLevel);

            if (checkIfStopped()) return Result.success();

            instance.pushEvent("Work manager run success",map);
        }
        return Result.success();
    }

    private boolean checkIfStopped() {
        if (isStopped()) {
            Logger.v(TAG,"Worker stopped!");
        }
        return isStopped();
    }

}

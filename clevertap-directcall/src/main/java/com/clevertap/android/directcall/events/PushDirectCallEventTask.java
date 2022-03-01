package com.clevertap.android.directcall.events;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;

import android.content.Context;
import androidx.annotation.WorkerThread;

import com.clevertap.android.directcall.Constants;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.utils.Utils;
import org.json.JSONObject;
import java.util.concurrent.Future;

public class PushDirectCallEventTask implements CTDirectCallTask {

    private final Context context;
    private CTSystemEvent systemEvent;
    private JSONObject eventProperties;

    public PushDirectCallEventTask(Context context, CTSystemEvent systemEvent, JSONObject eventProperties) {
        this.context = context;
        this.systemEvent = systemEvent;
        this.eventProperties = eventProperties;
    }

    @WorkerThread
    @Override
    public void execute() {
        DirectCallAPI.getLogger().debug(Constants.CALLING_LOG_TAG_SUFFIX,
                "Executing PushDirectCallEventTask...");

        String accountId = StorageHelper.getString(context, Constants.KEY_CLEVERTAP_ACCOUNT_ID, null);
        if (!Utils.getInstance().initCleverTapApiIfRequired(context, accountId)) {
            // if init fails then return without doing any work
            return;
        }

        pushDirectCallEvents(systemEvent, eventProperties);

    }

    @WorkerThread
    private void pushDirectCallEvents(CTSystemEvent eventName, JSONObject eventProperties) {
        try {
            CleverTapAPI cleverTapApi = DirectCallAPI.getInstance().getCleverTapApi();

            if (cleverTapApi == null) {
                DirectCallAPI.getLogger().debug(
                        CALLING_LOG_TAG_SUFFIX,
                        "cleverTapAPI instance can't be null to initialize the SDK"
                );
                return;
            }

            Future<?> future;
            // send event to CleverTap SDK
            future = cleverTapApi.pushDirectCallEvent(eventName.getName(), eventProperties);

            DirectCallAPI.getLogger().debug(Constants.CALLING_LOG_TAG_SUFFIX,
                    "Calling future for directCall event name = " +
                            eventName.getName());
            future.get();

            DirectCallAPI.getLogger().debug(Constants.CALLING_LOG_TAG_SUFFIX,
                    "Finished calling future for directCall event name = " +
                            eventName.getName());
        } catch (Exception e) {
           DirectCallAPI.getLogger().debug(Constants.CALLING_LOG_TAG_SUFFIX,
                    "Exception while raising DC system event: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}

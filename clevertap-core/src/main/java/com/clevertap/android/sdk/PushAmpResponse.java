package com.clevertap.android.sdk;

import static android.content.Context.JOB_SCHEDULER_SERVICE;
import static com.clevertap.android.sdk.CTJsonConverter.getRenderedTargetList;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.RequiresApi;
import com.clevertap.android.sdk.pushnotification.amp.CTBackgroundIntentService;
import com.clevertap.android.sdk.pushnotification.amp.CTBackgroundJobService;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class PushAmpResponse extends CleverTapResponse {

    private final Object inboxControllerLock;

    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final DBAdapter mDBAdapter;

    private final Logger mLogger;

    PushAmpResponse(CleverTapResponse cleverTapResponse) {
        mCleverTapResponse = cleverTapResponse;
        CoreState coreState = getCoreState();
        mConfig = coreState.getConfig();
        mLogger = mConfig.getLogger();
        inboxControllerLock = coreState.getCTLockManager().getInboxControllerLock();
        mDBAdapter = coreState.getDatabaseManager().loadDBAdapter(coreState.getContext());

    }

    @Override
    void processResponse(final JSONObject response, final String stringBody, final Context context) {
        //Handle Push Amplification response
        if (mConfig.isAnalyticsOnly()) {
            mLogger.verbose(mConfig.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing push amp response");

            // process Display Unit response
            mCleverTapResponse.processResponse(response, stringBody, context);

            return;
        }
        try {
            if (response.has("pushamp_notifs")) {
                mLogger.verbose(mConfig.getAccountId(), "Processing pushamp messages...");
                JSONObject pushAmpObject = response.getJSONObject("pushamp_notifs");
                final JSONArray pushNotifications = pushAmpObject.getJSONArray("list");
                if (pushNotifications.length() > 0) {
                    mLogger.verbose(mConfig.getAccountId(), "Handling Push payload locally");
                    handlePushNotificationsInResponse(pushNotifications);
                }
                if (pushAmpObject.has("pf")) {
                    try {
                        int frequency = pushAmpObject.getInt("pf");
                        updatePingFrequencyIfNeeded(context, frequency);
                    } catch (Throwable t) {
                        mLogger
                                .verbose("Error handling ping frequency in response : " + t.getMessage());
                    }

                }
                if (pushAmpObject.has("ack")) {
                    boolean ack = pushAmpObject.getBoolean("ack");
                    mLogger.verbose("Received ACK -" + ack);
                    if (ack) {
                        JSONArray rtlArray = getRenderedTargetList(mDBAdapter);
                        String[] rtlStringArray = new String[0];
                        if (rtlArray != null) {
                            rtlStringArray = new String[rtlArray.length()];
                        }
                        for (int i = 0; i < rtlStringArray.length; i++) {
                            rtlStringArray[i] = rtlArray.getString(i);
                        }
                        mLogger.verbose("Updating RTL values...");
                        mDBAdapter.updatePushNotificationIds(rtlStringArray);
                    }
                }
            }
        } catch (Throwable t) {
            //Ignore
        }

        // process Display Unit response
        mCleverTapResponse.processResponse(response, stringBody, context);
    }

    private void createAlarmScheduler(Context context) {
        int pingFrequency = getPingFrequency(context);
        if (pingFrequency > 0) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(CTBackgroundIntentService.MAIN_ACTION);
            intent.setPackage(context.getPackageName());
            PendingIntent alarmPendingIntent = PendingIntent
                    .getService(context, mConfig.getAccountId().hashCode(), intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            if (alarmManager != null) {
                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
                        Constants.ONE_MIN_IN_MILLIS * pingFrequency, alarmPendingIntent);
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createOrResetJobScheduler(Context context) {

        int existingJobId = StorageHelper.getInt(context, Constants.PF_JOB_ID, -1);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        //Disable push amp for devices below Api 26
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (existingJobId >= 0) {//cancel already running job
                jobScheduler.cancel(existingJobId);
                StorageHelper.putInt(context, Constants.PF_JOB_ID, -1);
            }

            mLogger.debug(mConfig.getAccountId(), "Push Amplification feature is not supported below Oreo");
            return;
        }

        if (jobScheduler == null) {
            return;
        }
        int pingFrequency = getPingFrequency(context);

        if (existingJobId < 0 && pingFrequency < 0) {
            return; //no running job and nothing to create
        }

        if (pingFrequency < 0) { //running job but hard cancel
            jobScheduler.cancel(existingJobId);
            StorageHelper.putInt(context, Constants.PF_JOB_ID, -1);
            return;
        }

        ComponentName componentName = new ComponentName(context, CTBackgroundJobService.class);
        boolean needsCreate = (existingJobId < 0 && pingFrequency > 0);

        //running job, no hard cancel so check for diff in ping frequency and recreate if needed
        JobInfo existingJobInfo = getJobInfo(existingJobId, jobScheduler);
        if (existingJobInfo != null
                && existingJobInfo.getIntervalMillis() != pingFrequency * Constants.ONE_MIN_IN_MILLIS) {
            jobScheduler.cancel(existingJobId);
            StorageHelper.putInt(context, Constants.PF_JOB_ID, -1);
            needsCreate = true;
        }

        if (needsCreate) {
            int jobid = mConfig.getAccountId().hashCode();
            JobInfo.Builder builder = new JobInfo.Builder(jobid, componentName);
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            builder.setRequiresCharging(false);

            builder.setPeriodic(pingFrequency * Constants.ONE_MIN_IN_MILLIS, 5 * Constants.ONE_MIN_IN_MILLIS);
            builder.setRequiresBatteryNotLow(true);

            if (Utils.hasPermission(context, "android.permission.RECEIVE_BOOT_COMPLETED")) {
                builder.setPersisted(true);
            }

            JobInfo jobInfo = builder.build();
            int resultCode = jobScheduler.schedule(jobInfo);
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                Logger.d(mConfig.getAccountId(), "Job scheduled - " + jobid);
                StorageHelper.putInt(context, Constants.PF_JOB_ID, jobid);
            } else {
                Logger.d(mConfig.getAccountId(), "Job not scheduled - " + jobid);
            }
        }
    }

    //Session

    private int getPingFrequency(Context context) {
        return StorageHelper.getInt(context, Constants.PING_FREQUENCY,
                Constants.PING_FREQUENCY_VALUE); //intentional global key because only one Job is running
    }

    //PN
    private void handlePushNotificationsInResponse(JSONArray pushNotifications) {
        try {
            for (int i = 0; i < pushNotifications.length(); i++) {
                Bundle pushBundle = new Bundle();
                JSONObject pushObject = pushNotifications.getJSONObject(i);
                if (pushObject.has("wzrk_ttl")) {
                    pushBundle.putLong("wzrk_ttl", pushObject.getLong("wzrk_ttl"));
                }

                Iterator iterator = pushObject.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next().toString();
                    pushBundle.putString(key, pushObject.getString(key));
                }
                if (!pushBundle.isEmpty() && !mDBAdapter
                        .doesPushNotificationIdExist(pushObject.getString("wzrk_pid"))) {
                    mLogger.verbose("Creating Push Notification locally");
                    if (pushAmpListener != null) {
                        pushAmpListener.onPushAmpPayloadReceived(pushBundle);
                    } else {
                        createNotification(context, pushBundle);
                    }
                } else {
                    mLogger.verbose(mConfig.getAccountId(),
                            "Push Notification already shown, ignoring local notification :" + pushObject
                                    .getString("wzrk_pid"));
                }
            }
        } catch (JSONException e) {
            mLogger.verbose(mConfig.getAccountId(), "Error parsing push notification JSON");
        }
    }

    private void resetAlarmScheduler(Context context) {
        if (getPingFrequency(context) <= 0) {
            stopAlarmScheduler(context);
        } else {
            stopAlarmScheduler(context);
            createAlarmScheduler(context);
        }
    }

    private void setPingFrequency(Context context, int pingFrequency) {
        StorageHelper.putInt(context, Constants.PING_FREQUENCY, pingFrequency);
    }

    private void stopAlarmScheduler(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent cancelIntent = new Intent(CTBackgroundIntentService.MAIN_ACTION);
        cancelIntent.setPackage(context.getPackageName());
        PendingIntent alarmPendingIntent = PendingIntent
                .getService(context, mConfig.getAccountId().hashCode(), cancelIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarmManager != null && alarmPendingIntent != null) {
            alarmManager.cancel(alarmPendingIntent);
        }
    }

    /**
     * updates the ping frequency if there is a change & reschedules existing ping tasks.
     */
    private void updatePingFrequencyIfNeeded(final Context context, int frequency) {
        mLogger.verbose("Ping frequency received - " + frequency);
        mLogger.verbose("Stored Ping Frequency - " + getPingFrequency(context));
        if (frequency != getPingFrequency(context)) {
            setPingFrequency(context, frequency);
            if (getCoreState().getConfig().isBackgroundSync() && !getCoreState().getConfig().isAnalyticsOnly()) {
                getCoreState().getPostAsyncSafelyHandler()
                        .postAsyncSafely("createOrResetJobScheduler", new Runnable() {
                            @Override
                            public void run() {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    mLogger.verbose("Creating job");
                                    createOrResetJobScheduler(context);
                                } else {
                                    mLogger.verbose("Resetting alarm");
                                    resetAlarmScheduler(context);
                                }
                            }
                        });
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static JobInfo getJobInfo(int jobId, JobScheduler jobScheduler) {
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == jobId) {
                return jobInfo;
            }
        }
        return null;
    }

}

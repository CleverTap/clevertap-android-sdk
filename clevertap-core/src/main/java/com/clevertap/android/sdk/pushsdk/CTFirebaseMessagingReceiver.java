package com.clevertap.android.sdk.pushsdk;

import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.buildPushNotificationRenderedListenerKey;
import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getAccountIdFromNotificationBundle;
import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getPushIdFromNotificationBundle;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import com.clevertap.android.sdk.BuildConfig;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.interfaces.NotificationRenderedListener;
import com.clevertap.android.sdk.pushnotification.INotificationRenderer;
import com.clevertap.android.sdk.pushnotification.NotificationInfo;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Receiver to receive firebase messaging broadcast directly from OS.
 * This guarantees OS delivered broadcast reaches to Receiver directly instead of FirebaseMessagingService.
 */
public class CTFirebaseMessagingReceiver extends BroadcastReceiver implements NotificationRenderedListener {


    private CountDownTimer countDownTimer;

    private long end;

    private String key = "";

    private Future<?> notificationHandlerTaskResult;

    private boolean isPRFinished;

    private PendingResult pendingResult;

    private static final String TAG = "CTRM";

    private ScheduledExecutorService scheduledExecutorService;

    private long start;

    /**
     * Runnable which will execute after 4.5 secs (configurable from server using ctrmt key).
     * This will shutdown executor to clean up resources and will finish pending result.
     */
    private final Runnable finishReceiverTask = new Runnable() {
        @Override
        public void run() {
            try {
                finishReceiverAndCancelTimer("timeOut");
                if (scheduledExecutorService != null) {
                    scheduledExecutorService.shutdown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Callback when notification is rendered by core sdk.
     *
     * @param isRendered true if rendered successfully
     */
    @SuppressLint("RestrictedApi")
    @Override
    public void onNotificationRendered(final boolean isRendered) {
        Logger.v(TAG, "onNotificationRendered() called for key = "+key);
        CleverTapAPI.removeNotificationRenderedListener(key);
        finishReceiverAndCancelTimer("onNotificationRendered");
    }

    /**
     * This will finish {@link PendingResult} to signal OS that we are done with our
     * work and OS can kill App process.
     *
     * @param from name of the caller
     */
    private void finishReceiverAndCancelTimer(String from) {
        try {
            Logger.v(TAG, "finishCTRMAndCancelTimer() called");

            long end = System.nanoTime();
            Logger.v(TAG,
                    "finishing CTRM in " + TimeUnit.NANOSECONDS.toSeconds(end - start)
                            + " seconds from finishCTRMAndCancelTimer when " + from);
            if (pendingResult != null && !isPRFinished) {
                pendingResult.finish();
                isPRFinished = true;

                // rendered before timer can finish, so cancel now
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("RestrictedApi")
    @Override
    public void onReceive(Context context, Intent intent) {

        start = System.nanoTime();
        if (context == null || intent == null) {
            return;
        }

        RemoteMessage remoteMessage = new RemoteMessage(intent.getExtras());
        final Bundle messageBundle = FcmNotificationParser.toBundle(remoteMessage);

        /*
          Configurable logic, required to render push and send impressions
          value of 1 represents old logic with future and 2 belongs to new wait and notify only logic.
         */
        int numReceiverLogic = Integer.parseInt(messageBundle.getString("ctrml", "2"));
        /*
          Configurable time, required to render push and send impressions
         */
        long receiverLifeSpan = Long.parseLong(messageBundle.getString("ctrmt", "4500"));

        if (numReceiverLogic == 2) {

            pendingResult = goAsync();

            Logger.d(TAG, "CTRM received for message");

            NotificationInfo notificationInfo = CleverTapAPI.getNotificationInfo(messageBundle);

            if (notificationInfo.fromCleverTap) {

                final boolean isRenderFallback = Utils.isRenderFallback(remoteMessage, context);
                if (isRenderFallback) {
                    key = buildPushNotificationRenderedListenerKey(
                            getAccountIdFromNotificationBundle(messageBundle),
                            getPushIdFromNotificationBundle(messageBundle)
                    );
                    CleverTapAPI.addNotificationRenderedListener(key, this);

                    countDownTimer = new CountDownTimer(receiverLifeSpan, 1000) {
                        @Override
                        public void onFinish() {
                            Logger.v(TAG, "is main thread = " + (Looper.myLooper() == Looper.getMainLooper()));
                            CleverTapAPI.removeNotificationRenderedListener(key);
                            finishReceiverAndCancelTimer("timer");
                        }

                        @Override
                        public void onTick(final long millisUntilFinished) {
                            // NO-OP
                        }
                    };

                    countDownTimer.start();

                } else {
                    Logger.v(TAG, "Notification payload does not have a fallback key.");
                    finishReceiverAndCancelTimer("isRenderFallback is false");
                }
            } else {
                Logger.v(TAG, "Notification payload is not from CleverTap.");
                finishReceiverAndCancelTimer("push is not from CleverTap.");
            }
        } else {

            final long finalReceiverLifeSpan = TimeUnit.MILLISECONDS.toNanos(
                    receiverLifeSpan); // use nano for accurate timeout

            try {
            /*
              Timer is implemented using SingleThreadScheduledExecutor
             */
                scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                scheduledExecutorService.schedule(finishReceiverTask, receiverLifeSpan, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Take permission from OS that we require more time to execute CTRM, please don't kill App process.
            pendingResult = goAsync();

        /*
            Task required to be executed on SingleThreadExecutor for timeout purpose of CT instance creation.
            This will also process and validate notification message received from server.
         */
            Runnable renderPushTask = new Runnable() {
                @Override
                public void run() {
                    Logger.v(TAG, "intent received for message" + PushUtils.bundleToString(intent.getExtras()));
                    //Logger.d(TAG, "starting renderPushTask on " + Thread.currentThread().getName());

                    NotificationInfo notificationInfo = CleverTapAPI.getNotificationInfo(messageBundle);

                    if (notificationInfo.fromCleverTap) {

                        final boolean isRenderFallback = Utils.isRenderFallback(remoteMessage, context);
                        if (isRenderFallback) {

                            try { // render rate logic
                                INotificationRenderer fallbackRenderer = new FallbackNotificationRenderer();
                                CleverTapAPI cleverTapAPI = CleverTapAPI.getGlobalInstance(context,
                                        getAccountIdFromNotificationBundle(
                                                messageBundle));

                                // Waiting for 500 ms for service to render push
                                Thread.sleep(500);

                                if (Thread.currentThread().isInterrupted()) {
                                    return; // if future is cancelled then return
                                }
                                Objects.requireNonNull(cleverTapAPI)
                                        .setNotificationRenderedListener(CTFirebaseMessagingReceiver.this);
                                cleverTapAPI.setCustomSdkVersion("ctpsdkversion", BuildConfig.VERSION_CODE);
                                messageBundle.putString(Constants.NOTIFICATION_HEALTH,
                                        Constants.WZRK_HEALTH_STATE_BAD);
                                messageBundle.putString("nh_source", "CTFirebaseMessagingCTRM");

                                notificationHandlerTaskResult = Objects.requireNonNull(cleverTapAPI)
                                        .renderPushNotification(fallbackRenderer, context, messageBundle);
                            } catch (Throwable throwable) {
                                Logger.v(TAG, "Error parsing FCM payload");
                                throwable.printStackTrace();
                                finishReceiverAndCancelTimer("exception during renderPushTask");
                            }
                        } else {
                            Logger.v(TAG, "Notification payload does not have a fallback key.");
                            finishReceiverAndCancelTimer("isRenderFallback is false");
                        }
                    } else {
                        Logger.v(TAG, "Notification payload is not from CleverTap.");
                        finishReceiverAndCancelTimer("push is not from CleverTap");
                    }

                    end = System.nanoTime();
                }
            };

        /*
            Root task for parent thread responsible for timeout of CT instance creation and notification rendering
             from core.This will make sure that both, CT instance creation and notification rendering
             from core gets finished in specified time defined by ctrmt.
         */
            Runnable timeOutHandlerTask = new Runnable() {

                @Override
                public void run() {
                    Future<?> renderPushTaskResult = null;
                    ExecutorService executorService = null;
                    try {
                        executorService = Executors.newSingleThreadExecutor();
                        renderPushTaskResult = executorService.submit(renderPushTask);

                        renderPushTaskResult.get(finalReceiverLifeSpan, TimeUnit.NANOSECONDS); // blocking

                        if (notificationHandlerTaskResult == null) {
                            return;
                        }

                        long remainingTimeForReceiverDeath = finalReceiverLifeSpan - (end
                                - start); // can be -ve or +ve

                        if (remainingTimeForReceiverDeath <= 0) {
                            finishReceiverTask.run(); // Task1 timedout.
                        } else {
                            notificationHandlerTaskResult.get(remainingTimeForReceiverDeath, TimeUnit.NANOSECONDS);
                            boolean ctrki = Boolean.parseBoolean(messageBundle.getString("ctrki", "false"));
                            if (ctrki) {
                                finishReceiverTask.run(); // Task2 success
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        finishReceiverTask.run(); // Task1 or Task 2 timeOut
                        if (renderPushTaskResult != null && !renderPushTaskResult.isCancelled()) {
                            Logger.d(TAG, "Cancelling renderPushTaskResult");
                            renderPushTaskResult.cancel(true);
                        }
                        if (notificationHandlerTaskResult != null && !notificationHandlerTaskResult.isCancelled()) {
                            Logger.d(TAG, "Cancelling future2");
                            notificationHandlerTaskResult.cancel(true);
                        }
                    } finally {
                        try {
                            if (executorService != null) {
                                executorService.shutdown();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            new Thread(timeOutHandlerTask).start();// All work must go in background - Parent thread

        }


    }

}
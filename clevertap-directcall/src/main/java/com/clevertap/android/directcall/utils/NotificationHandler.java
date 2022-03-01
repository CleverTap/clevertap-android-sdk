package com.clevertap.android.directcall.utils;

import static com.clevertap.android.directcall.Constants.ACTION_OUTGOING_HANGUP;
import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.KEY_BRAND_LOGO;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_INITIATOR_ACTIONS;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_RECEIVER_ACTIONS;
import static com.clevertap.android.directcall.utils.NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL;
import static com.clevertap.android.directcall.utils.NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL;
import static com.clevertap.android.directcall.utils.NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL;
import static com.clevertap.android.directcall.utils.NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.clevertap.android.directcall.Constants;
import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.directcall.recievers.CallNotificationActionReceiver;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.models.MissedCallActions;
import com.clevertap.android.directcall.ui.DirectCallingActivity;
import com.clevertap.android.directcall.ui.DirectIncomingCallFragment;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Helper class to manage notification channels, and create notifications.
 */
public class NotificationHandler {

    private static NotificationHandler instance;
    private static NotificationManager mNotificationManager;

    public NotificationHandler() {
    }

    /**
     * Singleton pattern implementation
     *
     * @return
     */
    public static NotificationHandler getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationHandler();
            mNotificationManager =
                    (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return instance;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public NotificationChannel getNotificationChannel(String CHANNEL_ID, String CHANNEL_NAME, String CHANNEL_DESC, String channelCause) throws Exception {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(CHANNEL_DESC);
        channel.setShowBadge(true);
        channel.canShowBadge();
        channel.enableLights(true);
        channel.setLightColor(Color.GREEN);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        if (channelCause.equals("call")) {
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setVibrationPattern(new long[]{0});
        } else {
            channel.enableVibration(true);
            channel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build());
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
        }
        return channel;
    }

    public PendingIntent getTrampolinePendingIntent(Context context, Intent intent) {
        Intent notificationTrampolineActivityIntent = intent;
        // Create the TaskStackBuilder
        PendingIntent resultPendingIntent = TaskStackBuilder.create(context)
                // Add the intent, which inflates the back stack
                .addNextIntentWithParentStack(notificationTrampolineActivityIntent)
                // Get the PendingIntent containing the entire back stack
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return resultPendingIntent;
    }

    public PendingIntent getPendingIntent(Context context, Intent intent) {
        return PendingIntent.getBroadcast(context, getRandomNotificationId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public int getRandomNotificationId() {
        return ThreadLocalRandom.current().nextInt(0, 9999);
    }

    public static class CallNotificationHandler {
        private Context context;
        private static CallNotificationHandler callNotificationInstance;
        private NotificationHandler notificationHandler;
        private String CALL_CHANNEL_ID, CALL_CHANNEL_NAME, CALL_CHANNEL_DESC;
        private String callNotificationStatus;

        public interface CallNotificationTypes {
            String OUTGOING_CALL = "outgoing";
            String INCOMING_CALL = "incoming";
            String ONGOING_CALL = "ongoing";
            String MISSED_CALL = "missed";
        }

        public String getCallNotificationStatus() {
            return callNotificationStatus;
        }

        public void setCallNotificationStatus(String callNotificationStatus) {
            this.callNotificationStatus = callNotificationStatus;
        }

        private CallNotificationHandler(Context context) {
            this.context = context;
            notificationHandler = NotificationHandler.getInstance(context);
            this.CALL_CHANNEL_ID = context.getString(R.string.secondary_channel_id);
            this.CALL_CHANNEL_NAME = context.getString(R.string.secondary_channel_name);
            this.CALL_CHANNEL_DESC = context.getString(R.string.secondary_channel_desc);
        }

        public static CallNotificationHandler getInstance(Context context) {
            if (callNotificationInstance == null) {
                callNotificationInstance = new CallNotificationHandler(context);
                mNotificationManager =
                        (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            }
            return callNotificationInstance;
        }

        @TargetApi(Build.VERSION_CODES.O)
        public String createChannel(Context context, int channelImportance) {
            NotificationChannel callInviteChannel = new NotificationChannel(Constants.VOICE_CHANNEL_HIGH_IMPORTANCE,
                    "Primary Voice Channel", NotificationManager.IMPORTANCE_HIGH);
            String channelId = Constants.VOICE_CHANNEL_HIGH_IMPORTANCE;

            if (channelImportance == NotificationManager.IMPORTANCE_LOW) {
                callInviteChannel = new NotificationChannel(Constants.VOICE_CHANNEL_LOW_IMPORTANCE,
                        "Primary Voice Channel", NotificationManager.IMPORTANCE_LOW);
                channelId = Constants.VOICE_CHANNEL_LOW_IMPORTANCE;
            }
            callInviteChannel.enableVibration(false);
            callInviteChannel.setSound(null, null);
            callInviteChannel.setVibrationPattern(new long[]{0});
            callInviteChannel.enableLights(true);
            callInviteChannel.setLightColor(Color.GREEN);
            callInviteChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(callInviteChannel);

            return channelId;
        }

        public void showCallNotification(final JSONObject callDetails, final String type) {
            try {
                Thread imageLoaderThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setCallNotificationStatus(type);   //setting call-notification type

                            SharedPreferences sharedPref = StorageHelper.getPreferences(context);
                            String logoUrl = sharedPref.getString(KEY_BRAND_LOGO, null);

                            RequestOptions requestOptions = RequestOptions
                                    .diskCacheStrategyOf(DiskCacheStrategy.ALL);

                            Glide.
                                    with(context).asBitmap().
                                    load(logoUrl).
                                    listener(new RequestListener<Bitmap>() {
                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                            onNotificationLargeIconLoaded(callDetails, type, null);
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                            onNotificationLargeIconLoaded(callDetails, type, resource);
                                            return false;
                                        }
                                    })
                                    .apply(requestOptions)
                                    .submit();

                        } catch (Exception e) {
                            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while generating the call notification: " + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                    }
                });
                imageLoaderThread.start();
            } catch (Exception e) {

            }
        }

        private void onNotificationLargeIconLoaded(JSONObject callDetails, String type, Bitmap largeIcon) {
            try {
                String channelId;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    channelId = createChannel(context, Utils.getInstance().isAppInBackground()
                            ?NotificationManager.IMPORTANCE_HIGH: NotificationManager.IMPORTANCE_LOW);
                }else {
                    channelId = context.getString(R.string.primary_channel_id);
                }

                String callContext = callDetails.has("context") ? callDetails.getString("context") : null;
                String fromCuid = callDetails.has("fromCuid") ? callDetails.getString("fromCuid") : null;
                String toCuid = callDetails.has("toCuid") ? callDetails.getString("toCuid") : null;
                Map<String, Intent> actionIntentMap = new HashMap<>();
                switch (type) {
                    case CallNotificationTypes.INCOMING_CALL:
                        try {
                            if (DirectIncomingCallFragment.getInstance() != null) {
                                actionIntentMap.put("Answer", getCallActionIntent("Answer", callDetails));
                            } else {
                                actionIntentMap.put("Answer", getCallActionIntent("Answer", new JSONObject(callDetails.getString(context.getString(R.string.callDetails)))));
                            }
                            actionIntentMap.put("Decline", getCallActionIntent("Decline", callDetails));
                            generateNotification(callContext, callDetails, "Incoming voice call", actionIntentMap, CallNotificationTypes.INCOMING_CALL, largeIcon, channelId);
                        } catch (Exception e) {

                        }
                        break;
                    case OUTGOING_CALL:
                        actionIntentMap.put("Hangup", getCallActionIntent(ACTION_OUTGOING_HANGUP, callDetails));
                        generateNotification(callContext, callDetails, "Ringing...", actionIntentMap, OUTGOING_CALL, largeIcon, channelId);
                        break;
                    case CallNotificationTypes.ONGOING_CALL:
                        actionIntentMap.put("Hangup", getCallActionIntent("Hangup_Ongoing", callDetails));
                        generateNotification(callContext, callDetails, "Ongoing voice call", actionIntentMap, CallNotificationTypes.ONGOING_CALL, largeIcon, channelId);
                        break;
                    case CallNotificationTypes.MISSED_CALL:
                        List<MissedCallActions> missedCallActions = new ArrayList<>();
                        String callDirection = DataStore.getInstance().getCallDirection();
                        switch (callDirection) {
                            case "outgoing":
                                missedCallActions = Utils.getMissedCallActionsList(context, KEY_MISSED_CALL_INITIATOR_ACTIONS);
                                break;
                            case "incoming":
                                missedCallActions = Utils.getMissedCallActionsList(context, KEY_MISSED_CALL_RECEIVER_ACTIONS);
                                break;
                        }

                        if (missedCallActions.size() > 0) {
                            for (int i = 0; i < missedCallActions.size(); i++) {
                                if (i != 3) {
                                    actionIntentMap.put(missedCallActions.get(i).getActionLabel(),
                                            getMissedActionIntent("Missed", callDirection, fromCuid, toCuid, callContext, missedCallActions.get(i)));
                                } else {
                                    break;
                                }
                            }
                        }
                        generateNotification("Missed voice call", callDetails, callContext, actionIntentMap, CallNotificationTypes.MISSED_CALL, largeIcon, channelId);
                        break;
                }
            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while generating the call notification: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        private Intent getCallActionIntent(String actionType, JSONObject callDetails) {
            Intent actionIntent = new Intent(context, CallNotificationActionReceiver.class);
            try {
                actionIntent.putExtra("actionType", actionType);
                actionIntent.putExtra("callDetails", callDetails.toString());
                if (actionType.equals(INCOMING_CALL) && DataStore.getInstance().getSid() != null) {
                    actionIntent.putExtra("sid", DataStore.getInstance().getSid());
                }
            } catch (Exception e) {

            }
            return actionIntent;
        }

        private Intent getMissedActionIntent(String actionType, String callDirection, String fromCuid, String toCuid, String callCtx, MissedCallActions missedCallActions) {
            Intent actionIntent = new Intent(context, CallNotificationActionReceiver.class);
            actionIntent.putExtra("actionType", actionType);
            actionIntent.putExtra("callDirection", callDirection);
            actionIntent.putExtra("callContext", callCtx);
            actionIntent.putExtra("callerCuid", fromCuid);
            actionIntent.putExtra("calleeCuid", toCuid);

            try {
                actionIntent.putExtra("missedCallActions", missedCallActions.toJson().toString());

            } catch (Exception e) {

            }
            return actionIntent;
        }

        Notification.Builder notificationBuilder1;
        NotificationCompat.Builder notificationBuilder2;

        private void generateNotification(final String title, final JSONObject callDetails, final String body, final Map<String, Intent> actionIntentMap, final String notiType, final Bitmap largeIcon, String channelId) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationBuilder1 = new Notification.Builder(context, channelId)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setShowWhen(true)
                            .setSmallIcon(R.drawable.ct_lightening)
                            .setStyle(new Notification.BigTextStyle().bigText(body))
                            .setAutoCancel(false);

                    if (largeIcon != null) {
                        notificationBuilder1.setLargeIcon(largeIcon);
                    }
                    if (notiType.equals(CallNotificationTypes.ONGOING_CALL))
                        notificationBuilder1.setUsesChronometer(true);
                    if (!notiType.equals(CallNotificationTypes.MISSED_CALL)) {
                        Intent intent = new Intent(context, DirectCallingActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        if (notiType.equals(INCOMING_CALL)) {
                            Bundle bundle = new Bundle();
                            bundle.putString(context.getString(R.string.callDetails), callDetails.getString(context.getString(R.string.callDetails)));
                            bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
                            bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
                            intent.putExtras(bundle);
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                notificationBuilder1.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE), true);
                            }
                        }
                        notificationBuilder1.setOngoing(true)
                                .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                    } else {
                        notificationBuilder1.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                                .setAutoCancel(true);
                    }

                    for (String key : actionIntentMap.keySet()) {
                        switch (key) {
                            case "Answer":
//                                            actionTitle = "<font color='#FF64DD17'>" + key + "</font>";
//                                            notificationBuilder1.addAction(0, Html.fromHtml(actionTitle), notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                notificationBuilder1.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                            case "Decline":
//                                            actionTitle = "<font color='#FFD50000'>" + key + "</font>";
//                                            notificationBuilder1.addAction(0, Html.fromHtml(actionTitle), notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                notificationBuilder1.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                            default:
                                notificationBuilder1.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                        }
                    }
                    assert mNotificationManager != null;
                    mNotificationManager.createNotificationChannel(notificationHandler.getNotificationChannel(CALL_CHANNEL_ID, CALL_CHANNEL_NAME, CALL_CHANNEL_DESC, "call"));

                    switch (notiType) {
                        case OUTGOING_CALL:
                            mNotificationManager.notify(OUTGOING_CALL, 101, notificationBuilder1.build());
                            break;
                        case ONGOING_CALL:
                            mNotificationManager.notify(ONGOING_CALL, 102, notificationBuilder1.build());
                            break;
                        case INCOMING_CALL:
                            mNotificationManager.notify(INCOMING_CALL, 103, notificationBuilder1.build());
                            break;
                        case MISSED_CALL:
                            mNotificationManager.notify(MISSED_CALL, 104, notificationBuilder1.build());
                            break;
                    }
                } else {
                    notificationBuilder2 = new NotificationCompat.Builder(context);
                    notificationBuilder2.setContentTitle(title)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .setBigContentTitle(title)
                                    .bigText(body))
                            .setShowWhen(true)
                            .setContentText(body)
                            .setAutoCancel(false)
                            .setPriority(NotificationManager.IMPORTANCE_HIGH)
                            .setSound(null)
                            .setVibrate(new long[]{0})
                            //.setLargeIcon(notificationHandler.getBitmapFromUrl(context, logoUrl))
                            //.setDefaults(Notification.DEFAULT_ALL)
                            .setSmallIcon(R.drawable.ct_lightening);
                    if (largeIcon != null) {
                        notificationBuilder2.setLargeIcon(largeIcon);
                    }
                    if (notiType.equals(CallNotificationTypes.ONGOING_CALL))
                        notificationBuilder2.setUsesChronometer(true);
                    if (!notiType.equals(CallNotificationTypes.MISSED_CALL)) {
                        Intent intent1 = new Intent(context, DirectCallingActivity.class);
                        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        if (notiType.equals(INCOMING_CALL)) {
                            Bundle bundle = new Bundle();
                            bundle.putString(context.getString(R.string.callDetails), callDetails.getString(context.getString(R.string.callDetails)));
                            bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
                            bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
                            intent1.putExtras(bundle);
                        }
                        notificationBuilder2.setOngoing(true)
                                .setContentIntent(PendingIntent.getActivity(context, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                    } else {
                        notificationBuilder2.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                                .setAutoCancel(true);
                    }

                    for (String key : actionIntentMap.keySet()) {
                        switch (key) {
                            case "Answer":
//                                            actionTitle = "<font color='#FF64DD17'>" + key + "</font>";
//                                            notificationBuilder1.addAction(0, Html.fromHtml(actionTitle), notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                notificationBuilder2.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                            case "Decline":
//                                            actionTitle = "<font color='#FFD50000'>" + key + "</font>";
//                                            notificationBuilder1.addAction(0, Html.fromHtml(actionTitle), notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                notificationBuilder2.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                            default:
                                notificationBuilder2.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                        }
                    }
                    assert mNotificationManager != null;
                    switch (notiType) {
                        case OUTGOING_CALL:
                            mNotificationManager.notify(OUTGOING_CALL, 101, notificationBuilder2.build());
                            break;
                        case ONGOING_CALL:
                            mNotificationManager.notify(ONGOING_CALL, 102, notificationBuilder2.build());
                            break;
                        case INCOMING_CALL:
                            mNotificationManager.notify(INCOMING_CALL, 103, notificationBuilder2.build());
                            break;
                        case MISSED_CALL:
                            mNotificationManager.notify(MISSED_CALL, 104, notificationBuilder2.build());
                            break;
                    }
                }
            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in call notification handling: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        public void removeNotification(String tag) {
            try {
                setCallNotificationStatus(null);

                if (mNotificationManager != null) {
                    switch (tag) {
                        case OUTGOING_CALL:
                            mNotificationManager.cancel(tag, 101);
                            break;
                        case ONGOING_CALL:
                            mNotificationManager.cancel(tag, 102);
                            break;
                        case INCOMING_CALL:
                            mNotificationManager.cancel(tag, 103);
                            break;
                        case MISSED_CALL:
                            mNotificationManager.cancel(tag, 104);
                            break;
                    }
                }
            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while generating the call notification: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        public void rebuildNotification(String type) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    switch (type) {
                        case OUTGOING_CALL:
                            mNotificationManager.notify(type, 101, notificationBuilder1.build());
                            break;
                        case ONGOING_CALL:
                            mNotificationManager.notify(type, 102, notificationBuilder1.build());
                            break;
                        case INCOMING_CALL:
                            mNotificationManager.notify(type, 103, notificationBuilder1.build());
                            break;
                    }
                } else {
                    switch (type) {
                        case OUTGOING_CALL:
                            mNotificationManager.notify(type, 101, notificationBuilder2.build());
                            break;
                        case ONGOING_CALL:
                            mNotificationManager.notify(type, 102, notificationBuilder2.build());
                            break;
                        case INCOMING_CALL:
                            mNotificationManager.notify(type, 103, notificationBuilder2.build());
                            break;
                    }
                }
            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in updating the call details on call notification: " + e.getLocalizedMessage());
                e.printStackTrace();
            }

        }
    }
}
package com.clevertap.android.directcall.fcm;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.KEY_BRAND_LOGO;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

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
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.recievers.CallNotificationActionReceiver;
import com.clevertap.android.directcall.ui.DirectCallingActivity;
import com.clevertap.android.directcall.utils.NotificationHandler;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FcmSigSockService extends Service {
    final String CHANNEL_ID = "com.directCall.pushNotification";
    final String CHANNEL_NAME = "Direct Call Push Notifications";
    final String CHANNEL_DESC = "push notification to receive call";
    Notification.Builder notificationBuilder1;
    NotificationCompat.Builder notificationBuilder2;
    JSONObject callDetails;
    String callContext;
    private Context context;
    NotificationManager mNotificationManager;
    Map<String, Intent> actionIntentMap = new HashMap<>();

    private static FcmSigSockService instance = null;

    public static FcmSigSockService getInstance() {
        return instance;
    }

    private Intent getActionIntent(String actionType, JSONObject callDetails) {
        Intent actionIntent = null;
        actionIntent = new Intent(this, CallNotificationActionReceiver.class);
        actionIntent.putExtra("actionType", actionType);
        actionIntent.putExtra("callDetails", (callDetails != null) ? callDetails.toString() : null);
        if (actionType != null && actionType.equals("Answer")
                && DataStore.getInstance().getSid() != null) {
            actionIntent.putExtra("sid", DataStore.getInstance().getSid());
        }
        return actionIntent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            instance = this;
            this.context = this;
            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationManager.createNotificationChannel(NotificationHandler.getInstance(context).getNotificationChannel(CHANNEL_ID, CHANNEL_NAME, CHANNEL_DESC, "call"));
            }

            if (DataStore.getInstance().getContext() != null) {
                context = DataStore.getInstance().getContext();
            } else {
                context = getApplicationContext();
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception handling the FCM call: " + e.getLocalizedMessage());
            e.printStackTrace();
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DataStore.getInstance().getContext() != null) {
            context = DataStore.getInstance().getContext();
        } else {
            context = getApplicationContext();
        }

        //do heavy work on a background thread
        new Thread(() -> {
            try {
                String action = intent.getAction();
                JSONObject incomingData = new JSONObject(intent.getStringExtra(Constants.FCM_NOTIFICATION_PAYLOAD));

                switch (action) {
                    case Constants.ACTION_INCOMING_CALL:
                        handleIncomingCall(context, incomingData);
                        break;
                    case Constants.ACTION_CANCEL_CALL:
                        String callId = incomingData.has("callId") ? incomingData.getString("callId") : null;
                        handleCancelledCall(context, callId);
                        break;
            /*case Constants.ACTION_FCM_NOTIFICATION_RECEIVED:
                JobSchedulerSocketService.generateNotification(context, incomingData);
                final String notificationId = incomingData.has("notificationId") ? incomingData.getString("notificationId") : "";
                //Following setter will help to get notificationID while marking fcmNotification status to delivered
                if (SocketIOManager.getSocket() != null && SocketIOManager.isSocketConnected()) {
                    Utils.getInstance().markNotificationDelivered(context, notificationId);
                } else {
                    FcmCacheManger.getInstance().setShouldMarkNotificationStatusDelivered(true);
                }
                break;*/
                    default:

                        break;
                }
            } catch (Exception e) {
                try {
                    endForeground();
                    stopSelf();
                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception handling the FCM call: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }catch (Exception e1){
                    e.printStackTrace();
                }
            }
        }).start();
        return START_NOT_STICKY;
    }

    private void handleIncomingCall(Context context, JSONObject incomingData) {
        setCallInProgressNotification(incomingData);
        DataStore.getInstance().setCallDirection("incoming");
        FcmUtil.getInstance(context).onIncomingCall(context, incomingData);
    }

    private void setCallInProgressNotification(JSONObject incomingData) {
        try {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Setting Call in progress notification");

            callDetails = incomingData;
            callContext = callDetails.has("context") ? callDetails.getString("context") : null;

            //startService in foreground fot oreo and onwards other show only notification
            Notification notification = createNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if(notification != null)
                    startForeground(10001, notification);
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception in setting the call notification: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private Notification createNotification() {
        try {
            /*if (DirectIncomingCallFragment.getInstance() != null) {
                actionIntentMap.put("Answer", getActionIntent("Answer", callDetails));
            } else {
                actionIntentMap.put("Answer", getActionIntent("Answer", callDetails));
            }*/
            actionIntentMap.put("Answer", getActionIntent("Answer", callDetails));
            actionIntentMap.put("Decline", getActionIntent("Decline", callDetails));

            SharedPreferences sharedPref = StorageHelper.getPreferences(this);
            String logoUrl = sharedPref.getString(KEY_BRAND_LOGO, null);
            RequestOptions requestOptions = RequestOptions
                    .diskCacheStrategyOf(DiskCacheStrategy.ALL);
            Glide.with(this).asBitmap().
                    load(logoUrl).
                    listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            generateNotification(null);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            generateNotification(resource);
                            return false;
                        }
                    })
                    .apply(requestOptions)
                    .submit();
        } catch (Exception e) {
            //no-op
            DirectCallAPI.getLogger().debug(
                    CALLING_LOG_TAG_SUFFIX,
                    "Failed to render foreground notification for FCM call: " + e.getLocalizedMessage()
            );
        }
        return null;
    }

    private void handleCancelledCall(Context context, String callId) {
        FcmUtil.getInstance(context).onCancelCall(context, callId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    private void endForeground() {
        try {
            stopForeground(true);
        } catch (Exception e) {
            //no-op
        }
    }

    public Notification generateNotification(Bitmap resource) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder1 = new Notification.Builder(context, CHANNEL_ID)
                        .setContentTitle(callContext)
                        .setContentText("Incoming voice call")
                        .setShowWhen(true)
                        .setSmallIcon(R.drawable.ct_lightening)
                        .setStyle(new Notification.BigTextStyle().bigText("Incoming voice call"))
                        .setAutoCancel(false);

                if (resource != null) {
                    notificationBuilder1.setLargeIcon(resource);
                }

                Intent intent = new Intent(context, DirectCallingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Bundle bundle = new Bundle();
                bundle.putString(context.getString(R.string.callDetails), callDetails.toString());
                bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
                bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
                intent.putExtras(bundle);
                notificationBuilder1.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE), true);
                notificationBuilder1.setOngoing(true)
                        .setContentIntent(PendingIntent.getActivity(context, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

                for (String key : actionIntentMap.keySet()) {
                    switch (key) {
                        case "Answer":
                            notificationBuilder1.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                        case "Decline":
                            notificationBuilder1.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                        default:
                            notificationBuilder1.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                    }
                }
                return notificationBuilder1.build();
                //startForeground(199, notificationBuilder1.build());
            } else {
                notificationBuilder2 = new NotificationCompat.Builder(context)
                        .setContentTitle(callContext)
                        .setContentText("Incoming voice call")
                        .setShowWhen(true)
                        .setSmallIcon(R.drawable.ct_lightening)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .setBigContentTitle(callContext)
                                .bigText("Incoming voice call"))
                        .setAutoCancel(false);
                if (resource != null) {
                    notificationBuilder2.setLargeIcon(resource);
                }
                Intent intent = new Intent(context, DirectCallingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Bundle bundle = new Bundle();
                bundle.putString(context.getString(R.string.callDetails), callDetails.getString(context.getString(R.string.callDetails)));
                bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
                bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
                intent.putExtras(bundle);
                notificationBuilder2.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT), true);

                notificationBuilder2.setOngoing(true)
                        .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

                for (String key : actionIntentMap.keySet()) {
                    switch (key) {
                        case "Answer":
                            notificationBuilder2.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                        case "Decline":
                            notificationBuilder2.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                        default:
                            notificationBuilder2.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                    }
                }
                return notificationBuilder2.build();
            }
        } catch (Exception e) {
            return null;
        }
    }
}

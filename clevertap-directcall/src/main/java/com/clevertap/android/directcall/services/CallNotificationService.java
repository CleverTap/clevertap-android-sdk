package com.clevertap.android.directcall.services;

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
import android.os.Vibrator;

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
import com.clevertap.android.directcall.ui.DirectCallingActivity;
import com.clevertap.android.directcall.recievers.CallNotificationActionReceiver;
import com.clevertap.android.directcall.ui.DirectIncomingCallFragment;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.utils.NotificationHandler;
import com.clevertap.android.directcall.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CallNotificationService extends Service {

    Notification.Builder notificationBuilder1;
    NotificationCompat.Builder notificationBuilder2;
    JSONObject callDetails;
    String callContext;
    String CALL_CHANNEL_ID, CALL_CHANNEL_NAME, CALL_CHANNEL_DESC;
    private Context context;
    NotificationManager mNotificationManager;
    Map<String, Intent> actionIntentMap = new HashMap<>();

    private static CallNotificationService instance = null;

    public static CallNotificationService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            instance = this;
            this.context = this;
            if(context!=null){
                mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }else {
                mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            }
            this.CALL_CHANNEL_ID = getString(R.string.secondary_channel_id);
            this.CALL_CHANNEL_NAME = getString(R.string.secondary_channel_name);
            this.CALL_CHANNEL_DESC = getString(R.string.secondary_channel_desc);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationManager.createNotificationChannel(NotificationHandler.getInstance(context).getNotificationChannel(CALL_CHANNEL_ID, CALL_CHANNEL_NAME, CALL_CHANNEL_DESC, "call"));
            }
        }catch (Exception e){
            stopSelf();
        }
    }

    private Intent getActionIntent(String actionType, JSONObject callDetails) {
        Intent actionIntent = null;
        try {
            actionIntent = new Intent(/*context*/ this, CallNotificationActionReceiver.class);
            actionIntent.putExtra("actionType", actionType);
            actionIntent.putExtra("callDetails", callDetails.toString());
            if(actionType.equals("Answer") && DataStore.getInstance().getSid()!=null){
                actionIntent.putExtra("sid", DataStore.getInstance().getSid());
            }
        }catch (Exception e){

        }
        return actionIntent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            if(intent.hasExtra("incomingNotificationData")) {
                callDetails = new JSONObject(intent.getStringExtra("incomingNotificationData"));
                callContext = callDetails.getString("context");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            stopSelf();
        }

        try {

            if(DirectIncomingCallFragment.getInstance()!=null){
                actionIntentMap.put("Answer", getActionIntent("Answer", callDetails));
            }else {
                actionIntentMap.put("Answer", getActionIntent("Answer", new JSONObject(callDetails.getString(this.getString(R.string.callDetails)))));
            }
            actionIntentMap.put("Decline", getActionIntent("Decline", callDetails));
        }catch (Exception e){

        }
        try {
            final String channelId = NotificationHandler.CallNotificationHandler.getInstance(context).createChannel(context, Utils.getInstance().isAppInBackground()
                    ?NotificationManager.IMPORTANCE_HIGH: NotificationManager.IMPORTANCE_LOW);

            SharedPreferences sharedPref = StorageHelper.getPreferences(this);
            String logoUrl = sharedPref.getString(Constants.KEY_BRAND_LOGO, null);
            RequestOptions requestOptions = RequestOptions
                    .diskCacheStrategyOf(DiskCacheStrategy.ALL);
            Glide.with(this).asBitmap().
                    load(logoUrl).
                    listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            try {
                                generateNotification(null, channelId);
                                //stopSelf();
                            }catch (Exception e1){
                                try {
                                    stopSelf();
                                }catch (Exception e2){

                                }
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            try {
                                generateNotification(resource, channelId);
                            }catch (Exception e){
                                try {
                                    stopSelf();
                                }catch (Exception e1){

                                }
                            }
                            return false;
                        }
                    })
                    .apply(requestOptions)
                    .submit();

        }catch (Exception e){

        }


        //do heavy work on a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Utils.getInstance().startAudio(getApplicationContext());
                } catch (Exception e) {
                    try {
                        stopSelf();
                    }catch (Exception e1){

                    }
                }
            }
        }).start();
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        try {
            Intent intent = new Intent(context, CallNotificationService.class);
            context.stopService(intent);
        }catch (Exception e){

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vib.cancel();
        }catch (Exception e){
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void generateNotification(Bitmap resource, String channelId){
        try {
            Intent directCallActivityLaunchIntent = getDirectCallActivityIntent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder1 = new Notification.Builder(context, channelId)
                        .setContentTitle(callContext)
                        .setContentText("Incoming voice call")
                        .setShowWhen(true)
                        .setSmallIcon(R.drawable.ct_lightening)
                        .setStyle(new Notification.BigTextStyle().bigText("Incoming voice call"))
                        //.setLargeIcon(notificationHandler.getBitmapFromUrl(context, logoUrl))
                        .setAutoCancel(false);

                if(resource!=null){
                    notificationBuilder1.setLargeIcon(resource);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    notificationBuilder1.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
                }

                notificationBuilder1.setFullScreenIntent(
                        PendingIntent.getActivity(
                                context,
                                0,
                                directCallActivityLaunchIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        ),
                        true
                );

                notificationBuilder1
                        .setOngoing(true)
                        .setContentIntent(
                                PendingIntent.getActivity(
                                        context,
                                        0,
                                        directCallActivityLaunchIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                        )
                        );

                for (String key : actionIntentMap.keySet()) {
                    switch (key) {
                        case "Answer":
                            if(Utils.getInstance().isAppInBackground()){
                                Intent answerActionIntent = getDirectCallActivityIntent();
                                answerActionIntent.setAction(context.getString(R.string.call_answer));
                                //Notification trampoline restrictions for Android 12
                                //https://developer.android.com/about/versions/12/behavior-changes-12#notification-trampolines
                                notificationBuilder1.addAction(
                                        0,
                                        key,
                                        NotificationHandler.getInstance(context).getTrampolinePendingIntent(context, answerActionIntent)
                                );
                            }else {
                                notificationBuilder1.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            }
                            break;
                        case "Decline":
                            notificationBuilder1.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                    }
                }
                startForeground(199, notificationBuilder1.build());
            }else {
                notificationBuilder2 = new NotificationCompat.Builder(context)
                        .setContentTitle(callContext)
                        .setContentText("Incoming voice call")
                        .setShowWhen(true)
                        .setSmallIcon(R.drawable.ct_lightening)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .setBigContentTitle(callContext)
                                .bigText("Incoming voice call"))
                        //.setLargeIcon(notificationHandler.getBitmapFromUrl(context, logoUrl))
                        .setAutoCancel(false);

                if(resource!=null){
                    notificationBuilder2.setLargeIcon(resource);
                }

                notificationBuilder2.setFullScreenIntent(PendingIntent.getActivity(
                        context,
                        0,
                        directCallActivityLaunchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE),
                        true
                );

                notificationBuilder2
                        .setOngoing(true)
                        .setContentIntent(
                                PendingIntent.getActivity(
                                        context,
                                        0,
                                        directCallActivityLaunchIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                )
                        );

                for (String key : actionIntentMap.keySet()) {
                    switch (key) {
                        case "Answer":
                            notificationBuilder2.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                        case "Decline":
                            notificationBuilder2.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                    }
                }

                startForeground(199, notificationBuilder2.build());
            }

        }catch (Exception e){
            try {
                stopSelf();
            }catch (Exception e1){
                //no-op
            }
        }
    }

    private Intent getDirectCallActivityIntent() throws JSONException{
        Intent intent = new Intent(context, DirectCallingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Bundle bundle = new Bundle();
        bundle.putString(context.getString(R.string.callDetails), callDetails.getString(context.getString(R.string.callDetails)));
        bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
        bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
        intent.putExtras(bundle);
        return intent;
    }
}
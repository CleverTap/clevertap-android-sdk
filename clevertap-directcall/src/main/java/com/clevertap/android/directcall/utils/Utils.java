package com.clevertap.android.directcall.utils;

import static android.content.Context.AUDIO_SERVICE;
import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.KEY_ACCOUNT_ID;
import static com.clevertap.android.directcall.Constants.KEY_DECLINE_REASON;
import static com.clevertap.android.directcall.Constants.KEY_DECLINE_REASON_CODE;
import static com.clevertap.android.directcall.Constants.KEY_NEVER_ASK_AGAIN_PERMISSION;
import static com.clevertap.android.directcall.ui.DirectIncomingCallFragment.afChangeListener;

import android.Manifest;
import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.directcall.enums.CallStatus;
import com.clevertap.android.directcall.events.CTSystemEvent;
import com.clevertap.android.directcall.events.DCSystemEventInfo;
import com.clevertap.android.directcall.fcm.FcmCacheManger;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.models.MissedCallActions;
import com.clevertap.android.directcall.services.CallNotificationService;
import com.clevertap.android.directcall.services.JobSchedulerSocketService;
import com.clevertap.android.directcall.ui.DirectIncomingCallFragment;
import com.clevertap.android.sdk.CleverTapAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.Ack;

public class Utils {

    private static Utils instance = null;
    private static MediaPlayer ringtonePlayer;

    private static Vibrator vib;

    public static Utils getInstance() {
        if (instance == null) {
            instance = new Utils();
        }
        return instance;
    }

    private Utils() {
    }

    public static String getAccountIdFromNotificationBundle(Bundle message) {
        String defaultValue = "";
        return message != null ? message.getString("wzrk_acct_id", defaultValue) : defaultValue;
    }

    /**
     * check if the user has active internet connection or not.
     *
     * @param context: context of the activity from which initSdk is called.
     * @return reutrns true if internet connection is available else false.
     * @throws IOException
     */
    public boolean hasInternetAccess(Context context){
        if (isNetworkAvailable(context)) {
            try {
                HttpURLConnection urlc = (HttpURLConnection)
                        (new URL("https://clients3.google.com/generate_204")
                                .openConnection());
                urlc.setRequestProperty("User-Agent", "Android");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(5000);
                urlc.setReadTimeout(5000);
                urlc.connect();
                return (urlc.getResponseCode() == 204);
            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while checking internet connectivity: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * check whether user is connected to data or wifi.
     *
     * @param context context of the activity from which initSdk is called.
     * @return
     */
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * gives real time latency of user network during a call.
     *
     * @param url:- url to be send to calculate the network latency
     * @return
     */
    public String ping(String url) {
        String str = "";
        try {
            Process process = Runtime.getRuntime().exec(
                    "ping -c 1 " + url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            int i;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            String op[];
            String delay[] = new String[8];
            while ((i = reader.read(buffer)) > 0)
                output.append(buffer, 0, i);
            reader.close();
            op = output.toString().split("\n");
            if (op.length > 1) {
                delay = op[1].split("time=");
            }
            if (delay.length > 1) {
                str = delay[1];
                if (str != null && !str.isEmpty()) {
                    if (str.indexOf('.') >= 0) {
                        str = str.substring(0, str.indexOf('.'));
                    } else {
                        str = str.substring(0, str.indexOf(' '));
                    }
                }
                Log.i("Pinger", "Ping: " + delay[1]);
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while checking real-time latency during call: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return str;
    }

    public boolean isJobIdRunning(Context context, int JobId) {
        try {
            final JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
                if (jobInfo.getId() == JobId) {
                    return true;
                }
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while getting the running Jobs: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return false;
    }

    public void preloadBrandLogo(Context context, String url) {
        try {
            Glide.with(context).asBitmap()
                    .load(url)
                    .preload(500, 500);
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while preloading the brand logo: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public boolean isNetworkBandwidthGood() {
        final String str = Utils.getInstance().ping("www.google.com");
        if (str != null && !str.isEmpty()) {
            return Integer.parseInt(str) < 300;
        } else {
            return false;
        }
    }

    public void startAudio(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
            audioManager.setSpeakerphoneOn(true);

            vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            int result = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                ringtonePlayer = MediaPlayer.create(context, uri);
                ringtonePlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .build());
                //ringtonePlayer.setAudioStreamType(AudioManager.STREAM_RING);
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                ringtonePlayer.setVolume(currentVolume, currentVolume);
                ringtonePlayer.setLooping(true);
                ringtonePlayer.start();
                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                    long[] VIBRATE_PATTERN = { 500, 500 };
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // API 26 and above
                        vib.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, 0));
                    } else {
                        // Below API 26
                        vib.vibrate(VIBRATE_PATTERN , 0);
                    }
                }
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while playing the call ringtone: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void stopAudio(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
            audioManager.setSpeakerphoneOn(false);
            if(vib != null) {
                vib.cancel();
            }

            if(ringtonePlayer != null) {
                ringtonePlayer.stop();
                ringtonePlayer.release();
            }
        } catch (IllegalStateException e){
            //e.printStackTrace();
        }
        catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while stopping the audio: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void startTimer(final Context context, final JSONObject callDetails) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        CountDownTimer timer;
                        timer = new CountDownTimer(30000, 1000) {

                            @Override
                            public void onTick(long millisUntilFinished) {
                            }

                            @Override
                            public void onFinish() {
                                try {
                                    stopAudio(context);
                                    releaseAudio(context);
                                    callMissed(context, callDetails);
                                    if (context != null) {
                                        if(DirectIncomingCallFragment.getInstance() != null)
                                        DirectIncomingCallFragment.getInstance().getActivity().finishAndRemoveTask();
                                    }
                                } catch (Exception e) {
                                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while stopping the outgoing call timer: " + e.getLocalizedMessage());
                                    e.printStackTrace();
                                }

                            }
                        }.start();
                        DataStore.getInstance().setIncomingTimer(timer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {

        }
    }

    public void releaseAudio(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.abandonAudioFocus(afChangeListener);
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while abandoning the audio focus: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void callMissed(final Context context, JSONObject callDetails) {
        try {
            String fromCuid = callDetails.getJSONObject("from").getString("cuid");
            String toCuid = callDetails.getJSONObject("to").getString("cuid");
            String callContext = callDetails.getString(context.getString(R.string.scontext));

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                NotificationHandler.CallNotificationHandler.getInstance(context).removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
            }else {
                Utils.getInstance().dismissCallNotificationService(context);
            }

            if (fromCuid != null)
                NotificationHandler.CallNotificationHandler.getInstance(context).showCallNotification(
                        new JSONObject().
                                put("context", callContext).
                                put("fromCuid", fromCuid).
                                put("toCuid", toCuid),
                        NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);

            DataStore dataStore = DataStore.getInstance();
            String id = callDetails.getJSONObject("from").getString(context.getString(R.string.id));
            String accountId = callDetails.getString(KEY_ACCOUNT_ID);
            String callId = callDetails.getString(context.getString(R.string.scall));
            String sid = callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "";

            JSONObject data = new JSONObject();
            data.put("responseSid", id + "_" + accountId);
            data.put("callId", callId);
            data.put(context.getString(R.string.sid), sid);

            dataStore.getSocket().emit(context.getString(R.string.s_miss), data, new Ack() {
                @Override
                public void call(Object... args) {
                    //no-op
                    //Raising the system event for incoming call
                    DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callContext)
                            .setCallStatus(CallStatus.CALL_MISSED)
                            .build();
                    DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_END, dcSystemEventInfo);
                }
            });

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    DataStore.getInstance().setClientbusyOnVoIP(false);
                }
            }, 1000);
        } catch (Exception e) {

        }
    }

    public void callCancelled(Context context) {
        try {
            if (context != null) {
                stopAudio(context);
                releaseAudio(context);
                DataStore.getInstance().getIncomingTimer().cancel();

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    NotificationHandler.CallNotificationHandler.getInstance(context).
                            removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                }else {
                    dismissCallNotificationService(context);
                }
                if (DataStore.getInstance().getCallDetails() != null) {
                    String fromCuID = DataStore.getInstance().getCallDetails().getJSONObject("from").getString("cuid");
                    String toCuID = DataStore.getInstance().getCallDetails().getJSONObject("to").getString("cuid");
                    String callContext = DataStore.getInstance().getCallDetails().getString(context.getString(R.string.scontext));

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        NotificationHandler.CallNotificationHandler.getInstance(context).removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                    }

                    NotificationHandler.CallNotificationHandler.getInstance(context).showCallNotification(
                            new JSONObject().
                                    put("context", callContext).
                                    put("fromCuid", fromCuID).
                                    put("toCuid", toCuID),
                            NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);
                }
            }
        } catch (Exception ignored) {
            //no-op
        } finally {
            delayedHandler(1000, () -> DataStore.getInstance().setClientbusyOnVoIP(false));
        }
    }

    public void fcmCallDecline(Context context){
        try {
            DataStore dataStore = DataStore.getInstance();

            if (dataStore.getCallDetails() != null) {
                JSONObject callDetails = dataStore.getCallDetails();
                String id = callDetails.getJSONObject("from").getString(context.getString(R.string.id));
                String accountId = callDetails.getString(KEY_ACCOUNT_ID);
                String callId = callDetails.getString(context.getString(R.string.scall));
                String callContext = callDetails.getString("context");
                String sid = callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "";

                JSONObject data = new JSONObject();
                data.put("responseSid", id + "_" + accountId);
                data.put("callId", callId);
                data.put(context.getString(R.string.sid), sid);
                data.put(KEY_DECLINE_REASON, FcmCacheManger.getInstance().getRejectedReason());
                data.put(KEY_DECLINE_REASON_CODE, FcmCacheManger.getInstance().getRejectedReasonCode());

                dataStore.getSocket().emit(context.getString(R.string.s_decline), data, new Ack() {
                    @Override
                    public void call(Object... args) {
                        //Raising the system event for incoming call
                        DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callContext)
                                .setCallStatus(CallStatus.CALL_DECLINED)
                                .setDeclineReason(FcmCacheManger.getInstance().getRejectedReason())
                                .build();
                        DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_END, dcSystemEventInfo);
                    }
                });
            }
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        DataStore.getInstance().setClientbusyOnVoIP(false);
                    } catch (Exception e) {

                    }
                }
            }, 1000);

        }catch (Exception e){

        }
    }

    public void callDeclined(final Context context) {
        try {
            if (context == null) {
                return;
            }
            //if incoming call is routed via FCM so it can be a chance that callee answered the call before getting authenticate that time
            // role of FcmCacheManger comes into play
            if(!SocketIOManager.isSocketConnected()){
                FcmCacheManger.getInstance().setRejected(true);
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                NotificationHandler.CallNotificationHandler.getInstance(context).
                        removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
            }else{
                dismissCallNotificationService(context);
            }

            //AppUtil.getInstance().stopAudio(getActivity());;
            Utils.getInstance().stopAudio(context);
            Utils.getInstance().releaseAudio(context);
            DataStore.getInstance().getIncomingTimer().cancel();

            if (context != null && DirectIncomingCallFragment.getInstance() != null) {
                DirectIncomingCallFragment.getInstance().getActivity().finishAndRemoveTask();
            }

            DataStore dataStore = DataStore.getInstance();
            if (dataStore.getCallDetails() != null) {
                JSONObject callDetails = dataStore.getCallDetails();
                String id = callDetails.getJSONObject("from").getString(context.getString(R.string.id));
                String accountId = callDetails.getString(KEY_ACCOUNT_ID);
                String call = callDetails.getString(context.getString(R.string.scall));
                String sid =  callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "";
                JSONObject data = new JSONObject();
                data.put("responseSid", id + "_" + accountId);
                data.put("callId", call);
                data.put(context.getString(R.string.sid), sid);

                dataStore.getSocket().emit(context.getString(R.string.s_decline), data, new Ack() {
                    @Override
                    public void call(Object... args) {
                    }
                });

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DataStore.getInstance().setClientbusyOnVoIP(false);
                        } catch (Exception e) {

                        }
                    }
                }, 1000);
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while sending the call decline event: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void lightUpScreen(Context context) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isScreenOn();
            Log.e("screen on.......", "" + isScreenOn);
            if (!isScreenOn) {
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "DirectCall:MyLock");
                if (!wl.isHeld()) {
                    wl.acquire(30000);
                }
                PowerManager.WakeLock wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DirectCall:MyCpuLock");
                if (!wl_cpu.isHeld()) {
                    wl_cpu.acquire(30000);
                }
            }
        } catch (Exception e) {
            //no-op
        }
    }

    public int compareVersionNames(String oldVersionName, String newVersionName) {
        int res = 0;

        try {
            String[] oldNumbers = oldVersionName.split("\\.");
            String[] newNumbers = newVersionName.split("\\.");

            // To avoid IndexOutOfBounds
            int maxIndex = Math.min(oldNumbers.length, newNumbers.length);

            for (int i = 0; i < maxIndex; i ++) {
                int oldVersionPart = Integer.valueOf(oldNumbers[i]);
                int newVersionPart = Integer.valueOf(newNumbers[i]);

                if (oldVersionPart < newVersionPart) {
                    res = -1;
                    break;
                } else if (oldVersionPart > newVersionPart) {
                    res = 1;
                    break;
                }
            }

            // If versions are the same so far, but they have different length...
            if (res == 0 && oldNumbers.length != newNumbers.length) {
                res = (oldNumbers.length > newNumbers.length)?1:-1;
            }

            return res;
        }catch (Exception e){
            return res;

        }
    }

    public void removeNeverAskAgain(Context context){
        try {
            if (context != null){
                SharedPreferences sharedPref = StorageHelper.getPreferences(context);
                //removing direct_call_never_ask_again from prefs if permission is granted somehow from permission settings.
                if(Utils.getInstance().hasPermissions(context, Manifest.permission.RECORD_AUDIO)){
                    StorageHelper.remove(context, KEY_NEVER_ASK_AGAIN_PERMISSION);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dismissCallNotificationService(Context context){
        try {
            if(CallNotificationService.getInstance()!=null){
                Intent intent = new Intent(context, CallNotificationService.class);
                context.stopService(intent);
            }
        }catch (Exception e)
        {
            //no-op
        }
    }

    public void sendBroadcast(Context context, String action){
        try {
            Intent intent = new Intent(action);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }catch (Exception e){
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while broadcasting internal message: " + e.getLocalizedMessage());
        }
    }

    public void markNotificationDelivered(Context context, String notificationId) {
        try {
            DataStore dataStore = DataStore.getInstance();

            if (dataStore.getCallDetails() != null) {
                String fcmNotificationId = notificationId;
                JSONObject data = new JSONObject();
                data.put("notificationId", fcmNotificationId);
                //Log.d("DirectCallNotification", "emitting fcm_notification_ack :" + data);
                dataStore.getSocket().emit(context.getString(R.string.fcm_notification_ack), data, new Ack() {
                    @Override
                    public void call(Object... args) {
                        //Log.d("DirectCallNotification", "args are : "+ args[0]);
                    }
                });
            }
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        DataStore.getInstance().setClientbusyOnVoIP(false);
                    } catch (Exception e) {
                        //no-op
                    }
                }
            }, 1000);

        }catch (Exception ignored){

        }
    }

    public static void storeMissedCallActionsList(Context context, String key, List<MissedCallActions> value){
        JSONArray jsonArray = new JSONArray();
        for(MissedCallActions missedCallActions: value){
            JSONObject jsonObject = missedCallActions.toJson();
            jsonArray.put(jsonObject);
        }
        StorageHelper.putString(context, key, jsonArray.toString());
    }

    public static List<MissedCallActions> getMissedCallActionsList(Context context, String key) {
        List<MissedCallActions> missedCallActions = new ArrayList<>();
        try {
            JSONArray missedCallActionsArr;
            String json = StorageHelper.getString(context, key, null);
            if(json != null){
                missedCallActionsArr = new JSONArray(json);
                if(missedCallActionsArr.length() > 0){
                    for (int index = 0; index < missedCallActionsArr.length(); index++) {
                        JSONObject jsonObj = missedCallActionsArr.getJSONObject(index);
                        missedCallActions.add(MissedCallActions.fromJson(jsonObj));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return missedCallActions;
    }

    public float dipToPx(Context context, float dipValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return dipValue * density;
    }

    public void delayedHandler(int delay, Runnable runnable) {
        new Handler(Looper.getMainLooper()).postDelayed(runnable, delay);
    }

    public Boolean isAppInBackground(){
        ActivityManager.RunningAppProcessInfo myProcess = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(myProcess);
        return myProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
    }

    public final String trimDoubleQuotes(String str){
        return str.replace("\"", "");
    }

    /**
     * Creates {@link com.clevertap.android.sdk.CleverTapAPI} instance if it's null and initializes
     * Geofence SDK, mostly in killed state.
     * <br>
     * <b>Must be called from background thread</b>
     *
     * @param context application {@link Context}
     * @return true if CleverTap sdk initialized successfully, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @WorkerThread
    public boolean initCleverTapApiIfRequired(@NonNull Context context, String accountId) {

        DirectCallAPI directCallAPIInstance = DirectCallAPI.getInstance();

        if (directCallAPIInstance.getCleverTapApi() == null) {
            if(accountId != null) {
                CleverTapAPI cleverTapAPI = CleverTapAPI.getGlobalInstance(context, accountId);

                if (cleverTapAPI == null) {
                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX,
                            "Critical issue :: After calling CleverTapAPI.getGlobalInstance, also init is failed! Dropping this call");
                    return false;
                }else {
                    DirectCallAPI.getInstance().setCleverTapApi(cleverTapAPI);
                }
            }else {
                return false;
            }
        }
        return true;
    }

    //returns the number of seconds passed since the Unix epoch (January 1, 1970 00:00:00 UTC/GMT)
    public long getNow(){
        return System.currentTimeMillis() / 1000;
    }

    public void startJobServiceForSigSock(Context context) {
        try {
            if (!Utils.getInstance().isJobIdRunning(context, 10) || !SocketIOManager.isSocketConnected()) {
                JobSchedulerSocketService jobSchedulerSocketService = JobSchedulerSocketService.getInstance(context);
                JobInfo myJob = new JobInfo.Builder(10, new ComponentName(context, jobSchedulerSocketService.getClass()))
                        .setBackoffCriteria(4000, JobInfo.BACKOFF_POLICY_LINEAR)
                        .setPersisted(true)
                        .setMinimumLatency(1)
                        .setOverrideDeadline(1)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .build();

                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                if (jobScheduler != null) {
                    jobScheduler.schedule(myJob);
                }
            }
        } catch (Exception e) {
            //no-op
        }
    }

    public void resetSession(Context context) {
        stopSigSockService(context);
        SocketIOManager.resetSocketConfiguration();
        DirectCallAPI.getInstance().logout(context);
    }

    private void stopSigSockService(Context context) {
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancelAll();
            }
        } catch (Exception e) {
            //no-op
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public void setCommunicationDevice(Context context, Integer targetDeviceType) {
        if(context == null) {
            DirectCallAPI.getLogger().debug(
                    CALLING_LOG_TAG_SUFFIX,
                    "context is null, failed to set communication device"
            );
            return;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        List<AudioDeviceInfo> devices = audioManager.getAvailableCommunicationDevices();
        for (AudioDeviceInfo device : devices) {
            if (device.getType() == targetDeviceType) {
                boolean result = audioManager.setCommunicationDevice(device);
                DirectCallAPI.getLogger().debug(
                        CALLING_LOG_TAG_SUFFIX,
                        "AUDIO_MANAGER: " + "setCommunicationDevice type:" + targetDeviceType + " result:" + result
                );
            }
        }
    }

    public Animation getAlphaAnimator() {
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(800);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        return anim;
    }
}
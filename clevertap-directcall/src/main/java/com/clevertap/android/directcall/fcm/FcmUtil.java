package com.clevertap.android.directcall.fcm;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.ENABLE_HTTP_LOG;
import static com.clevertap.android.directcall.Constants.HTTP_CONNECT_TIMEOUT;
import static com.clevertap.android.directcall.Constants.HTTP_MAX_RETRIES;
import static com.clevertap.android.directcall.Constants.HTTP_READ_TIMEOUT;
import static com.clevertap.android.directcall.Constants.HTTP_RETRY_DELAY;
import static com.clevertap.android.directcall.Constants.REASON_CODE_INVALID_CUID;
import static com.clevertap.android.directcall.Constants.KEY_ACCESS_TOKEN;
import static com.clevertap.android.directcall.Constants.KEY_ACCOUNT_ID;
import static com.clevertap.android.directcall.Constants.KEY_API_KEY;
import static com.clevertap.android.directcall.Constants.KEY_BRAND_LOGO;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_CC;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_CUID;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_PHONE;
import static com.clevertap.android.directcall.Constants.KEY_DECLINE_REASON;
import static com.clevertap.android.directcall.Constants.KEY_DECLINE_REASON_CODE;
import static com.clevertap.android.directcall.Constants.KEY_JWT_TOKEN;
import static com.clevertap.android.directcall.Constants.KEY_NEVER_ASK_AGAIN_PERMISSION;
import static com.clevertap.android.directcall.Constants.KEY_RETRY_DELAY;
import static com.clevertap.android.directcall.Constants.KEY_SECONDARY_BASE_URL;
import static com.clevertap.android.directcall.Constants.REASON_CODE_MICROPHONE_PERMISSION_NOT_GRANTED;
import static com.clevertap.android.directcall.Constants.REASON_CODE_USER_BUSY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.Nullable;

import com.clevertap.android.directcall.ApiEndPoints;
import com.clevertap.android.directcall.enums.CallStatus;
import com.clevertap.android.directcall.Constants;
import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.directcall.ui.DirectCallingActivity;
import com.clevertap.android.directcall.events.CTSystemEvent;
import com.clevertap.android.directcall.events.DCSystemEventInfo;
import com.clevertap.android.directcall.exception.InitException;
import com.clevertap.android.directcall.ui.DirectIncomingCallFragment;
import com.clevertap.android.directcall.ui.DirectOngoingCallFragment;
import com.clevertap.android.directcall.interfaces.DirectCallInitResponse;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.network.Http;
import com.clevertap.android.directcall.network.HttpMethod;
import com.clevertap.android.directcall.network.StringListener;
import com.clevertap.android.directcall.services.CallNotificationService;
import com.clevertap.android.directcall.utils.JwtUtil;
import com.clevertap.android.directcall.utils.NotificationHandler;
import com.clevertap.android.directcall.utils.SocketIOManager;
import com.clevertap.android.directcall.utils.Utils;
import com.clevertap.android.sdk.CleverTapAPI;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.socket.client.Ack;

public class FcmUtil {
    private static FcmUtil ourInstance;
    String cc, phone, accountId, apikey, currentCuid;
    String jwt = "";

    private String expiredCallId = null, fcmNotificationID = null;

    public static FcmUtil getInstance(Context context) {
        if(ourInstance==null){
           ourInstance = new FcmUtil();
        }
        return ourInstance;
    }

    public String getExpiredCallId() {
        return expiredCallId;
    }

    public void resetExpiredCallId(String expiredCallId) {
        this.expiredCallId = expiredCallId;
    }

    public String getFcmNotificationID() {
        return fcmNotificationID;
    }

    public void setFcmNotificationID(String fcmNotificationID) {
        this.fcmNotificationID = fcmNotificationID;
    }

    private FcmUtil() {
    }
    
    public void onIncomingCall(final Context context, final JSONObject incomingdata){
        JwtUtil.getInstance(context).verifyToken(context, true, new DirectCallInitResponse() {
            @Override
            public void onSuccess() {
                try {
                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "parsing VoIP push payload");

                    prepareSdkForVoIpPush(context);

                    String callId = incomingdata.getString("call");
                    String callContext = incomingdata.getString("context");

                    //Raising the system event for incoming call
                    DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callContext).build();
                    DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_INCOMING, dcSystemEventInfo);

                    //Further processing
                    SharedPreferences prefs = StorageHelper.getPreferences(context);
                    phone = prefs.getString(KEY_CONTACT_PHONE, "");
                    cc = prefs.getString(KEY_CONTACT_CC, "");
                    accountId = prefs.getString(KEY_ACCOUNT_ID, null);
                    apikey = prefs.getString(KEY_API_KEY, null);
                    currentCuid = prefs.getString(KEY_CONTACT_CUID, "");
                    jwt = prefs.getString(KEY_JWT_TOKEN, "");
                    String accountId = prefs.getString(KEY_ACCOUNT_ID, "");


                    incomingdata.put(KEY_ACCOUNT_ID, accountId);
                    incomingdata.put(KEY_API_KEY, prefs.getString(KEY_API_KEY, null));
                    incomingdata.put(KEY_CONTACT_PHONE, prefs.getString(KEY_CONTACT_PHONE, null));
                    incomingdata.put(KEY_CONTACT_CC, prefs.getString(KEY_CONTACT_CC, null));
                    incomingdata.put(KEY_JWT_TOKEN, jwt);
                    incomingdata.put(KEY_CONTACT_CUID, currentCuid);
                    incomingdata.put(context.getString(R.string.callType), "incoming");
                    if (currentCuid.length() > 0) {
                        incomingdata.put("initiatorData", currentCuid);
                    } else {
                        incomingdata.put("initiatorData", cc + phone);
                    }

                    String sid = incomingdata.has("sid")? incomingdata.getString("sid"):"";

                    //setting calldetails, callId, sid to singleton for every call
                    DataStore.getInstance().setCallDetails(incomingdata);
                    DataStore.getInstance().setSid(sid);
                    DataStore.getInstance().setCallId(callId);

                    DataStore.getInstance().setAppContext(context);


                    String incomingCuid = incomingdata.getJSONObject("to").getString("cuid");
                    String fromId = incomingdata.getJSONObject("from").getString(context.getString(R.string.id));

                    if(!incomingCuid.equals(currentCuid)){
                       // return;
                        incomingdata.put("responseSid", fromId + "_" + accountId);
                        incomingdata.put("callId", callId);
                        incomingdata.put(context.getString(R.string.sid), sid);
                        incomingdata.put(Constants.KEY_DECLINE_REASON, context.getString(R.string.invalid_cuid));
                        incomingdata.put(Constants.KEY_DECLINE_REASON_CODE, REASON_CODE_INVALID_CUID);
                        if (SocketIOManager.getSocket() != null && SocketIOManager.isSocketConnected()) {
                            SocketIOManager.getSocket().emit(context.getString(R.string.s_decline), incomingdata, new Ack() {
                                @Override
                                public void call(Object... args) {
                                    //Raising the system event for incoming call
                                    DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callContext)
                                            .setCallStatus(CallStatus.CALL_DECLINED)
                                            .setDeclineReason(CallStatus.DeclineReason.INVALID_CUID)
                                            .build();

                                    DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_END, dcSystemEventInfo);
                                }
                            });
                        }else {
                            FcmCacheManger.getInstance().setRejected(true);
                            FcmCacheManger.getInstance().setRejectedReason(CallStatus.DeclineReason.INVALID_CUID);
                            FcmCacheManger.getInstance().setRejectedReasonCode(401);
                        }
                        return;
                    }

                    if(context != null){
                        Utils.getInstance().removeNeverAskAgain(context);
                        Boolean isNeverAskAgain = StorageHelper.getBoolean(context, KEY_NEVER_ASK_AGAIN_PERMISSION, false);
                        if(isNeverAskAgain){
                            JSONObject data = new JSONObject();
                            data.put("responseSid", fromId + "_" + accountId);
                            data.put("callId", callId);
                            data.put(context.getString(R.string.sid), sid);
                            if(isNeverAskAgain){
                                data.put(KEY_DECLINE_REASON, context.getString(R.string.microphone_permission_not_granted));
                                data.put(KEY_DECLINE_REASON_CODE, REASON_CODE_MICROPHONE_PERMISSION_NOT_GRANTED);
                            }
                            if (SocketIOManager.getSocket() != null && SocketIOManager.isSocketConnected()) {
                                SocketIOManager.getSocket().emit(context.getString(R.string.s_decline), data, new Ack() {
                                    @Override
                                    public void call(Object... args) {
                                        //Raising the system event for incoming call
                                        DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callContext)
                                                .setCallStatus(CallStatus.CALL_DECLINED)
                                                .setDeclineReason(CallStatus.DeclineReason.MICROPHONE_PERMISSION_NOT_GRANTED)
                                                .build();

                                        DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_END, dcSystemEventInfo);
                                    }
                                });
                            }else {
                                FcmCacheManger.getInstance().setRejected(true);
                                FcmCacheManger.getInstance().setRejectedReason(CallStatus.DeclineReason.MICROPHONE_PERMISSION_NOT_GRANTED);
                                FcmCacheManger.getInstance().setRejectedReasonCode(401);
                            }
                            return;
                        }
                    }

                    //this user is busy on other voip or pstn call so decline with reasonCode-REASON_CODE_USER_BUSY
                    if (DataStore.getInstance().isClientbusyOnVoIP() || DataStore.getInstance().isClientbusyOnPstn()) {
                        incomingdata.put("responseSid", fromId + "_" + accountId);
                        incomingdata.put("callId", callId);
                        incomingdata.put(context.getString(R.string.sid), sid);
                        incomingdata.put(KEY_DECLINE_REASON, context.getString(R.string.client_busy));
                        incomingdata.put(KEY_DECLINE_REASON_CODE, REASON_CODE_USER_BUSY);
                        if (SocketIOManager.getSocket() != null && SocketIOManager.isSocketConnected()) {
                            SocketIOManager.getSocket().emit(context.getString(R.string.s_decline), incomingdata, new Ack() {
                                @Override
                                public void call(Object... args) {

                                }
                            });
                        }else {
                            FcmCacheManger.getInstance().setRejected(true);
                            FcmCacheManger.getInstance().setRejectedReason(CallStatus.DeclineReason.USER_BUSY);
                            FcmCacheManger.getInstance().setRejectedReasonCode(REASON_CODE_USER_BUSY);
                        }
                    } else {
                        DataStore.getInstance().setClientbusyOnVoIP(true);
//                        SocketInit.getInstance().setCallDetails(incomingdata);
//                        SocketInit.getInstance().setSid(sid);

                        Utils.getInstance().lightUpScreen(context);
                        JSONObject incomingNotificationData = new JSONObject();
                        incomingNotificationData.put(context.getString(R.string.callDetails), incomingdata.toString());
                        //incomingNotificationData.put(context.getString(R.string.screen), context.getString(R.string.incoming));
                        incomingNotificationData.put(context.getString(R.string.sid), sid);
                        incomingNotificationData.put("context", incomingdata.getString("context"));

                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            NotificationHandler.CallNotificationHandler.getInstance(context).
                                    showCallNotification(incomingNotificationData,
                                            NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                            Utils.getInstance().startAudio(context);
                            Utils.getInstance().startTimer(context, incomingdata);
                        } else {
                            Intent incomingCallNotificationIntent;
                            incomingCallNotificationIntent = new Intent(context, CallNotificationService.class);
                            incomingCallNotificationIntent.putExtra("incomingNotificationData", incomingNotificationData.toString());
                            context.startForegroundService(incomingCallNotificationIntent);
                            Utils.getInstance().startTimer(context, incomingdata);
                        }

                        try {
                            Utils.getInstance().sendBroadcast(context, Constants.ACTION_INCOMING_CALL);
                        } catch (Exception e) {
                            //no-op
                        }

                        DataStore.getInstance().setRecording(true);
                        Intent i = new Intent(context, DirectCallingActivity.class);
                        i.putExtra(context.getString(R.string.callDetails), incomingdata.toString());
                        i.putExtra(context.getString(R.string.screen), context.getString(R.string.incoming));
                        i.putExtra(context.getString(R.string.sid), sid);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        context.startActivity(i);
                    }
                }catch (Exception e){
                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in handling the FCM call payload: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(InitException initException) {
                //do nothing or can return invalid (false) value to primary notification Handler
            }
        });

    }

    private void prepareSdkForVoIpPush(Context context) {
        if (SocketIOManager.getSocket() != null) {
            if (!SocketIOManager.isSocketConnected()) {
                SocketIOManager.getSocket().connect();
            } else {
                return;
            }
        } else {
            Utils.getInstance().startJobServiceForSigSock(context);
        }
        //preloading brand__logo
        if (context != null) {
            SharedPreferences sharedPref = StorageHelper.getPreferences(context);
            String logoUrl = sharedPref.getString(KEY_BRAND_LOGO, null);
            Utils.getInstance().preloadBrandLogo(context, logoUrl);
        }
    }

    public void onCancelCall(Context context, String callId){
        if(DirectIncomingCallFragment.getInstance()!=null){
            DirectIncomingCallFragment.getInstance().onCancel();
        }else if(DirectOngoingCallFragment.getInstance()!=null){
            DirectOngoingCallFragment.getInstance().closeCallScreen();
            updateCallStatus(context, callId);
        }
        expiredCallId = callId;
    }

    public void updateCallStatus(Context context, String callId) {
        try {
            CleverTapAPI cleverTapAPI = DirectCallAPI.getInstance().getCleverTapApi();
            String authToken = StorageHelper.getString(context, KEY_ACCESS_TOKEN, null);
            String baseUrl = StorageHelper.getString(context, KEY_SECONDARY_BASE_URL, null);
            int retryDelay = StorageHelper.getInt(context, KEY_RETRY_DELAY, HTTP_RETRY_DELAY);
            if (cleverTapAPI != null && authToken != null && baseUrl != null) {
                JSONObject voiceCallStatus = new JSONObject();
                voiceCallStatus.put("status", "cancelled");
                new Http.Request(cleverTapAPI, HttpMethod.PATCH)
                        .url(baseUrl + ApiEndPoints.PATCH_UPDATE_CALL_STATUS)
                        .header("Authorization", authToken)
                        .pathParameter("id", callId)
                        .queryParameter("accountId", accountId)
                        .body(voiceCallStatus)
                        .withBackoffCriteria(HTTP_MAX_RETRIES, retryDelay, TimeUnit.SECONDS)
                        .backoffCriteriaFailedListener(DirectCallAPI.getInstance().getBackoffCriteriaFailedListener())
                        .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
                        .enableLog(ENABLE_HTTP_LOG)
                        .execute(new StringListener() {
                            @Override
                            public void onResponse(@Nullable String response, int responseCode, Boolean isSuccessful) {
                                //no-op
                            }

                            @Override
                            public void onFailure(@Nullable Exception e) {
                                //no-op
                            }
                        });
            }
        }catch (Exception e){
            //no-op
        }
    }
}

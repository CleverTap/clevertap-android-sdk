package com.clevertap.android.directcall.javaclasses;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import com.clevertap.android.directcall.Constants;
import com.clevertap.android.directcall.interfaces.CallType;
import com.clevertap.android.directcall.interfaces.DirectCallInitResponse;
import com.clevertap.android.directcall.interfaces.ICallStatusHandler;
import com.clevertap.android.directcall.interfaces.OnMissedCallHostSetReceiver;
import com.clevertap.android.directcall.interfaces.OutgoingCallResponse;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.models.PushAPF;
import com.clevertap.android.directcall.models.MissedCallActions;

import org.json.JSONObject;

import java.util.List;

import io.socket.client.Socket;

/**
 * this is a singleton class mainly used to store values during a call.
 */

public class DataStore {

    private static DataStore instance = null;
    private Socket socket;
    private String callData;
    private String jwt, phone, cc, accountId, apikey;
    private ICallStatusHandler ICallStatusHandler;
    private Activity outgoingActivity;
    private CountDownTimer outgoingTimer, incomingTimer;
    private AudioManager audioManager;
    private Context context, appCtx;
    private JSONObject initJsonOptions;
    private ICallStatusHandler.incomingCallStatus incomingCallStatus;
    private String callId;
    private OutgoingCallResponse outgoingCallResponse;
    private CallType callType;
    private String callDirection;
    private Boolean isRecording;
    private JSONObject cli;
    private PushAPF pushAPF;
    private DirectCallInitResponse directCallInitResponse;
    DirectCallAPI.MissedCallNotificationOpenedHandler missedCallNotificationOpenedHandler;
    Boolean isClientbusyOnVoIP = false, isClientbusyOnPstn = false;
    Boolean isCalleBusyOnAnotherCall = false;
    Boolean readPhoneStatePermission = false;
    JSONObject callDetails = new JSONObject();
    String sid = "";
    String sna;
    String fromCuid, toCuid;
    String callContext;
    List<MissedCallActions> missedCallReceiverActions, missedCallInitiatorActions;

    private Boolean isScratchCardScratched = false;

    private DataStore(){

    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public Boolean isScratchCardScratched() {
        return isScratchCardScratched;
    }

    public void setScratchCardScratched(Boolean scratchCardScratched) {
        isScratchCardScratched = scratchCardScratched;
    }

    public String getFromCuid() {
        return fromCuid;
    }

    public void setFromCuid(String fromCuid) {
        this.fromCuid = fromCuid;
    }

    public String getToCuid() {
        return toCuid;
    }

    public void setToCuid(String toCuid) {
        this.toCuid = toCuid;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public JSONObject getCallDetails() {
        return callDetails;
    }

    public void setCallDetails(JSONObject callDetails) {
        this.callDetails = callDetails;
    }

    public PushAPF getIosAPF() {
        return pushAPF;
    }

    public void setIosAPF(PushAPF pushAPF) {
        this.pushAPF = pushAPF;
    }

    public JSONObject getCli() {
        return cli;
    }

    public void setCli(JSONObject cli) {
        this.cli = cli;
    }

    public Boolean getRecording() {
        return isRecording;
    }

    public void setRecording(Boolean recording) {
        isRecording = recording;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    public OutgoingCallResponse getOutgoingCallResponse() {
        return outgoingCallResponse;
    }

    public void setOutgoingCallResponse(OutgoingCallResponse outgoingCallResponse) {
        this.outgoingCallResponse = outgoingCallResponse;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public ICallStatusHandler.incomingCallStatus getIncomingCallStatus() {
        return incomingCallStatus;
    }

    public void setIncomingCallStatus(ICallStatusHandler.incomingCallStatus incomingCallStatus) {
        this.incomingCallStatus = incomingCallStatus;
    }

    public Context getContext() {
        return context;
    }

    public Context getAppContext(){
        return appCtx;
    }

    public void setAppContext(Context appCtx) {
        this.appCtx = appCtx;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public CountDownTimer getTimer() {
        return outgoingTimer;
    }

    public void setTimer(CountDownTimer timer) {
        this.outgoingTimer = timer;
    }

    public CountDownTimer getIncomingTimer() {
        return incomingTimer;
    }

    public void setIncomingTimer(CountDownTimer incomingTimer) {
        this.incomingTimer = incomingTimer;
    }

    public Activity getOutgoingActivity() {
        return outgoingActivity;
    }

    public void setOutgoingActivity(Activity outgoingActivity) {
        this.outgoingActivity = outgoingActivity;
    }

    public ICallStatusHandler getCallStatus() {
        return ICallStatusHandler;
    }

    public DirectCallAPI.MissedCallNotificationOpenedHandler getMissedCallNotificationOpenedHandler() {
        return missedCallNotificationOpenedHandler;
    }

    public void setMissedCallInitiatorHost(final String missedCallInitiatorHostPackage, final OnMissedCallHostSetReceiver onMissedCallHostSetReceiver) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Object missedCallHostClass;
                    missedCallHostClass = Class.forName(missedCallInitiatorHostPackage).newInstance();
                    missedCallNotificationOpenedHandler = (DirectCallAPI.MissedCallNotificationOpenedHandler) missedCallHostClass;
                    onMissedCallHostSetReceiver.onSetMissedCallReceiver(missedCallNotificationOpenedHandler);
                } catch (Exception e) {
                    //no-op
                    DirectCallAPI.getLogger().debug(Constants.CALLING_LOG_TAG_SUFFIX, "Exception when missed call action button is clicked: " + e.getLocalizedMessage());
                }finally {
                    onMissedCallHostSetReceiver.onSetMissedCallReceiver(null);
                }
            }
        });
    }

    public void setMissedCallReceiverHost(final String missedCallReceiverHostPackage, final OnMissedCallHostSetReceiver onMissedCallHostSetReceiver) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Object missedCallHostClass;
                    missedCallHostClass = Class.forName(missedCallReceiverHostPackage).newInstance();
                    missedCallNotificationOpenedHandler = (DirectCallAPI.MissedCallNotificationOpenedHandler) missedCallHostClass;
                    onMissedCallHostSetReceiver.onSetMissedCallReceiver(missedCallNotificationOpenedHandler);
                } catch (Exception e) {
                    //no-op
                    DirectCallAPI.getLogger().debug(Constants.CALLING_LOG_TAG_SUFFIX, "Exception when missed call action button is clicked: " + e.getLocalizedMessage());
                }finally {
                    onMissedCallHostSetReceiver.onSetMissedCallReceiver(null);
                }
            }
        });
    }

    public void setCallStatus(ICallStatusHandler ICallStatusHandler) {
        this.ICallStatusHandler = ICallStatusHandler;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public  void setCallData(String callData) {
        this.callData = callData;
    }

    public String getCallData() {
        return this.callData;
    }

    public JSONObject getInitJsonOptions() {
        return initJsonOptions;
    }

    public void setInitJsonOptions(JSONObject initJsonOptions) {
        this.initJsonOptions = initJsonOptions;
    }

    public DirectCallInitResponse getDirectCallInitResponse() {
        return directCallInitResponse;
    }

    public void setDirectCallInitResponse(DirectCallInitResponse directCallInitResponse) {
        this.directCallInitResponse = directCallInitResponse;
    }

    public Boolean isClientbusyOnVoIP() {
        return isClientbusyOnVoIP;
    }

    public void setClientbusyOnVoIP(Boolean clientbusyOnCall) {
        isClientbusyOnVoIP = clientbusyOnCall;
    }

    public Boolean isClientbusyOnPstn() {
        return isClientbusyOnPstn;
    }

    public void setClientbusyOnPstn(Boolean clientbusyOnPstn) {
        isClientbusyOnPstn = clientbusyOnPstn;
    }

    public Boolean isCalleBusyOnAnotherCall() {
        return isCalleBusyOnAnotherCall;
    }

    public void setCalleBusyOnAnotherCall(Boolean calleBusyOnAnotherCall) {
        isCalleBusyOnAnotherCall = calleBusyOnAnotherCall;
    }

    public String getSna() {
        return sna;
    }

    public void setSna(String sna) {
        this.sna = sna;
    }

    public Boolean getReadPhoneStatePermission() {
        return readPhoneStatePermission;
    }

    public void setReadPhoneStatePermission(Boolean readPhoneStatePermission) {
        this.readPhoneStatePermission = readPhoneStatePermission;
    }

    public void setMissedCallReceiverActions(List<MissedCallActions> missedCallActions) {
        this.missedCallReceiverActions = missedCallActions;
    }

    public List<MissedCallActions> getMissedCallReceiverActions(){
        return missedCallReceiverActions;
    }

    public List<MissedCallActions> getMissedCallInitiatorActions() {
        return missedCallInitiatorActions;
    }

    public void setMissedCallInitiatorActions(List<MissedCallActions> missedCallInitiatorActions) {
        this.missedCallInitiatorActions = missedCallInitiatorActions;
    }

    public String getCallDirection() {
        return callDirection;
    }

    public void setCallDirection(String callDirection) {
        this.callDirection = callDirection;
    }

    public String getCallContext() {
        return callContext;
    }

    public void setCallContext(String callContext) {
        this.callContext = callContext;
    }
}

package com.clevertap.android.directcall.services;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.clevertap.android.directcall.Constants.ACTION_CONNECTION_STATUS;
import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.IO_SERVER_DISCONNECT;
import static com.clevertap.android.directcall.Constants.KEY_ACCOUNT_ID;
import static com.clevertap.android.directcall.Constants.KEY_API_KEY;
import static com.clevertap.android.directcall.Constants.KEY_BRAND_LOGO;
import static com.clevertap.android.directcall.Constants.KEY_CALL_CONTEXT;
import static com.clevertap.android.directcall.Constants.KEY_CALL_DATA;
import static com.clevertap.android.directcall.Constants.KEY_CALL_HOST;
import static com.clevertap.android.directcall.Constants.KEY_CALL_ID;
import static com.clevertap.android.directcall.Constants.KEY_CALL_TYPE;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_CC;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_CUID;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_PHONE;
import static com.clevertap.android.directcall.Constants.KEY_DECLINE_REASON;
import static com.clevertap.android.directcall.Constants.KEY_DECLINE_REASON_CODE;
import static com.clevertap.android.directcall.Constants.KEY_FROM_DATA;
import static com.clevertap.android.directcall.Constants.KEY_INITIATOR_DATA;
import static com.clevertap.android.directcall.Constants.KEY_JWT_TOKEN;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_INITIATOR_ACTIONS;
import static com.clevertap.android.directcall.Constants.KEY_NEVER_ASK_AGAIN_PERMISSION;
import static com.clevertap.android.directcall.Constants.KEY_PLATFORM;
import static com.clevertap.android.directcall.Constants.KEY_PSTN;
import static com.clevertap.android.directcall.Constants.KEY_RECORDING;
import static com.clevertap.android.directcall.Constants.KEY_SNA;
import static com.clevertap.android.directcall.Constants.KEY_STATUS;
import static com.clevertap.android.directcall.Constants.KEY_TAGS;
import static com.clevertap.android.directcall.Constants.KEY_TO_Data;
import static com.clevertap.android.directcall.Constants.KEY_VAR1;
import static com.clevertap.android.directcall.Constants.KEY_VAR2;
import static com.clevertap.android.directcall.Constants.KEY_VAR3;
import static com.clevertap.android.directcall.Constants.KEY_VAR4;
import static com.clevertap.android.directcall.Constants.KEY_VAR5;
import static com.clevertap.android.directcall.Constants.KEY_WEBHOOK;
import static com.clevertap.android.directcall.Constants.MAKE_CALL_ERR_CODE_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL;
import static com.clevertap.android.directcall.Constants.MAKE_CALL_ERR_CODE_RECEIVER_NOT_REACHABLE;
import static com.clevertap.android.directcall.Constants.MAKE_CALL_ERR_MSG_INVALID_CALL_TOKEN;
import static com.clevertap.android.directcall.Constants.MAKE_CALL_ERR_MSG_MALFORMED_JWT;
import static com.clevertap.android.directcall.Constants.MAKE_CALL_ERR_MSG_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL;
import static com.clevertap.android.directcall.Constants.MAKE_CALL_TIMEOUT;
import static com.clevertap.android.directcall.Constants.OUTGOING_CALL_TIME_DURATION;
import static com.clevertap.android.directcall.Constants.REASON_CODE_INVALID_CUID;
import static com.clevertap.android.directcall.Constants.REASON_CODE_MICROPHONE_PERMISSION_NOT_GRANTED;
import static com.clevertap.android.directcall.Constants.REASON_CODE_USER_BUSY;
import static com.clevertap.android.directcall.Constants.SIG_SOCK_PING_TIMEOUT;
import static com.clevertap.android.directcall.Constants.SOCKET_NO_ACK;
import static com.clevertap.android.directcall.Constants.SUCCESS_SDK_CONNECTED;
import static com.clevertap.android.directcall.Constants.TRANSPORT_ERROR;
import static com.clevertap.android.directcall.init.DirectCallAPI.getLogger;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.clevertap.android.directcall.Constants;
import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.directcall.enums.CallStatus;
import com.clevertap.android.directcall.events.CTSystemEvent;
import com.clevertap.android.directcall.events.DCSystemEventInfo;
import com.clevertap.android.directcall.exception.CallException;
import com.clevertap.android.directcall.exception.InitException;
import com.clevertap.android.directcall.fcm.FcmCacheManger;
import com.clevertap.android.directcall.fcm.FcmUtil;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.interfaces.ICallStatusHandler;
import com.clevertap.android.directcall.interfaces.PushApfInterface;
import com.clevertap.android.directcall.interfaces.OutgoingCallResponse;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.models.PushAPF;
import com.clevertap.android.directcall.models.MissedCallActions;
import com.clevertap.android.directcall.recievers.ConnectivityReceiver;
import com.clevertap.android.directcall.ui.DirectCallingActivity;
import com.clevertap.android.directcall.ui.DirectIncomingCallFragment;
import com.clevertap.android.directcall.ui.DirectOngoingCallFragment;
import com.clevertap.android.directcall.utils.AckWithTimeOut;
import com.clevertap.android.directcall.utils.CustomHandler;
import com.clevertap.android.directcall.utils.NotificationHandler;
import com.clevertap.android.directcall.utils.SocketIOManager;
import com.clevertap.android.directcall.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class JobSchedulerSocketService extends JobService implements ConnectivityReceiver.ConnectivityReceiverListener {
    private Socket socket;
    private String cc, phone, accountId, apikey, cuid;
    private static String jwt = "";
    private DataStore dataStore;
    ICallStatusHandler ICallStatusHandler;
    CountDownTimer outgoingTimer;
    private static Boolean isUnAuthorized = false;
    CustomHandler customHandler = CustomHandler.getInstance();
    private ConnectivityReceiver mConnectivityReceiver;
    private int transportDisconnectTimer = 0;
    private CountDownTimer transportErrorCountDownTimer;
    private Handler countDownHandler;
    private Boolean isDisconnectDueToTransportError = false;
    private final Timer reconnectScheduler = new Timer();
    private int reconnectingCount = 0;
    private final int MAX_RETRIES_AFTER_UN_AUTHORIZED = 3;

    private static JobSchedulerSocketService instance;
    private static Context context;

    public static JobSchedulerSocketService getInstance(Context ctx) {
        if (instance == null) {
            instance = new JobSchedulerSocketService();
            context = ctx;
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mConnectivityReceiver = new ConnectivityReceiver(this);
        try {
            registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

            countDownHandler = new Handler(Looper.getMainLooper());
        } catch (Exception e) {
            getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while registering the network receiver: " + e.getLocalizedMessage());
            e.printStackTrace();
        }

        //preloading brand_logo
        if (getApplicationContext() != null) {
            SharedPreferences sharedPref = StorageHelper.getPreferences(getApplicationContext());
            String logoUrl = sharedPref.getString(KEY_BRAND_LOGO, null);
            Utils.getInstance().preloadBrandLogo(getApplicationContext(), logoUrl);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        try {
            unregisterReceiver(mConnectivityReceiver);
        } catch (Exception e) {
            getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while unregistering the network receiver: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return true;
    }

    /**
     * fetch the jwt token and and make connection to signalling channel.
     */
    public void startSigSock() {
        try {
            Context context = getApplicationContext();
            SharedPreferences sharedPref = StorageHelper.getPreferences(context);
            phone = sharedPref.getString(KEY_CONTACT_PHONE, "");
            cc = sharedPref.getString(KEY_CONTACT_CC, "");
            accountId = sharedPref.getString(KEY_ACCOUNT_ID, null);
            apikey = sharedPref.getString(KEY_API_KEY, null);
            cuid = sharedPref.getString(KEY_CONTACT_CUID, "");
            jwt = sharedPref.getString(KEY_JWT_TOKEN, "");
            String sna = sharedPref.getString(KEY_SNA, "");

            try {
                if (jwt != null && jwt.isEmpty()) {
                    Utils.getInstance().resetSession(context);
                    customHandler.sendInitAnnotations(DataStore.getInstance().getDirectCallInitResponse(), CustomHandler.Init.ON_FAILURE, InitException.SdkNotInitializedAppRestartRequiredException);
                    return;
                }
            } catch (Exception e) {
                Utils.getInstance().resetSession(this.getApplicationContext());
                return;
            }

            IO.Options opts = new IO.Options();
            opts.transports = new String[]{getString(R.string.websocket)};
            opts.forceNew = true;
            opts.reconnection = true;
            opts.reconnectionAttempts = 5;
            opts.reconnectionDelay = 1500;
            opts.reconnectionDelayMax = 6000;
            opts.timeout = 300000;
            opts.query = getString(R.string.jwt) + jwt;

            String url = "https://" + sna;

            try {
                socket = SocketIOManager.getSocket(opts, url);
                SocketIOManager.setSocket(socket);
                setUpEventHandlers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception ignored) {
        }
    }


    // <----- Callback functions ------->
    Emitter.Listener onDisconnect = args -> {
        try {
            if (args != null && args[0] instanceof String) {
                String disconnectReason = (String) args[0];
                disconnectHandler(disconnectReason);
            }
        } catch (Exception ignored) {
        }
    };

    Emitter.Listener onConnect = args -> {
        try {
            final JSONObject authArray = new JSONObject();
            authArray.put(getString(R.string.platform), getString(R.string.android));
            authArray.put(KEY_ACCOUNT_ID, accountId);
            authArray.put(KEY_API_KEY, apikey);
            authArray.put(KEY_CONTACT_CC, cc);
            authArray.put(KEY_CONTACT_PHONE, phone);
            authArray.put(KEY_CONTACT_CUID, cuid);

            isDisconnectDueToTransportError = false;
            socket.io().reconnection(true);
            socket.emit(getString(R.string.s_authentication), authArray);
        } catch (Exception ignored) {
        }
    };

    Emitter.Listener onAuthenticate = args -> {
        try {
            getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Connected to the signalling channel!");

            JSONObject status = new JSONObject();
            status.put("status", true);
            Object ackObj = args[args.length - 1];
            if (ackObj instanceof Ack) {
                //Important fix: to prevent the contact remain marked online in db due to Sockenticate module
                Ack ack = (Ack) ackObj;
                ack.call(status);
            }

            DirectCallAPI.getInstance().setEnabled(true);
            DirectCallAPI.sdkReady = true;

            isDisconnectDueToTransportError = false;
            isUnAuthorized = false;
            SocketIOManager.setIsUnAuthorized(false);
            dataStore = DataStore.getInstance();
            dataStore.setSocket(socket);

            if (FcmCacheManger.getInstance().isAnswered()) {
                if (DirectIncomingCallFragment.getInstance() != null) {
                    DirectIncomingCallFragment.getInstance().callAccepted();
                }
            } else if (FcmCacheManger.getInstance().isRejected()) {
                Utils.getInstance().fcmCallDecline(getApplicationContext());
            } /*else if (FcmCacheManger.getInstance().getShouldMarkNotificationStatusDelivered()) {
                Utils.getInstance().markNotificationDelivered(getApplicationContext());
            }*/
            //resetting cache each time socket connection is authenticated
            FcmCacheManger.getInstance().resetFcmCache();

            if (DataStore.getInstance() != null && DataStore.getInstance().getDirectCallInitResponse() != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        if (SocketIOManager.getIsUnAuthorized()) {
                            customHandler.sendInitAnnotations(DataStore.getInstance().getDirectCallInitResponse(), CustomHandler.Init.ON_FAILURE, InitException.SdkNotInitializedDueToCuidConnectedElsewhereException);
                        } else {
                            getLogger().debug(
                                    CALLING_LOG_TAG_SUFFIX,
                                    "Direct Call SDK Initialized!"
                            );
                            customHandler.sendInitAnnotations(DataStore.getInstance().getDirectCallInitResponse(), CustomHandler.Init.ON_SUCCESS, null);
                        }
                        DataStore.getInstance().setDirectCallInitResponse(null);
                    } catch (Exception ignored) {

                    }
                }, 1200);
            } else {
                Utils.getInstance().delayedHandler(1000, () -> {
                    try {
                        if (!SocketIOManager.getIsUnAuthorized()) {
                            Intent intent = new Intent(ACTION_CONNECTION_STATUS);
                            intent.putExtra("status", SUCCESS_SDK_CONNECTED);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        }
                    } catch (Exception e) {
                        //no-op
                    }
                });
            }
        } catch (Exception e) {
            getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in post authentication handling on signalling channel: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    };

    Emitter.Listener onUnAuthorize = args -> {
        try {
            isUnAuthorized = true;
            SocketIOManager.setIsUnAuthorized(true);

            if (reconnectingCount == 0) {
                int MAX_UN_AUTHORIZED_RETRY_DELAY = 5000;
                reconnectScheduler.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            if (reconnectingCount < MAX_RETRIES_AFTER_UN_AUTHORIZED) {

                                if (socket != null && !socket.connected()) {
                                    socket.connect();
                                } else {
                                    //Socket gets connected here, resetting the reconnectingCount
                                    reconnectingCount = 0;
                                    reconnectScheduler.cancel();
                                    return;
                                }
                            } else {
                                reconnectingCount = 0;
                                Utils.getInstance().resetSession(getApplicationContext());
                                reconnectScheduler.cancel();
                                return;
                            }
                            reconnectingCount += 1;
                        } catch (Exception ignored) {

                        }
                    }
                }, MAX_UN_AUTHORIZED_RETRY_DELAY, MAX_UN_AUTHORIZED_RETRY_DELAY);
            }
        } catch (Exception e) {
            //no-op
        }
    };

    Emitter.Listener onCancel = args -> {
        try {
            Ack ack = (Ack) args[args.length - 1];
            String cancelCallId = (String) args[0];
            String currentCallId = dataStore.getCallId();
            if (currentCallId == null) {
                ack.call("noactivecall");
            } else if (!(currentCallId.equals(cancelCallId))) {
                ack.call("othercall");
            } else {
                JSONObject status = new JSONObject();
                status.put("status", true);
                ack.call(status);

                if (DirectIncomingCallFragment.getInstance() != null) {
                    ICallStatusHandler.incomingCallStatus incomingCallStatus = dataStore.getIncomingCallStatus();
                    incomingCallStatus.onCancel();
                } else {
                    Utils.getInstance().callCancelled(getApplicationContext());
                }
            }
        } catch (Exception e) {
            getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in cancel call event handling: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    };

    Emitter.Listener onDecline = args -> {
        try {
            SocketIOManager.sendSuccessAck(args);

            JSONObject data = (JSONObject) args[0];
            if(data.has(KEY_DECLINE_REASON_CODE) && data.get(KEY_DECLINE_REASON_CODE) instanceof Integer) {
                int reasonCode = data.getInt(KEY_DECLINE_REASON_CODE);
                switch (reasonCode) {
                    case REASON_CODE_MICROPHONE_PERMISSION_NOT_GRANTED:
                    case REASON_CODE_INVALID_CUID:
                        onCallDeclined();
                        break;
                    case REASON_CODE_USER_BUSY:
                        DataStore.getInstance().setCalleBusyOnAnotherCall(true);
                        Utils.getInstance().delayedHandler(2500, this::onCallDeclined);
                        break;
                    default:
                        onCallDeclined();
                }
            }
        } catch (Exception e) {
            getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in decline call event handling: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    };

    Emitter.Listener onMiss = args -> {
        SocketIOManager.sendSuccessAck(args);

        onCallMissed();

        Utils.getInstance().delayedHandler(1500,
                () -> {
                    try {
                        List<MissedCallActions> initiatorMissedCallActionsList = Utils.getMissedCallActionsList(context, KEY_MISSED_CALL_INITIATOR_ACTIONS);
                        if (initiatorMissedCallActionsList.size() > 0) {
                            if (DataStore.getInstance().getCallContext() != null)
                                NotificationHandler.CallNotificationHandler.getInstance(getApplicationContext().getApplicationContext()).showCallNotification(
                                        new JSONObject().
                                                put("context", DataStore.getInstance().getCallContext()).
                                                put("toCuid", DataStore.getInstance().getToCuid()).
                                                put("fromCuid", DataStore.getInstance().getFromCuid()),
                                        NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);

                        }
                    } catch (Exception e) {
                        getLogger().
                                debug(CALLING_LOG_TAG_SUFFIX,
                                        "Exception while generating missed call notification at initiator's end: "+ e.getLocalizedMessage());
                    }
                });
    };

    Emitter.Listener onIncomingCall = args -> {
        try {
            //FcmCacheManger.getInstance().resetFcmCache();
            FcmUtil.getInstance(getApplicationContext()).resetExpiredCallId(null); //this is important to reset the expiredCallId on every new call
            SharedPreferences sharedPref = StorageHelper.getPreferences(getApplicationContext());
            DataStore.getInstance().setAppContext(getApplicationContext());
            SocketIOManager.sendSuccessAck(args);
            JSONObject incomingData = (JSONObject) args[0];
            String callId = incomingData.getString("call");
            String accountId = sharedPref.getString(KEY_ACCOUNT_ID, "");

            //Raising the system event for incoming call
            DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callContext).build();
            DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_INCOMING, dcSystemEventInfo);

            //Do further processing
            String fromId = incomingData.getJSONObject("from").getString(getString(R.string.id));
            String sid = incomingData.has("sid") ? incomingData.getString("sid") : "";
            incomingData.put(KEY_ACCOUNT_ID, accountId);
            incomingData.put(KEY_API_KEY, apikey);
            incomingData.put(KEY_CONTACT_PHONE, phone);
            incomingData.put(KEY_CONTACT_CC, cc);
            incomingData.put(KEY_JWT_TOKEN, jwt);
            incomingData.put(KEY_CONTACT_CUID, cuid);
            incomingData.put(getApplication().getString(R.string.callType), "incoming");
            if (cuid.length() > 0) {
                incomingData.put("initiatorData", cuid);
            } else {
                incomingData.put("initiatorData", cc + phone);
            }

            if (getApplicationContext() != null) {
                Utils.getInstance().removeNeverAskAgain(getApplicationContext());
                Boolean isNeverAskAgain = StorageHelper.getBoolean(getApplicationContext(), KEY_NEVER_ASK_AGAIN_PERMISSION, false);
                if (isNeverAskAgain) {
                    JSONObject data = new JSONObject();
                    data.put("responseSid", fromId + "_" + accountId);
                    data.put("callId", callId);
                    data.put(getString(R.string.sid), sid);
                    data.put(KEY_DECLINE_REASON, getString(R.string.microphone_permission_not_granted));
                    data.put(KEY_DECLINE_REASON_CODE, REASON_CODE_MICROPHONE_PERMISSION_NOT_GRANTED);

                    if (SocketIOManager.getSocket() != null) {
                        SocketIOManager.getSocket().emit(getApplication().getString(R.string.s_decline), data, new Ack() {
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
                    }
                    return;
                }
            }

            //this user is busy on other voip or pstn call so decline with reasonCode-1001
            if (DataStore.getInstance().isClientbusyOnVoIP() || DataStore.getInstance().isClientbusyOnPstn()) {
                incomingData.put("responseSid", fromId + "_" + accountId);
                incomingData.put("callId", callId);
                incomingData.put(getString(R.string.sid), sid);
                incomingData.put(KEY_DECLINE_REASON, getString(R.string.client_busy));
                incomingData.put(KEY_DECLINE_REASON_CODE, REASON_CODE_USER_BUSY);
                if (SocketIOManager.getSocket() != null) {
                    SocketIOManager.getSocket().emit(getString(R.string.s_decline), incomingData, new Ack() {
                        @Override
                        public void call(Object... args) {
                            DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callContext)
                                    .setCallStatus(CallStatus.CALL_DECLINED)
                                    .setDeclineReason(CallStatus.DeclineReason.USER_BUSY)
                                    .build();
                            DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_END, dcSystemEventInfo);
                        }
                    });
                }
            } else {
                DataStore.getInstance().setClientbusyOnVoIP(true);
                DataStore.getInstance().setCallDetails(incomingData);
                DataStore.getInstance().setSid(sid);

                Utils.getInstance().lightUpScreen(getApplicationContext());
                JSONObject incomingNotificationData = new JSONObject();
                incomingNotificationData.put(getApplicationContext().getString(R.string.callDetails), incomingData.toString());
                incomingNotificationData.put(getApplicationContext().getString(R.string.sid), sid);
                incomingNotificationData.put("context", incomingData.getString("context"));

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    NotificationHandler.CallNotificationHandler.getInstance(getApplicationContext()).
                            showCallNotification(incomingNotificationData,
                                    NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                    Utils.getInstance().startAudio(getApplicationContext());
                    Utils.getInstance().startTimer(getApplicationContext(), incomingData);
                } else {
                    Intent incomingCallNotificationIntent;
                    if (context != null) {
                        incomingCallNotificationIntent = new Intent(context, CallNotificationService.class);
                    } else {
                        incomingCallNotificationIntent = new Intent(getApplicationContext(), CallNotificationService.class);
                    }
                    incomingCallNotificationIntent.putExtra("incomingNotificationData", incomingNotificationData.toString());
                    context.startForegroundService(incomingCallNotificationIntent);
                    Utils.getInstance().startTimer(getApplicationContext(), incomingData);
                }

                try {
                    Utils.getInstance().sendBroadcast(getApplicationContext(), Constants.ACTION_INCOMING_CALL);
                } catch (Exception e) {
                    //no-op
                }
                dataStore.setCallId(callId);
                dataStore.setRecording(true);
                Intent i = new Intent(getApplicationContext(), DirectCallingActivity.class);
                i.putExtra(getApplicationContext().getString(R.string.callDetails), incomingData.toString());
                i.putExtra(getApplicationContext().getString(R.string.screen), getApplicationContext().getString(R.string.incoming));
                i.putExtra(getApplicationContext().getString(R.string.sid), sid);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                if (Build.VERSION.SDK_INT >= 29 && Utils.getInstance().isAppInBackground()) {
                    return;
                }
                startActivity(i);
            }
        } catch (Exception e) {
            getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in incoming call event handling: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    };

    Emitter.Listener onAnswer = args -> {
        SocketIOManager.sendSuccessAck(args);
        onCallAnswered();
    };

    Emitter.Listener onHoldUnHold = args -> {
        try {
            JSONObject jsonObject = (JSONObject) args[0];
            if (jsonObject.has("hold")) {
                DirectOngoingCallFragment.getInstance().putOnHold(jsonObject.getBoolean("hold"));
            }
        } catch (Exception e) {
            getLogger()
                    .debug(CALLING_LOG_TAG_SUFFIX,
                            "error in hold-unHold event handling: " + e.getLocalizedMessage());
        }
    };

    /**
     * registers all sigSock events
     */
    private void setUpEventHandlers() {
        if (SocketIOManager.getIsNewInstance()) {
            socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            socket.on(Socket.EVENT_CONNECT, onConnect);
            socket.on(getString(R.string.s_authenticated), onAuthenticate);
            socket.on(getString(R.string.sUnauthorized), onUnAuthorize);
            socket.on(getString(R.string.s_cancel), onCancel);
            socket.on(getString(R.string.s_decline), onDecline);
            socket.on(getString(R.string.s_miss), onMiss);
            socket.on(getString(R.string.s_incoming_call), onIncomingCall);
            socket.on(getString(R.string.s_answer), onAnswer);
            socket.on(getString(R.string.sHoldUnhold), onHoldUnHold);
        }
        Utils.getInstance().delayedHandler(1000,
                () -> {
                    if (!socket.connected()) {
                        socket.connect();
                    }
                });
    }

    private void onCallDeclined() {
        if(dataStore == null) {
            dataStore = DataStore.getInstance();
        }
        ICallStatusHandler = dataStore.getCallStatus();
        if (ICallStatusHandler != null) {
            ICallStatusHandler.onDecline();
        }
        if (dataStore.getTimer() != null) {
            dataStore.getTimer().cancel();
        }
    }

    private void onCallMissed() {
        if(dataStore == null) {
            dataStore = DataStore.getInstance();
        }
        ICallStatusHandler = dataStore.getCallStatus();
        if (ICallStatusHandler != null) {
            ICallStatusHandler.onMiss();
        }
        if (dataStore.getTimer() != null) {
            dataStore.getTimer().cancel();
        }
    }

    private void onCallAnswered() {
        if(dataStore == null) {
            dataStore = DataStore.getInstance();
        }
        ICallStatusHandler = dataStore.getCallStatus();
        if (ICallStatusHandler != null) {
            ICallStatusHandler.onAnswer();
        }
        if (dataStore.getTimer() != null) {
            dataStore.getTimer().cancel();
        }
    }

    /**
     * called when the user's network connectivity is changed.
     *
     * @param isConnected:- true if connected else false.
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (isConnected) {
            try {
                if (isDisconnectDueToTransportError && transportDisconnectTimer < 10) {
                    try {
                        if(transportErrorCountDownTimer != null)
                            transportErrorCountDownTimer.cancel();

                        new CountDownTimer(SIG_SOCK_PING_TIMEOUT - (transportDisconnectTimer * 1000L), 1000) {
                            public void onTick(long millisUntilFinished) {
                            }

                            public void onFinish() {
                                startSigSock();
                            }
                        }.start();
                    } catch (Exception ignored) {

                    }
                } else {
                    startSigSock();
                }
            } catch (Exception e) {
                getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while listening the state changes in network: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        } else {
            getLogger().debug(CALLING_LOG_TAG_SUFFIX, "SDK got disconnected from the signalling channel");
        }
    }

    static String calleeCC, calleePhone, calleeCuid, callContext;
    static JSONObject callOptions;
    static OutgoingCallResponse outgoingCallResponse;

    /**
     * emit make call on the sigSock to make an outgoing call.
     *
     * @param calleeCC:-             cc of the user whom user is trying to call.
     * @param calleePhone:-          phone number of the user whom user is trying to make a call.
     * @param calleeCuid:-           cuid of the user to whom user is trying to make a call.
     * @param callContext:-          context of the call.
     * @param callOptions:-          jsonObject containing call options passed at the time of making a call.
     * @param outgoingCallResponse:- callback statuses to be returned to the user on making a call.
     */
    public void makeCall(final String calleeCC, final String calleePhone, final String calleeCuid, final String callContext, final JSONObject callOptions, final OutgoingCallResponse outgoingCallResponse) {
        String[] PERMISSIONS = {
                android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                android.Manifest.permission.RECORD_AUDIO
        };
        try {
            JobSchedulerSocketService.calleeCC = calleeCC;
            JobSchedulerSocketService.calleePhone = calleePhone;
            JobSchedulerSocketService.calleeCuid = calleeCuid;
            JobSchedulerSocketService.callContext = callContext;
            JobSchedulerSocketService.callOptions = callOptions;
            JobSchedulerSocketService.outgoingCallResponse = outgoingCallResponse;

            if (Utils.getInstance().hasPermissions(context, PERMISSIONS)) {

                if (SocketIOManager.getIsUnAuthorized()) {
                    DirectCallAPI.sdkReady = true;
                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.CuidConnectedElsewhereException, null);
                    return;
                }
                final DataStore dataStore = DataStore.getInstance();

                JSONArray tagsarray = null;
                if (callOptions.has("tags") && callOptions.get("tags") instanceof JSONArray) {
                    tagsarray = callOptions.getJSONArray("tags");
                }
                Boolean autoFallBack = false;
                if (callOptions.has("autoFallback")) {
                    if (callOptions.getBoolean("autoFallback")) {
                        autoFallBack = true;
                    }
                }
                final Boolean isPstn = callOptions.getBoolean("pstn");
                dataStore.setRecording(callOptions.getBoolean("recording"));

                final JSONObject jsonObject = new JSONObject();
                jsonObject.put(KEY_CONTACT_CC, calleeCC);
                jsonObject.put(KEY_CONTACT_PHONE, calleePhone);
                jsonObject.put(KEY_CALL_CONTEXT, callContext);
                jsonObject.put(KEY_CONTACT_CUID, calleeCuid);
                jsonObject.put(KEY_PSTN, callOptions.getBoolean("pstn"));
                jsonObject.put(KEY_RECORDING, callOptions.getBoolean("recording"));
                jsonObject.put(KEY_WEBHOOK, callOptions.getString("webhook"));
                jsonObject.put(KEY_VAR1, callOptions.getString("var1"));
                jsonObject.put(KEY_VAR2, callOptions.getString("var2"));
                jsonObject.put(KEY_VAR3, callOptions.getString("var3"));
                jsonObject.put(KEY_VAR4, callOptions.getString("var4"));
                jsonObject.put(KEY_VAR5, callOptions.getString("var5"));
                jsonObject.put(KEY_TAGS, tagsarray);
                jsonObject.put("apf", autoFallBack);

                if (callOptions.has("callToken")) { callOptions.getString("callToken");
                    if (!callOptions.getString("callToken").equals("")) {
                        jsonObject.put("callToken", callOptions.getString("callToken"));
                    }
                }

                Boolean finalAutoFallBack = autoFallBack;
                dataStore.getSocket().emit(context.getString(R.string.s_make_call), jsonObject, new AckWithTimeOut(MAKE_CALL_TIMEOUT) {
                    @Override
                    public void call(Object... args) {
                        try {
                            if (args != null) {
                                if (args[0].toString().equalsIgnoreCase(SOCKET_NO_ACK)) {
                                    DirectCallAPI.sdkReady = true;
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InternetLostAtReceiverEndException, null);
                                } else {
                                    cancelTimer(); //Important: cancel AckTimer here
                                    try {
                                        JSONObject data = (JSONObject) args[0];
                                        if(data == null) {
                                            return;
                                        }
                                        if (data.getBoolean(KEY_STATUS)) {
                                            final JSONObject callData = data.has(KEY_CALL_DATA) ? data.getJSONObject(KEY_CALL_DATA): null;
                                            if(callData == null) {
                                                getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Call related details are missing!");
                                                return;
                                            }
                                            final String callId = callData.has(KEY_CALL_ID) ? callData.getString(KEY_CALL_ID) : null;
                                            String host = callData.has(KEY_CALL_HOST) ? callData.getString(KEY_CALL_HOST) : null;
                                            String callingContext = callData.has(KEY_CALL_CONTEXT) ? callData.getString(KEY_CALL_CONTEXT) : null;
                                            JSONObject toData = callData.has(KEY_TO_Data) ? callData.getJSONObject(KEY_TO_Data) : null;
                                            JSONObject fromData = callData.has(KEY_FROM_DATA) ? callData.getJSONObject(KEY_FROM_DATA) : null;

                                            if (callId != null && !callId.isEmpty()) {
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_SUCCESS, null, null);

                                                //Raising the system event for incoming call
                                                DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callingContext).build();
                                                DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_OUTGOING, dcSystemEventInfo);

                                                //------------Setting VoIP channel is busy to block the further call requests ---------
                                                dataStore.setClientbusyOnVoIP(true);
                                                dataStore.setCallId(callId);

                                                //Further processing
                                                SharedPreferences sharedPref = StorageHelper.getPreferences(context);
                                                accountId = sharedPref.getString(KEY_ACCOUNT_ID, null);
                                                apikey = sharedPref.getString(KEY_API_KEY, null);
                                                cuid = sharedPref.getString(KEY_CONTACT_CUID, "");
                                                phone = sharedPref.getString(KEY_CONTACT_PHONE, "");
                                                cc = sharedPref.getString(KEY_CONTACT_CC, "");
                                                jwt = sharedPref.getString(KEY_JWT_TOKEN, null);

                                                data.put(KEY_ACCOUNT_ID, accountId);
                                                data.put(KEY_API_KEY, apikey);
                                                data.put(KEY_CONTACT_PHONE, phone);
                                                data.put(KEY_CONTACT_CC, cc);
                                                data.put(KEY_JWT_TOKEN, jwt);
                                                data.put(KEY_CONTACT_CUID, cuid);
                                                data.put(KEY_CALL_CONTEXT, callingContext);
                                                data.put(KEY_CALL_ID, callId);
                                                data.put(KEY_CALL_HOST, host);
                                                data.put(KEY_CALL_TYPE, "outgoing");
                                                data.put(KEY_TO_Data, callData);

                                                if (cuid.length() > 0) {
                                                    data.put(KEY_INITIATOR_DATA, cuid);
                                                } else {
                                                    data.put(KEY_INITIATOR_DATA, cc + phone);
                                                }

                                                boolean canIosApf = checkIosApfEligibility(toData, isPstn);
                                                if(canIosApf) {
                                                    DataStore.getInstance().setIosAPF(new PushAPF(calleeCC, calleePhone, calleeCuid, callContext, callOptions, outgoingCallResponse));
                                                }else {
                                                    DataStore.getInstance().setIosAPF(new PushAPF("",
                                                            "",
                                                            "",
                                                            "",
                                                            new JSONObject(), null));
                                                }


                                                if (isPstn) {
                                                    if (toData.getString("cc").equals("false") ||
                                                            toData.getString("phone").equals("false")) {
                                                        DirectCallAPI.sdkReady = true;
                                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.NumberNotExistsException, null);
                                                        return;
                                                    }

                                                    if (callOptions.has("ios_apf") && callOptions.getBoolean("ios_apf")) {
                                                        startIosApfOutgoingCall(data);
                                                    } else {
                                                        //following condition/handling is True for Never_Ask_Again flow
                                                        final Intent callScreenIntent = new Intent(context, DirectCallingActivity.class);
                                                        callScreenIntent.putExtra(context.getString(R.string.screen), context.getString(R.string.call_screen));
                                                        callScreenIntent.putExtra(context.getString(R.string.callDetails), data.toString());
                                                        context.startActivity(callScreenIntent);
                                                        //                                                        if (unAuthorizedFlag != null && unAuthorizedFlag.equals("401")) {
//                                                            dataStore.getTimer().cancel();  //Important
//
//                                                            Intent broadcastIntent = new Intent(Constants.CALL_FINISH_ACTION);
//                                                            broadcastIntent.putExtra(BROADCAST_ACTION, ACTION_ACTIVITY_FINISH);
//                                                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
//                                                            countDownHandler.post(new Runnable() {
//                                                                @Override
//                                                                public void run() {
//                                                                    new CountDownTimer(750, 750) {
//
//                                                                        @Override
//                                                                        public void onTick(long millisUntilFinished) {
//                                                                        }
//
//                                                                        @Override
//                                                                        public void onFinish() {
//                                                                            callScreenIntent.putExtra(Constants.ACTIVITY_RELAUNCH_REASON, getString(R.string.microphone_permission_not_granted));
//                                                                            context.startActivity(callScreenIntent);
//                                                                        }
//                                                                    }.start();
//                                                                }
//                                                            });
//                                                        } else {
//                                                            context.startActivity(callScreenIntent);
//                                                        }
                                                    }
                                                } else {
                                                    Intent i = new Intent(context, DirectCallingActivity.class);
                                                    i.putExtra(context.getString(R.string.screen), context.getString(R.string.outgoing));
                                                    i.putExtra(context.getString(R.string.callDetails), data.toString());
                                                    i.addFlags(FLAG_ACTIVITY_NEW_TASK);
                                                    context.startActivity(i);

                                                    startOutgoingCallTimer(callId);
                                                }
                                            }
                                        } else {
                                            JSONObject errorObject = data.has("error") ? data.getJSONObject("error") : null;
                                            if(errorObject == null) {
                                                return;
                                            }
                                            String errorMessage = errorObject.has("message") ? errorObject.getString("message") : null;
                                            Integer errorCode = errorObject.has("code") ? errorObject.getInt("code") : null;

                                            if (errorCode != null
                                                    && errorCode == MAKE_CALL_ERR_CODE_RECEIVER_NOT_REACHABLE && finalAutoFallBack) {
                                                if (calleeCC != null && calleeCC.length() > 0 && calleePhone != null && calleePhone.length() > 0) {
                                                    callOptions.put("pstn", true);
                                                    callOptions.remove("autoFallback");
                                                    makeCall(calleeCC, calleePhone, "", callContext, callOptions, outgoingCallResponse);
                                                } else {
                                                    callOptions.put("pstn", true);
                                                    callOptions.remove("autoFallback");
                                                    makeCall("", "", calleeCuid, callContext, callOptions, outgoingCallResponse);
                                                }
                                            } else if (errorMessage != null && errorMessage.equals(MAKE_CALL_ERR_MSG_INVALID_CALL_TOKEN)) {
                                                DirectCallAPI.sdkReady = true;
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.InvalidCallTokenException, null);
                                            } else if (errorCode != null && errorCode == MAKE_CALL_ERR_CODE_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL &&
                                                    errorMessage != null && errorMessage.equals(MAKE_CALL_ERR_MSG_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL)) {
                                                DirectCallAPI.sdkReady = true;
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.MissingCcPhoneToMakePstnCallException, null);
                                                if (DirectCallingActivity.getInstance() != null) {
                                                    DirectCallingActivity.getInstance().finish();
                                                }
                                            } else if (errorCode != null && errorCode == MAKE_CALL_ERR_CODE_RECEIVER_NOT_REACHABLE) {
                                                DirectCallAPI.sdkReady = true;
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.ContactNotReachableException, null);
                                            } else if (errorMessage != null && errorMessage.equals(MAKE_CALL_ERR_MSG_MALFORMED_JWT)) {
                                                DirectCallAPI.sdkReady = true;
                                                getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Unregistered CallToken!");
                                            } else {
                                                DirectCallAPI.sdkReady = true;
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.VoIPCallException, null);
                                            }
                                        }
                                    } catch (Exception e) {
                                        DirectCallAPI.sdkReady = true;
                                        getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while making the call: " + Log.getStackTraceString(e));
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.VoIPCallException, null);
                                    }
                                }
                            }

                        } catch (Exception e) {
                            DirectCallAPI.sdkReady = true;
                        }
                    }
                });
            } else {
                DirectCallAPI.sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, CallException.MicrophonePermissionNotGrantedException, null);
            }
        } catch (Exception e) {
            DirectCallAPI.sdkReady = true;
            getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while parsing the make-call response: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void startOutgoingCallTimer(String callId) {
        new Handler(Looper.getMainLooper()).post(() -> {
            dataStore = DataStore.getInstance();
            outgoingTimer = new CountDownTimer(OUTGOING_CALL_TIME_DURATION, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    final PushAPF pushAPF = dataStore.getIosAPF();
                    if (pushAPF != null && pushAPF.getContext() == null) {
                        dataStore.getOutgoingActivity().finish();
                        DirectCallAPI.sdkReady = true;

                        if (outgoingTimer != null) {
                            outgoingTimer.cancel();
                        }
                        dataStore.getSocket().emit(getString(R.string.s_cancel), callId);

                        DataStore.getInstance().setTimer(outgoingTimer);
                    } else {
                        emitIosAPF(callId, new PushApfInterface() {
                            @Override
                            public void onSuccess() {
                                try {
                                    if(pushAPF != null && pushAPF.getCallOptions() != null) {
                                        pushAPF.getCallOptions().put("pstn", true);
                                        pushAPF.getCallOptions().put("ios_apf", true);
                                        makeCall(pushAPF.getCalleeCC(),
                                                pushAPF.getCalleePhone(),
                                                pushAPF.getCuid(),
                                                pushAPF.getContext(),
                                                pushAPF.getCallOptions(),
                                                outgoingCallResponse);
                                        dataStore.setIosAPF(new PushAPF("",
                                                "",
                                                "", "", new JSONObject(), null));

                                        if (outgoingTimer != null) {
                                            outgoingTimer.cancel();
                                        }
                                    }
                                } catch (Exception e) {
                                    getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while handling the iOSAPF: " + e.getLocalizedMessage());
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure() {

                            }
                        });

                    }
                }
            };
            outgoingTimer.start();
            dataStore.setTimer(outgoingTimer);
        });
    }

    private void startIosApfOutgoingCall(JSONObject data) {
        ICallStatusHandler = dataStore.getCallStatus();
        if(ICallStatusHandler != null && data != null) {
            ICallStatusHandler.onIosApf(data.toString());
        }
    }

    private boolean checkIosApfEligibility(JSONObject callData, boolean isPstn) throws Exception {
        boolean canIosApf = false;
        JSONObject toData = callData.has(KEY_TO_Data) ? callData.getJSONObject(KEY_TO_Data) : null;
        if ((toData != null && toData.has(KEY_PLATFORM) && "ios".equals(toData.getString(KEY_PLATFORM)))
                || (callData.has("fcmEnabled") && callData.getBoolean("fcmEnabled"))) {
            if (!isPstn && callOptions.has("autoFallback") && callOptions.getBoolean("autoFallback")) {
                canIosApf = true;
            }
        }
        return canIosApf;
    }

    /**
     * To mark the call-status to autoFallback from connecting before making a PSTN call to the iOS receiver.
     *
     * @param callId:-          id of the call got in the response of makeCall.
     * @param pushApfInterface:- to execute the iosApf() method in the outgoingFragment.
     */
    public void emitIosAPF(String callId, final PushApfInterface pushApfInterface) {
        if (DataStore.getInstance().getSocket() != null) {
            DataStore.getInstance().getSocket().emit(getString(R.string.s_ios_apf), callId, (Ack) args -> {
                try {
                    JSONObject iosApfStatus = (JSONObject) args[0];
                    if (iosApfStatus.getBoolean("status")) {
                        pushApfInterface.onSuccess();
                    } else {
                        pushApfInterface.onFailure();
                    }
                } catch (Exception e) {
                    getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in ios-apf handling: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    private void disconnectHandler(String disconnectReason) {
        switch (disconnectReason) {
            case TRANSPORT_ERROR:
                isDisconnectDueToTransportError = true;
                socket.io().reconnection(false); //this is important to prevent from Unauthorized case
                transportDisconnectTimer = 0;
                countDownHandler.post(() -> {
                    try {
                        transportErrorCountDownTimer = new CountDownTimer(SIG_SOCK_PING_TIMEOUT, 1000) {

                            public void onTick(long millisUntilFinished) {
                                transportDisconnectTimer += 1;
                            }

                            public void onFinish() {
                                try {
                                    socket.io().reconnection(true);
                                    if (!socket.connected()) {
                                        socket.connect();
                                    }
                                } catch (Exception ignored) {

                                }
                            }
                        };
                        transportErrorCountDownTimer.start();
                    } catch (Exception e) {
                        //
                    }
                });
                break;
            case IO_SERVER_DISCONNECT:
                try {
                    if (!isUnAuthorized) {
                        //disconnecting socket, logging out, stopping service
                        Utils.getInstance().resetSession(context);
                        reconnectScheduler.cancel();
                    }
                } catch (Exception e) {
                    getLogger().debug(
                            CALLING_LOG_TAG_SUFFIX,
                            "IO Server disconnect while making connection to VoIP channel: " + e.getLocalizedMessage()
                    );
                }
                break;
        }
    }
}



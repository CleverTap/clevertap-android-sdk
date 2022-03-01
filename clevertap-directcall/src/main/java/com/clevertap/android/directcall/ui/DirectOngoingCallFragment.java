package com.clevertap.android.directcall.ui;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.KEY_ACCOUNT_ID;
import static com.clevertap.android.directcall.Constants.KEY_CALL_CONTEXT;
import static com.clevertap.android.directcall.Constants.KEY_CALL_ID;
import static com.clevertap.android.directcall.Constants.KEY_CALL_TYPE;
import static com.clevertap.android.directcall.Constants.KEY_CONTACT_CUID;
import static com.clevertap.android.directcall.Constants.NETWORK_LATENCY_CHECK_FREQUENCY;
import static com.clevertap.android.directcall.Constants.SIP_CLIENT_HANGUP;
import static com.clevertap.android.directcall.Constants.SIP_CLIENT_INIT_JSSIP;
import static com.clevertap.android.directcall.Constants.SIP_CLIENT_INIT_SDK;
import static com.clevertap.android.directcall.Constants.SIP_CLIENT_MUTE;
import static com.clevertap.android.directcall.Constants.SIP_CLIENT_UN_MUTE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.clevertap.android.directcall.AudioSwitchUtil;
import com.clevertap.android.directcall.Constants;
import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.directcall.enums.CallStatus;
import com.clevertap.android.directcall.events.CTSystemEvent;
import com.clevertap.android.directcall.events.DCSystemEventInfo;
import com.clevertap.android.directcall.exception.CallException;
import com.clevertap.android.directcall.fcm.FcmUtil;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.interfaces.CallNotificationAction;
import com.clevertap.android.directcall.interfaces.CallType;
import com.clevertap.android.directcall.interfaces.ICallStatusHandler;
import com.clevertap.android.directcall.javaclasses.DCJSInterface;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.javaclasses.VoIPCallStatus;
import com.clevertap.android.directcall.models.SelectedTemplate;
import com.clevertap.android.directcall.recievers.CallNotificationActionReceiver;
import com.clevertap.android.directcall.recievers.EarPieceIntentReceiver;
import com.clevertap.android.directcall.utils.CallScreenUtil;
import com.clevertap.android.directcall.utils.CustomHandler;
import com.clevertap.android.directcall.utils.NotificationHandler;
import com.clevertap.android.directcall.utils.SocketIOManager;
import com.clevertap.android.directcall.utils.Utils;
import com.clevertap.android.directcall.utils.VibrateUtils;
import com.clevertap.android.directcall.utils.WakeLockUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class DirectOngoingCallFragment extends Fragment implements ICallStatusHandler.incomingCallStatus, CallType, CallNotificationAction {
    private View view, rootViewBackground;
    private WebView webView;
    private ImageView ivMute, ivSpeaker, ivHangup, ivBluetooth, ivLogo;
    private TextView tvContext, tvCallScreenLabel, tvNetworkLatency, tvPoweredBy, tvHoldState;
    private AudioManager audioManager;
    private EarPieceIntentReceiver earPieceIntentReceiver;
    private SensorManager mSensorManager;
    private static final int SENSOR_SENSITIVITY = 4;
    private WebView jsView;
    private Socket socket;
    private JSONObject callData;
    private String callDetails;
    private String accountId;
    private String callId;
    private String callContext;
    private boolean isSpeakerOn = false, isMute = false, isBlueToothOn = false, isHold = false,
            isHoldDueToPSTN = false, isFragmentVisible = false, isPutOnHold = false;
    private Chronometer chTimer;
    private Timer pingTimer;
    private boolean isPstnCallAnswered = false;
    private NotificationHandler.CallNotificationHandler callNotificationHandler;
    private String callType, fromCuid, toCuid;
    private final CustomHandler customHandler = CustomHandler.getInstance();
    private static Vibrator vibrator;
    private String templateType;
    private ImageView ivBannerImage;
    private DCJSInterface jsInterface;
    private AudioSwitchUtil audioSwitchUtil;
    private SpeakerType currentSpeakerType = SpeakerType.BUILT_IN;
    private FragmentActivity activity;
    private Context appContext;

    enum BluetoothControllerVisibility {
        PARTIAL, FULL, HIDDEN
    }

    enum SpeakerType {
        BUILT_IN("built_in"), EXTERNAL("external");

        private final String name;

        SpeakerType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static DirectOngoingCallFragment instance;

    public static DirectOngoingCallFragment getInstance() {
        return instance;
    }

    public boolean getIsSpeakerOn() {
        return isSpeakerOn;
    }

    public boolean getIsFragmentVisible() {
        return isFragmentVisible;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_direct_call_ongoing_screen, container, false);
        instance = this;
        isFragmentVisible = true;

        if (getActivity() != null) {
            activity = getActivity();
            appContext = activity.getApplicationContext();

            initViews();
            initResources();
            prepareResources();
            updateUI();
            initWebView();

            try {
                if (getArguments() != null)
                    callDetails = getArguments().getString(getString(R.string.callDetails));
                DataStore dataStore = DataStore.getInstance();
                dataStore.setCallData(callDetails);
                callData = new JSONObject(callDetails);

                try {
                    if (callData.has(getString(R.string.active_template)) && callData.get(getString(R.string.active_template)) instanceof JSONObject) {
                        SelectedTemplate selectedTemplate = SelectedTemplate.fromJson(callData.getJSONObject(getString(R.string.active_template)));
                        if (selectedTemplate.getType() != null && selectedTemplate.getUrl() != null) {
                            templateType = selectedTemplate.getType();
                            String templateUrl = selectedTemplate.getUrl();

                            if (templateType.equals(getString(R.string.template_scratchcard))) {
                                ivBannerImage = view.findViewById(R.id.iv_banner);
                                Glide.with(this)
                                        .load(templateUrl)
                                        .into(ivBannerImage);
                            }
                        }
                    }
                    callContext = callData.has(KEY_CALL_CONTEXT) ? callData.getString(KEY_CALL_CONTEXT) : null;
                    accountId = callData.has(KEY_ACCOUNT_ID) ? callData.getString(KEY_ACCOUNT_ID) : null;
                    callId = callData.has(KEY_CALL_ID) ? callData.getString(KEY_CALL_ID) : null;
                    callType = callData.has(KEY_CALL_TYPE) ? callData.getString(KEY_CALL_TYPE) : null;

                    if (callType != null) {
                        switch (callType) {
                            case "incoming":
                                fromCuid = callData.getJSONObject("from").getString(KEY_CONTACT_CUID);
                                toCuid = callData.getJSONObject("to").getString(KEY_CONTACT_CUID);
                                break;

                            case "outgoing":
                                fromCuid = callData.getJSONObject("data").getJSONObject("from").getString(KEY_CONTACT_CUID);
                                toCuid = callData.getJSONObject("data").getJSONObject("to").getString(KEY_CONTACT_CUID);
                                break;
                        }
                    }

                    DataStore.getInstance().setFromCuid(fromCuid);
                    DataStore.getInstance().setToCuid(toCuid);

                    if (callData.has("data")) {
                        if (callData.getJSONObject("data").has("pstn") && callData.getJSONObject("data").getBoolean("pstn")) {
                            //when a PSTN call is dialling
                            tvCallScreenLabel.setText(getString(R.string.calling));
                        }
                    } else {
                        if (callData.has("pstn") && callData.getBoolean("pstn")) {
                            tvCallScreenLabel.setText(getString(R.string.calling));
                        }
                    }
                } catch (JSONException e) {
                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while parsing the calling details: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                setBranding();

                ivHangup.setOnClickListener(v -> {
                    DirectCallAPI.sdkReady = true;
                    hangupCall();
                });

                ivBluetooth.setOnClickListener(v -> {
                    if (isBlueToothOn) {
                        isBlueToothOn = false;
                        ivBluetooth.setImageResource(R.drawable.ct_bluetooth_outline);
                        audioSwitchUtil.setupAudioOutputDevice(false, false);
                    } else {
                        isBlueToothOn = true;
                        ivBluetooth.setImageResource(R.drawable.ct_bluetooth_filled);
                        if (isSpeakerOn) {
                            isSpeakerOn = false;
                            ivSpeaker.setImageResource(R.drawable.ct_speaker_outline);
                        }
                        audioSwitchUtil.setupAudioOutputDevice(true, false);
                    }
                });

                ivSpeaker.setOnClickListener(v -> {
                    switchSpeakerState();
                });

                ivMute.setOnClickListener(v -> switchMuteState());

            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Failed to render the layout for ongoing call screen: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        return view;
    }

    private void netWorkLatencyHandler(String latency) {
        int networkLatency = Integer.parseInt(latency);

        runOnMainThread(() -> {
            if (networkLatency > 300 && networkLatency < 400) {
                chTimer.startAnimation(Utils.getInstance().getAlphaAnimator());
                tvNetworkLatency.setVisibility(View.VISIBLE);
                tvNetworkLatency.setText(getString(R.string.bad_network));
            } else if (networkLatency >= 400) {
                chTimer.startAnimation(Utils.getInstance().getAlphaAnimator());
                tvNetworkLatency.setVisibility(View.VISIBLE);
                tvNetworkLatency.setText(getString(R.string.poor_network));
            } else {
                chTimer.clearAnimation();
                tvNetworkLatency.setVisibility(View.GONE);
                tvNetworkLatency.setText("");
            }
        });
    }

    private void initViews() {
        webView = view.findViewById(R.id.webView);
        tvHoldState = view.findViewById(R.id.tv_hold_state);
        tvContext = view.findViewById(R.id.tv_callContext);
        tvCallScreenLabel = view.findViewById(R.id.tv_callScreen_label);
        tvPoweredBy = view.findViewById(R.id.tv_poweredBy);
        ivHangup = view.findViewById(R.id.iv_hangup);
        ivMute = view.findViewById(R.id.iv_mute);
        ivSpeaker = view.findViewById(R.id.iv_speaker);
        ivLogo = view.findViewById(R.id.iv_brand_logo);
        ivBluetooth = view.findViewById(R.id.iv_bluetooth);
        rootViewBackground = view.findViewById(R.id.root_view_background);
        ivMute = view.findViewById(R.id.iv_mute);
        ivSpeaker = view.findViewById(R.id.iv_speaker);
        chTimer = view.findViewById(R.id.tv_timer);
        tvNetworkLatency = view.findViewById(R.id.tv_networkLatency);
    }

    private void initResources() {
        audioManager = DataStore.getInstance().getAudioManager();
        if (audioManager == null) {
            audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        }

        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

        callNotificationHandler = NotificationHandler.CallNotificationHandler.getInstance(activity);
        CallNotificationActionReceiver.setCallNotificationActionListener(this);

        audioSwitchUtil = AudioSwitchUtil.getInstance(audioManager);
        pingTimer = new Timer();
        vibrator = VibrateUtils.getVibrator(activity);
        earPieceIntentReceiver = new EarPieceIntentReceiver();
    }

    private void prepareResources() {
        if (audioManager.isSpeakerphoneOn()) {
            audioManager.setSpeakerphoneOn(false);
        }

        if (audioSwitchUtil.isBluetoothHeadsetConnected()) {
            setBluetoothControllerVisibility(BluetoothControllerVisibility.PARTIAL);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        activity.registerReceiver(bluetoothConnectionReceiver, filter);

        pingTimer.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        try {
                            if (activity != null) {
                                String networkLatency = Utils.getInstance().ping("www.google.com");
                                if (networkLatency != null && !networkLatency.isEmpty()) {
                                    netWorkLatencyHandler(networkLatency);
                                }
                            }
                        } catch (Exception e) {
                            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while checking the signal strength: " + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                    }
                }, 0, NETWORK_LATENCY_CHECK_FREQUENCY
        );
    }

    private void updateUI() {
        ivMute.setEnabled(false);
        ivSpeaker.setEnabled(false);

        tvCallScreenLabel.startAnimation(Utils.getInstance().getAlphaAnimator());

        Bundle extras = getArguments();
        if (extras != null && extras.getString(Constants.ACTIVITY_RELAUNCH_REASON) != null
                && extras.getString(Constants.ACTIVITY_RELAUNCH_REASON).equals(getString(R.string.microphone_permission_not_granted))) {
            tvCallScreenLabel.setText("Retrying");
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        webView.setBackgroundColor(0);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        WebView.setWebContentsDebuggingEnabled(true);

        //an interface to cross communication b/w native java and js(webView)
        jsInterface = new DCJSInterface(getContext(), audioManager);
        webView.addJavascriptInterface(jsInterface, "Android");
        webView.loadUrl(getString(R.string.jsurl));

        try {
            // Get cert from raw resource...
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = getResources().getAssets().open("root_ca.crt"); // stored at -> /app/src/main/res/raw
            final Certificate certificate = cf.generateCertificate(caInput);
            caInput.close();
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    jsView = view;
                    loadWebViewUrl(SIP_CLIENT_INIT_SDK);

                    initCallSock();

                    tvContext.setText(callContext);
                }

                @Override
                public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                    // Get cert from SslError
                    try {
                        SslCertificate sslCertificate = error.getCertificate();
                        Certificate cert = getX509Certificate(sslCertificate);
                        if (cert != null && certificate != null) {
                            try {
                                // Reference: https://developer.android.com/reference/java/security/cert/Certificate.html#verify(java.security.PublicKey)
                                cert.verify(certificate.getPublicKey()); // Verify here...
                                handler.proceed();
                            } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
                                if (handler != null) handler.cancel();
                            }
                        } else {
                            handler.cancel();
                        }
                    } catch (Exception e) {
                        DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "SSL certificate error: " + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                }

            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onPermissionRequest(final PermissionRequest request) {
                    request.grant(request.getResources());
                }
            });
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "SSL certificate error: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            try {
                if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                    if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
                        //near
                        if (activity != null) {
                            WakeLockUtils.holdWakeLock(activity, PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK);
                        }
                    } else {
                        //far
                        WakeLockUtils.releaseWakeLock();
                    }
                }
            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception handling the state changes in Proximity sensor: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    public void onResume() {
        super.onResume();
        try {
            String callId = FcmUtil.getInstance(getContext()).getExpiredCallId();
            if (callId != null && callId.equals(callData.getString("call"))) {
                closeCallScreen();
                if (activity != null) {
                    FcmUtil.getInstance(getContext()).updateCallStatus(activity, callId);
                }
            }

            IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
            if (activity!= null) {
                activity.registerReceiver(earPieceIntentReceiver, filter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (vibrator != null)
            vibrator.cancel();
    }

    @Override
    public void onStop() {
        super.onStop();
        //Each time, the app goes to background, it headsups the ongoing notification
        if (callNotificationHandler != null && callNotificationHandler.getCallNotificationStatus() != null) {
            if (callNotificationHandler.getCallNotificationStatus().equals(NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL)) {
                callNotificationHandler.rebuildNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
            } else if (callNotificationHandler.getCallNotificationStatus().equals(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL)) {
                callNotificationHandler.rebuildNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (vibrator != null)
                vibrator.cancel();

            isFragmentVisible = false;

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        chTimer.stop();
                        chTimer.setText("00:00");
                    } catch (Exception e) {
                        DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while stopping the call timer: " + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                }
            });
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                CallScreenUtil.getInstance().releaseMediaPlayer();
            pingTimer.cancel();
            DataStore.getInstance().setRecording(false);
            socket.disconnect();
            socket.close();
            mSensorManager.unregisterListener(mSensorListener);

            WakeLockUtils.releaseWakeLock();
            WakeLockUtils.deallocate();

            webView.removeAllViews();
            webView.destroyDrawingCache();
            webView.destroy();
            webView = null;
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while releasing the references: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (activity != null) {
                activity.unregisterReceiver(earPieceIntentReceiver);
                activity.unregisterReceiver(bluetoothConnectionReceiver);
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while unregistering the earPieceIntent receiver: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * sets the branding of the view according to the data set by the owner on the dashboard.
     */
    private void setBranding() {
        Map<String, View> brandingViewParams = new HashMap<>();
        brandingViewParams.put("ivLogo", ivLogo);
        brandingViewParams.put("rootViewBackground", rootViewBackground);
        brandingViewParams.put("tvContext", tvContext);
        brandingViewParams.put("tvPoweredBy", tvPoweredBy);
        brandingViewParams.put("chTimer", chTimer);
        brandingViewParams.put("tvCallScreenLabel", tvCallScreenLabel);
        brandingViewParams.put("tvNetworkLatency", tvNetworkLatency);
        brandingViewParams.put("tvHoldState", tvHoldState);

        CallScreenUtil.getInstance().setBranding(getContext(), brandingViewParams, CallScreenUtil.DCCallScreenType.ONGOING);
    }

    /**
     * initialize the callSock.
     */
    private void initCallSock() {
        DataStore.getInstance().setCallType(DirectOngoingCallFragment.this);
        IO.Options opts = new IO.Options();
        opts.transports = new String[]{getString(R.string.websocket)};
        opts.forceNew = true;
        opts.reconnection = true;
        opts.reconnectionAttempts = 10;
        opts.reconnectionDelay = 1500;
        opts.reconnectionDelayMax = 6000;
        opts.timeout = 30000;
        opts.forceNew = false;
        try {
            opts.query = getString(R.string.jwt) + callData.getString("token");
            String url = "https://" + callData.getString("host") + ":3001";
            socket = IO.socket(url, opts);
            setUpEventHandlers();
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while establishing the connection to remote peer: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers all the events/Handlers of callSock.
     */
    private void setUpEventHandlers() {
        try {
            socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            socket.on(Socket.EVENT_CONNECT, onConnect);
            socket.on(getString(R.string.s_authenticated), onAuthenticated);
            socket.on(getString(R.string.s_socket_timeout), onSocketTimeout);
            socket.on(getString(R.string.s_endpoint_ready), onEndpointReady);
            socket.on(getString(R.string.callStatus), onCallStatus);
            socket.on(getString(R.string.s_connection_timeout), onConnectionTimeout);
            socket.on(Socket.EVENT_RECONNECT, onReconnect);
            socket.on(Socket.EVENT_RECONNECT_ATTEMPT, onReconnectionAttempt);
            socket.on(Socket.EVENT_RECONNECTING, onReconnecting);
            socket.on(Socket.EVENT_RECONNECT_FAILED, onReconnectionFailed);
            socket.on(Socket.EVENT_RECONNECT_ERROR, onReconnectError);
            if (!socket.connected()) {
                socket.connect();
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while handling the real-time changes in the ongoing call: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    // <----- Callback functions ------->
    Emitter.Listener onDisconnect = objects -> {
        //no-handling required
    };

    Emitter.Listener onConnect = args -> {
        try {
            final JSONObject authData = new JSONObject();
            authData.put("platform", "android");
            authData.put("accountId", callData.getString("accountId"));
            authData.put("apikey", callData.getString("apikey"));
            authData.put("cc", callData.getString("cc"));
            authData.put("phone", callData.getString("phone"));
            authData.put("callId", callData.getString("call"));
            authData.put("cuid", callData.getString("cuid"));
            authData.put("cli", DataStore.getInstance().getCli());
            socket.emit(getString(R.string.s_authentication), authData);
        }catch (Exception ignored){
            //no-op
        }
    };

    Emitter.Listener onAuthenticated = args -> {
        //no-handling required
    };

    Emitter.Listener onSocketTimeout = args -> {
        //no-handling required
    };

    Emitter.Listener onEndpointReady = args -> {
        try {
            JSONObject payload = (JSONObject) args[0];
            if (payload.has("secret")) {
                jsInterface.setSecretToCallDetail(payload.getString("secret"));
            }
            if (callData.has("data")) {
                if (callData.getJSONObject("data").getBoolean("pstn")) {
                    callPSTN();
                } else {
                    loadWebViewUrl(SIP_CLIENT_INIT_JSSIP);
                }
            } else {
                if (callData.getBoolean("pstn")) {
                    callPSTN();
                } else {
                    loadWebViewUrl(SIP_CLIENT_INIT_JSSIP);
                }
            }

        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Problem occurs while initializing the SIP client: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    };

    Emitter.Listener onCallStatus = args -> {
        try {
            JSONObject callStatus = (JSONObject) args[0];
            if (callStatus.has("alone") && callStatus.getBoolean("alone")) {
                overCallOnSip();
            } else if (callStatus.has("left")) {
                SharedPreferences sharedPref = StorageHelper.getPreferences(activity);
                String leftEndpoint = callStatus.getJSONObject("left").has("endpoint") ?
                        callStatus.getJSONObject("left").getString("endpoint") : "";
                if (leftEndpoint.equals(sharedPref.getString(KEY_CONTACT_CUID, ""))) {
                    overCallOnSip();
                }
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error occurs while handling the changes in call state: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    };

    Emitter.Listener onConnectionTimeout = args -> { };

    Emitter.Listener onReconnect = args -> { };

    Emitter.Listener onReconnectionAttempt = args -> { };

    Emitter.Listener onReconnecting = args -> { };

    Emitter.Listener onReconnectionFailed = args -> {
        try {
            loadWebViewUrl(SIP_CLIENT_HANGUP);

            if (appContext != null)
                Utils.getInstance().sendBroadcast(appContext, Constants.ACTION_CALL_OVER);

            callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Problem occurs while hanging up the call on reconnection failed: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    };

    Emitter.Listener onReconnectError = args -> { };

    public void setSpeakerOff() {
        isSpeakerOn = false;
        ivSpeaker.setImageResource(R.drawable.ct_speaker_outline);
    }

    public void switchSpeakerState() {
        if (/*audioManager.isSpeakerphoneOn()*/ isSpeakerOn) {
            isSpeakerOn = false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                Utils.getInstance().setCommunicationDevice(getContext(), AudioDeviceInfo.TYPE_BUILTIN_EARPIECE);
            } else {
                audioManager.setSpeakerphoneOn(false);
            }
            ivSpeaker.setImageResource(R.drawable.ct_speaker_outline);
        } else {
            isSpeakerOn = true;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                Utils.getInstance().setCommunicationDevice(getContext(), AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
            } else {
                audioManager.setSpeakerphoneOn(true);
            }
            if (isBlueToothOn) {
                isBlueToothOn = false;
                ivBluetooth.setImageResource(R.drawable.ct_bluetooth_outline);
            }
            ivSpeaker.setImageResource(R.drawable.ct_speaker_filled);
        }
    }

    public void emitHoldOnSigSock(Boolean hold) {
        if (activity != null && callType != null) {
            String oppositeCuId = null;
            if (callType.equals("incoming")) {
                oppositeCuId = fromCuid;
            } else if (callType.equals("outgoing")) {
                oppositeCuId = toCuid;
            }

            try {
                if (SocketIOManager.getSocket() != null && SocketIOManager.getSocket().connected()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("hold", hold);
                    jsonObject.put("cuid", oppositeCuId);
                    SocketIOManager.getSocket().emit(getString(R.string.sHoldUnhold), jsonObject);
                }
            }catch (Exception ignored){
                //really, will never come here

            }
        }
    }

    public void switchMuteState() {
        if (DirectOngoingCallFragment.getInstance() != null) {
            if (!isHoldDueToPSTN && !isPutOnHold && !isHold) {
                if (isMute) {
                    isMute = false;

                    loadWebViewUrl(SIP_CLIENT_UN_MUTE);

                    ivMute.setImageResource(R.drawable.ct_mute_outline);
                } else {
                    isMute = true;

                    loadWebViewUrl(SIP_CLIENT_MUTE);

                    ivMute.setImageResource(R.drawable.ct_mute_filled);
                }
            }
        }
    }

    public void switchHoldState() {
        try {
            if (isHoldDueToPSTN) {
                isHoldDueToPSTN = false;
                loadWebViewUrl(SIP_CLIENT_UN_MUTE);

                tvHoldState.setVisibility(View.GONE);
                tvHoldState.clearAnimation();
                getInstance().emitHoldOnSigSock(false);
            } else {
                isHoldDueToPSTN = true;
                loadWebViewUrl(SIP_CLIENT_MUTE);
                tvHoldState.setVisibility(View.VISIBLE);
                tvHoldState.setText(getString(R.string.call_is_on_hold));
                tvHoldState.startAnimation(Utils.getInstance().getAlphaAnimator());
                getInstance().emitHoldOnSigSock(true);
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while switching the Hold<->UnHold state: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void closeCallScreen() {
        try {
            callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
            if (activity != null) {
                activity.finishAndRemoveTask();
            }
            CallScreenUtil.getInstance().releaseMediaPlayer();
            DataStore.getInstance().setClientbusyOnVoIP(false);
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while closing the call screen: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void hangupCall() {
        try {
            callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

            if (callData.has("data") && callData.getJSONObject("data").getBoolean("pstn") && !isPstnCallAnswered) {
                JSONObject tocall = callData.getJSONObject("to");
                socket.emit(getString(R.string.s_cancel_pstn), tocall, (Ack) args -> {
                    try {
                        JSONObject pstnCallback = (JSONObject) args[0];
                        boolean status = pstnCallback.getBoolean("status");
                        if (status) {
                            if (activity != null) {
                                CallScreenUtil.getInstance().releaseMediaPlayer();
                                activity.finishAndRemoveTask();
                            }
                            DataStore.getInstance().setClientbusyOnVoIP(false);
                        }
                    } catch (Exception e) {
                        DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in while cancelling the PSTN call: " + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                loadWebViewUrl(SIP_CLIENT_HANGUP);
                DataStore.getInstance().setClientbusyOnVoIP(false);
                if (getActivity() != null && getActivity().getApplicationContext() != null)
                    Utils.getInstance().sendBroadcast(getActivity().getApplicationContext(), Constants.ACTION_CALL_OVER);
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in while cancelling the PSTN call: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }


    /**
     * check if all the permissions are given by the user before making a pstn call..
     */
    private void callPSTN() {
        try {
            String[] PERMISSIONS = {
                    android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    android.Manifest.permission.RECORD_AUDIO
            };
            if (Utils.getInstance().hasPermissions(getContext(), PERMISSIONS)) {
                startPSTN();
            } else {
                if (activity != null)
                    activity.finishAndRemoveTask();
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while initiating the PSTN call: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * starts the outgoing tone when user is trying to make a outgoing call, and emits a call_pstn on callsock to make
     * a PSTN call to the receiver.
     */
    private void startPSTN() throws JSONException {
        JSONObject toCall = callData.getJSONObject("to");
        try {
            CallScreenUtil.getInstance().playOutgoingRingtone(getContext());
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while initiating the PSTN call: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        if (socket != null)
            socket.emit(getString(R.string.s_call_pstn), toCall, (Ack) args -> {
                try {
                    JSONObject pstnCallback = (JSONObject) args[0];
                    boolean status = pstnCallback.getBoolean("status");
                    if (status) {
                        runOnMainThread(() -> {
                            if (callContext != null) {
                                try {
                                    tvCallScreenLabel.setText(getString(R.string.ongoing_call));

                                    callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

                                    callNotificationHandler.showCallNotification(new JSONObject().put("context", callContext)
                                            , NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);

                                    customHandler.sendCallAnnotation(DataStore.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, null, VoIPCallStatus.CALL_ANSWERED);

                                    isPstnCallAnswered = true;

                                    CallScreenUtil.getInstance().releaseMediaPlayer();

                                    loadWebViewUrl(SIP_CLIENT_INIT_JSSIP);
                                } catch (JSONException ignored) {
                                    //no-op
                                }
                            }
                        });


                    } else {
                        String error = pstnCallback.getString("error");
                        switch (error) {
                            case "declined":
                                DirectCallAPI.sdkReady = true;
                                customHandler.sendCallAnnotation(DataStore.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, null, VoIPCallStatus.CALL_DECLINED);
                                break;
                            case "missed":
                                DirectCallAPI.sdkReady = true;
                                customHandler.sendCallAnnotation(DataStore.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, null, VoIPCallStatus.CALL_MISSED);
                                break;
                            case "Invalid or missing cc/phone":
                                DirectCallAPI.sdkReady = true;
                                customHandler.sendCallAnnotation(DataStore.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.ON_FAILURE, CallException.CcPhoneMissingForCuidInPstnCallException, null);
                                break;
                        }
                        try {
                            if (activity != null) {
                                if (callNotificationHandler.getCallNotificationStatus().equals(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL))
                                    callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                                CallScreenUtil.getInstance().releaseMediaPlayer();
                                activity.finishAndRemoveTask();
                            }
                        } catch (Exception e) {
                            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while handling the PSTN call: " + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while handling the PSTN call: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            });
    }

    /**
     * starts the call timer to be displayed on the UI.
     */
    private void startTimer() {
        try {
            runOnMainThread(() -> {
                try {
                    chTimer.setBase(SystemClock.elapsedRealtime());
                    chTimer.start();
                } catch (Exception e) {
                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while displaying the timer on screen: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            //no-op
        }
    }

    @Override
    public void onCancel() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        if (activity != null) {
            activity.finishAndRemoveTask();
            DataStore.getInstance().setClientbusyOnVoIP(false);
        }
    }


    /**
     * To get an incoming call from asterisk it emits call_voip on callSock once the SIP client is registered on sip channel .
     */
    @Override
    public void callVoip() {
        if (socket != null) {
            socket.emit(getString(R.string.s_call_voip), "", (Ack) args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    boolean status = data.getBoolean("status");
                    if (status) {
                        if (DataStore.getInstance().getRecording() != null && DataStore.getInstance().getRecording()) {
                            socket.emit(getString(R.string.s_record_call), callData.getString("call"), new Ack() {
                                @Override
                                public void call(Object... args) {
                                }
                            });
                        }
                        runOnMainThread(() -> {
                            try {
                                tvCallScreenLabel.clearAnimation();
                                tvCallScreenLabel.setText(getString(R.string.ongoing_call));
                              /*  ivMute.setVisibility(View.VISIBLE);
                                ivSpeaker.setVisibility(View.VISIBLE);
                                chTimer.setVisibility(View.VISIBLE);*/
                                ivMute.setAlpha(1.0f);
                                ivSpeaker.setAlpha(1.0f);
                                ivBluetooth.setAlpha(1.0f);
                                ivMute.setEnabled(true);
                                ivSpeaker.setEnabled(true);
                                ivBluetooth.setEnabled(true);
                                chTimer.setVisibility(View.VISIBLE);

                                if (audioSwitchUtil.isBluetoothHeadsetConnected()) {
                                    isBlueToothOn = true;
                                    ivBluetooth.setImageResource(R.drawable.ct_bluetooth_filled);
                                }
                                if (templateType != null &&
                                        templateType.equals(getString(R.string.template_scratchcard))
                                        && DataStore.getInstance().isScratchCardScratched()) {
                                    ivBannerImage.setVisibility(View.VISIBLE);
                                }
                                if (callContext != null) {
                                    callNotificationHandler.showCallNotification(new JSONObject().put("context", callContext), NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                                }
                            } catch (Exception e) {
                                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while updating the details on call screen: " + e.getLocalizedMessage());
                                e.printStackTrace();
                            }
                        });
                        startTimer();
                    } else {
                        overCallOnSip();
                    }
                } catch (Exception e) {
                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while initiating the RTP packet switching: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            });
        }
    }


    BroadcastReceiver bluetoothConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean shouldEnableExternalSpeaker = false;
            switch (action) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    shouldEnableExternalSpeaker = true;
                    setBluetoothControllerVisibility(BluetoothControllerVisibility.FULL);
                    ivBluetooth.setImageResource(R.drawable.ct_bluetooth_filled);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    shouldEnableExternalSpeaker = false;
                    setBluetoothControllerVisibility(BluetoothControllerVisibility.HIDDEN);
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        setBluetoothControllerVisibility(BluetoothControllerVisibility.HIDDEN);
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        setBluetoothControllerVisibility(BluetoothControllerVisibility.PARTIAL);
                    }
            }
            audioSwitchUtil.setupAudioOutputDevice(shouldEnableExternalSpeaker, false);
        }
    };

    public void setBluetoothControllerVisibility(BluetoothControllerVisibility visibility) {
        if (visibility == BluetoothControllerVisibility.PARTIAL) {
            isBlueToothOn = false;
            ivBluetooth.setAlpha(0.5f);
            ivBluetooth.setEnabled(false);
            ivBluetooth.setVisibility(View.VISIBLE);
        } else if (visibility == BluetoothControllerVisibility.FULL) {
            isBlueToothOn = true;
            ivBluetooth.setAlpha(1f);
            ivBluetooth.setEnabled(true);
            ivBluetooth.setVisibility(View.VISIBLE);
        } else {
            isBlueToothOn = false;
            ivBluetooth.setVisibility(View.GONE);
        }
    }

    public void switchSpeakerDevice() {
        if (isBlueToothOn) {
            isBlueToothOn = false;
            ivBluetooth.setImageResource(R.drawable.ct_bluetooth_outline);
        } else {
            isSpeakerOn = false;
            ivSpeaker.setImageResource(R.drawable.ct_speaker_outline);
        }
    }

    public void putOnHold(final Boolean hold) {
        try {
            runOnMainThread(() -> {
                try {
                    if (isFragmentVisible) {
                        if (hold) {
                            isPutOnHold = true;
                            loadWebViewUrl(SIP_CLIENT_MUTE);
                            tvHoldState.setText("Call is put on hold");
                            tvHoldState.setVisibility(View.VISIBLE);
                            tvHoldState.startAnimation(Utils.getInstance().getAlphaAnimator());
                            long[] pattern = {0, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500};
                            vibrator.vibrate(pattern, -1);
                        } else {
                            isPutOnHold = false;
                            tvHoldState.setVisibility(View.GONE);
                            tvHoldState.setText("");
                            tvHoldState.clearAnimation();
                            vibrator.cancel();
                            loadWebViewUrl(SIP_CLIENT_UN_MUTE);
                        }
                        getInstance().restoreMuteState();
                    }
                } catch (Exception e) {
                    //no handling
                }
            });
        } catch (Exception e) {
        }
    }

    private void restoreMuteState() {
        try {
            if (isMute) {
                loadWebViewUrl(SIP_CLIENT_MUTE);
            } else {
                //loadWebViewUrl(SIP_CLIENT_UNMUTE);
            }
        } catch (Exception e) {
            //no-op
        }
    }

    // credits to @Heath Borders at http://stackoverflow.com/questions/20228800/how-do-i-validate-an-android-net-http-sslcertificate-with-an-x509trustmanager
    private Certificate getX509Certificate(SslCertificate sslCertificate) {
        try {
            Bundle bundle = SslCertificate.saveState(sslCertificate);
            byte[] bytes = bundle.getByteArray("x509-certificate");
            if (bytes == null) {
                return null;
            } else {
                try {
                    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                    return certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                } catch (Exception e) {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onActionClick(String action) {
        DirectCallAPI.sdkReady = true;
        hangupCall();
    }

    public void raiseDcEndSystemEvent() {
        if (accountId == null || callId == null) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX,
                    "DC system event details are null! dropping further processing");
            return;
        }

        DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callContext)
                .setCallStatus(CallStatus.CALL_OVER)
                .build();
        DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_END, dcSystemEventInfo);
    }

    public void overCallOnSip() {
        DirectCallAPI.sdkReady = true;
        loadWebViewUrl(SIP_CLIENT_HANGUP);

        if (appContext != null) {
            Utils.getInstance().sendBroadcast(appContext, Constants.ACTION_CALL_OVER);
        }
        if (callNotificationHandler != null) {
            callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
        }
    }

    private void loadWebViewUrl(String url) {
        new Handler(Looper.getMainLooper()).post(() -> jsView.loadUrl(url));
    }

    private void runOnMainThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}

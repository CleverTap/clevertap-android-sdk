package com.clevertap.android.directcall.ui;

import static com.clevertap.android.directcall.Constants.ACTION_ACTIVITY_FINISH;
import static com.clevertap.android.directcall.Constants.ACTION_OUTGOING_HANGUP;
import static com.clevertap.android.directcall.Constants.BROADCAST_ACTION;
import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.CALL_FINISH_ACTION;
import static com.clevertap.android.directcall.init.DirectCallAPI.sdkReady;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.enums.CallStatus;
import com.clevertap.android.directcall.events.CTSystemEvent;
import com.clevertap.android.directcall.events.DCSystemEventInfo;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.interfaces.CallNotificationAction;
import com.clevertap.android.directcall.interfaces.ICallStatusHandler;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.javaclasses.VoIPCallStatus;
import com.clevertap.android.directcall.recievers.CallNotificationActionReceiver;
import com.clevertap.android.directcall.utils.CallScreenUtil;
import com.clevertap.android.directcall.utils.CustomHandler;
import com.clevertap.android.directcall.utils.NotificationHandler.CallNotificationHandler;
import com.clevertap.android.directcall.utils.Utils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.socket.client.Ack;

public class DirectOutgoingCallFragment extends Fragment implements ICallStatusHandler, ActivityCompat.OnRequestPermissionsResultCallback, CallNotificationAction {
    private View rootViewBackground;
    private ImageView ivHangup, ivLogo;
    private TextView tvContext, tvCallStatus, tvCallScreenLabel, tvPoweredBy, tvCalleeBusy;
    private JSONObject callDetails;
    private String callContext;
    private CallNotificationHandler callNotificationHandler;
    private final CustomHandler customHandler = CustomHandler.getInstance();
    private Vibrator vibrator;
    private FragmentActivity activity;
    private Context appContext;
    private final int BUSY_MESSAGE_COUNTDOWN_TIME = 10000;
    private final int CALL_STATUS_COUNTDOWN_TIME = 2000;
    private final String CALL_DECLINED = "Declined";
    private final String CALL_MISSED = "Missed";

    public DirectOutgoingCallFragment() {
    }

    private final BroadcastReceiver eventsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String replaceAction = intent.getStringExtra(BROADCAST_ACTION);
                if (replaceAction != null && replaceAction.equals(ACTION_ACTIVITY_FINISH)) {
                    activity.runOnUiThread(() -> {
                        try {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(DataStore.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, null, VoIPCallStatus.CALL_ANSWERED);

                            releaseResources();

                            closeScreen();
                        }catch (Exception e){
                            //no-op
                        }
                    });
                }
            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error when closing the outgoing call screen: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_direct_call_outgoing_screen, container, false);
        initViews(view);
        
        if(getActivity() != null) {
            activity = getActivity();
            appContext = activity.getApplicationContext();
            vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);

            LocalBroadcastManager.getInstance(activity).
                    registerReceiver(eventsReceiver, new IntentFilter(CALL_FINISH_ACTION));

            try {
                callNotificationHandler = CallNotificationHandler.getInstance(appContext);
                CallNotificationActionReceiver.setCallNotificationActionListener(this);

                Bundle extras = getArguments();
                if(extras != null) {
                    callDetails = new JSONObject(extras.getString("callDetails"));
                    callContext = callDetails.getJSONObject("data").getString("context");
                    String fromCuID = callDetails.getJSONObject("data").getJSONObject("from").getString("cuid");
                    String toCuID = callDetails.getJSONObject("data").getJSONObject("to").getString("cuid");

                    persistCallInfo(fromCuID, toCuID);

                    updateUI();

                    //playing ringtone
                    CallScreenUtil.getInstance().playOutgoingRingtone(getContext());
                }
            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while parsing the call info: " + e.getLocalizedMessage());
                e.printStackTrace();
            }

            ivHangup.setOnClickListener(v -> {
                sdkReady = true;
                hangupCall();
            });
        }

        return view;
    }

    private void initViews(View view) {
        ivHangup = view.findViewById(R.id.iv_decline);
        tvContext = view.findViewById(R.id.tv_callContext);
        tvCallStatus = view.findViewById(R.id.tv_callStatus);
        rootViewBackground = view.findViewById(R.id.root_view_background);
        tvCallScreenLabel = view.findViewById(R.id.tv_callScreen_label);
        tvCalleeBusy = view.findViewById(R.id.tv_busy_state);
        ivLogo = view.findViewById(R.id.iv_brand_logo);
        tvPoweredBy = view.findViewById(R.id.tv_poweredBy);
    }

    private void updateUI() {
        tvContext.setText(callContext);

        setBranding();
    }

    private void persistCallInfo(String fromCuID, String toCuID) {
        DataStore.getInstance().setCallStatus(DirectOutgoingCallFragment.this);

        DataStore.getInstance().setToCuid(toCuID);
        DataStore.getInstance().setFromCuid(fromCuID);
    }

    public void hangupCall() {
        try {

            DataStore dataStore = DataStore.getInstance();
            dataStore.getTimer().cancel();

            final String callId = callDetails.getString(getString(R.string.scall));
            dataStore.getSocket().emit(getString(R.string.s_cancel), callId, new Ack() {
                @Override
                public void call(Object... args) {
                    //Raising the system event for incoming call
                    DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callContext)
                            .setCallStatus(CallStatus.CALL_CANCELLED)
                            .build();
                    DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_END, dcSystemEventInfo);
                }
            });

            releaseResources();
            closeScreen();

        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while hanging up the outgoing call screen: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void closeScreen() {
        //Removing outgoing notification
        if(callNotificationHandler != null)
            callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

        if (activity != null) {
            activity.finish();
        }
    }

    private void releaseResources() {
        if (activity != null && appContext != null)
            CallScreenUtil.getInstance().stopMediaPlayer();
    }

    private void setBranding() {
        Map<String, View> brandingViewParams = new HashMap<>();
        brandingViewParams.put(CallScreenUtil.BrandingViews.ivLogo, ivLogo);
        brandingViewParams.put(CallScreenUtil.BrandingViews.rootViewBackground, rootViewBackground);
        brandingViewParams.put(CallScreenUtil.BrandingViews.tvContext, tvContext);
        brandingViewParams.put(CallScreenUtil.BrandingViews.tvCallScreenLabel, tvCallScreenLabel);
        brandingViewParams.put(CallScreenUtil.BrandingViews.tvPoweredBy, tvPoweredBy);
        brandingViewParams.put(CallScreenUtil.BrandingViews.OUTGOING.tvCallStatus, tvCallStatus);

        CallScreenUtil.getInstance().setBranding(getContext(), brandingViewParams, CallScreenUtil.DCCallScreenType.OUTGOING);
    }

    /**
     * To display the respective event on the caller's screen before ending the screen,
     * it starts the countdown timer of 2 seconds when the opposite party(i.e. receiver) either declines or misses the call
     */
    private void startEndTimer(final int countDownTime, boolean isBusyHandlingTimer) {
        new CountDownTimer(countDownTime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                try {
                    sdkReady = true;

                    if (isBusyHandlingTimer) {
                        tvCalleeBusy.clearAnimation();
                        DataStore.getInstance().setCalleBusyOnAnotherCall(false);
                        stopVibration();
                    }

                    closeScreen();

                } catch (Exception e) {
                    sdkReady = true;
                    try {
                        callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                    } catch (Exception e1) {
                        //no-op
                    }
                    DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while displaying the end timer details: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void stopVibration() {
        if(vibrator!=null)
            vibrator.cancel();
    }

    /**
     * called when opposite party(i.e. receiver) answered the call.
     */
    @Override
    public void onAnswer() {
        try {
            String[] PERMISSIONS = {
                    android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    android.Manifest.permission.RECORD_AUDIO
            };
            if (activity != null && appContext != null && Utils.getInstance().hasPermissions(appContext, PERMISSIONS)) {
                //Reporting new Answered call's state to the exposed callback
                customHandler.sendCallAnnotation(DataStore.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, null, VoIPCallStatus.CALL_ANSWERED);

                activity.runOnUiThread(() -> {
                    try {
                        //Removing outgoing notification, displaying ongoing notification
                        callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                        if (callContext != null) {
                            callNotificationHandler.showCallNotification(
                                    new JSONObject().put("context", callContext),
                                    CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                        }

                        releaseResources();

                        DirectOngoingCallFragment ongoingCallFragment = new DirectOngoingCallFragment();
                        Bundle args = new Bundle();
                        args.putString("callDetails", callDetails.toString());
                        ongoingCallFragment.setArguments(args);
                        updateViewFragment(activity, ongoingCallFragment);
                    } catch (Exception e) {
                        DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error when an outgoing call is answered: " + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                DataStore.getInstance().getTimer().cancel();
                releaseResources();
                closeScreen();
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error when an outgoing call is answered: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            releaseResources();

            DataStore.getInstance().setClientbusyOnVoIP(false);

            stopVibration();

        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while releasing the occupied resources: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        stopVibration();
    }

    /**
     * called when incoming call is declined by the receiver so close the current view after 2 seconds
     */
    @Override
    public void onDecline() {
        if(activity != null)
        activity.runOnUiThread(() -> {
            try {
                releaseResources();

                if (DataStore.getInstance().isCalleBusyOnAnotherCall()) {
                    tvCalleeBusy.setVisibility(View.VISIBLE);
                    tvCalleeBusy.setAnimation(Utils.getInstance().getAlphaAnimator());
                    startEndTimer(BUSY_MESSAGE_COUNTDOWN_TIME, true);
                    vibrate(0);
                    customHandler.sendCallAnnotation(DataStore.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, null, VoIPCallStatus.CALLEE_BUSY_ON_ANOTHER_CALL);
                } else {
                    customHandler.sendCallAnnotation(DataStore.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, null, VoIPCallStatus.CALL_DECLINED);
                    tvCallStatus.setText(CALL_DECLINED);
                    startEndTimer(CALL_STATUS_COUNTDOWN_TIME, false);
                }
            }catch (Exception e){
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception when outgoing call is declined: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        });
    }

    private void vibrate(int repeatCount) {
        long[] pattern = {0, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500};
        if (vibrator != null)
            vibrator.vibrate(pattern, repeatCount);
    }

    /**
     * called when incoming call is missed by the receiver so close the current view after 2 seconds.
     */
    @Override
    public void onMiss() {
        activity.runOnUiThread(() -> {
            try {
                if (activity != null && appContext != null) {
                    CallScreenUtil.getInstance().stopMediaPlayer();
                    callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                }
                DirectCallAPI.sdkReady = true;
                customHandler.sendCallAnnotation(DataStore.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, null, VoIPCallStatus.CALL_MISSED);
                tvCallStatus.setText(CALL_MISSED);
                startEndTimer(CALL_STATUS_COUNTDOWN_TIME, false);
            }catch (Exception e){
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception when outgoing call is missed: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * called when initiating a PSTN call if the receiver's platform is iOS and the AutoFallback is true.
     */
    @Override
    public void onIosApf(String data) {
        try {
            DirectCallAPI.getLogger().verbose(CALLING_LOG_TAG_SUFFIX, "Initiating an iOS-APF call");

            DirectOngoingCallFragment directOngoingCallFragment = new DirectOngoingCallFragment();
            Bundle args = new Bundle();
            args.putString("callDetails", data);
            directOngoingCallFragment.setArguments(args);
            updateViewFragment(activity, directOngoingCallFragment);

        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in iOS AutoFallback handling: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void updateViewFragment(FragmentActivity activity, Fragment fragment){
        if(activity != null){
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, fragment);
            ft.addToBackStack(null);
            ft.remove(DirectOutgoingCallFragment.this);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (callNotificationHandler.getCallNotificationStatus() != null &&
                    callNotificationHandler.getCallNotificationStatus().equals(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL))
                callNotificationHandler.rebuildNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

            if (activity != null)
                LocalBroadcastManager.getInstance(activity).unregisterReceiver(eventsReceiver);
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error when closing the outgoing call screen: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onActionClick(String action) {
        if (action != null && action.equals(ACTION_OUTGOING_HANGUP)) {
            sdkReady = true;
            hangupCall();
        }
    }

}

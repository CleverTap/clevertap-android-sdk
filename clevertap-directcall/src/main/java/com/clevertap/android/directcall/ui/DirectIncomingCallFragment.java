package com.clevertap.android.directcall.ui;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.DEFAULT_PIN_VIEW_ITEM_COUNT;
import static com.clevertap.android.directcall.Constants.KEY_ACCOUNT_ID;
import static com.clevertap.android.directcall.Constants.KEY_DECLINE_REASON;
import static com.clevertap.android.directcall.Constants.KEY_DECLINE_REASON_CODE;
import static com.clevertap.android.directcall.Constants.KEY_NEVER_ASK_AGAIN_PERMISSION;
import static com.clevertap.android.directcall.Constants.KEY_TEMPLATE_PIN_VIEW_CONFIG;
import static com.clevertap.android.directcall.Constants.KEY_TEMPLATE_SCRATCHCARD_CONFIG;
import static com.clevertap.android.directcall.Constants.REASON_CODE_MICROPHONE_PERMISSION_NOT_GRANTED;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.directcall.custom.DirectCallPinView;
import com.clevertap.android.directcall.custom.DCScratchCard;
import com.clevertap.android.directcall.enums.CallStatus;
import com.clevertap.android.directcall.events.CTSystemEvent;
import com.clevertap.android.directcall.events.DCSystemEventInfo;
import com.clevertap.android.directcall.fcm.FcmCacheManger;
import com.clevertap.android.directcall.fcm.FcmUtil;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.interfaces.CallNotificationAction;
import com.clevertap.android.directcall.interfaces.ICallStatusHandler;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.models.DirectCallTemplates;
import com.clevertap.android.directcall.models.SelectedTemplate;
import com.clevertap.android.directcall.recievers.CallNotificationActionReceiver;
import com.clevertap.android.directcall.utils.CallScreenUtil;
import com.clevertap.android.directcall.utils.NotificationHandler;
import com.clevertap.android.directcall.utils.PinViewObservable;
import com.clevertap.android.directcall.utils.SocketIOManager;
import com.clevertap.android.directcall.utils.Utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.Ack;

public class DirectIncomingCallFragment extends Fragment implements ICallStatusHandler.incomingCallStatus, ActivityCompat.OnRequestPermissionsResultCallback, CallNotificationAction {
    private View view, rootViewBackground;
    private ImageView ivDecline, ivAccept, ivLogo;
    private JSONObject callDetails;
    private String callContext, fromCuID, toCuID;
    private TextView tvContext, tvCallScreenLabel, tvPoweredBy;
    private AudioManager audioManager;
    private NotificationHandler.CallNotificationHandler callNotificationHandler;
    private int pinViewItemCount;
    private TextView tvPinLabel;
    View bannerPlaceholder;
    DirectCallPinView directCallPinView;
    VideoView videoView;

    public static AudioManager.OnAudioFocusChangeListener afChangeListener;

    private Group mScratchOuterLayer;
    private DCScratchCard scratchView;

    private static DirectIncomingCallFragment instance;

    public static DirectIncomingCallFragment getInstance() {
        return instance;
    }

    String templateType, templateUrl;
    Bundle extras;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            instance = this;

            templateType = getString(R.string.template_default);
            int resLayout = R.layout.fragment_direct_call_incoming_banner_screen;

            resetTemplateSettings();

            extras = getArguments();
            if(extras == null && getActivity() != null) {
                getActivity().finish();
            }

            callDetails = new JSONObject(getArguments().getString(getString(R.string.callDetails)));

            if (callDetails.has(getString(R.string.active_template)) && callDetails.get(getString(R.string.active_template)) instanceof JSONObject) {
                SelectedTemplate selectedTemplate = SelectedTemplate.fromJson(
                        callDetails.getJSONObject(getString(R.string.active_template)));

                if (selectedTemplate.getType() != null && selectedTemplate.getUrl() != null) {
                    templateType = selectedTemplate.getType();
                    templateUrl = selectedTemplate.getUrl();
                }
                resLayout = getActiveTemplateLayout(templateType);
            }

            view = inflater.inflate(resLayout, container, false);

        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Failed to render the layout for incoming call screen: " + e.getLocalizedMessage());
            e.printStackTrace();
        }

        renderActiveTemplateUI();

        try {
            initViews();

            if(getActivity() != null && getActivity().getApplicationContext() != null)
            callNotificationHandler = NotificationHandler.CallNotificationHandler.getInstance(getActivity().getApplicationContext());

            CallNotificationActionReceiver.setCallNotificationActionListener(this);

            callContext = callDetails.getString(getString(R.string.scontext));

            if (callDetails.has("from")) {
                JSONObject fromObj = callDetails.getJSONObject("from");
                if(fromObj.has("cuid")) {
                    fromCuID = fromObj.getString("cuid");
                }
            }
            if (callDetails.has("to")) {
                JSONObject toObj = callDetails.getJSONObject("to");
                if(toObj.has("cuid")) {
                    toCuID = toObj.getString("cuid");
                }
            }

            updateUI();

            DataStore.getInstance().setIncomingCallStatus(DirectIncomingCallFragment.this);
            audioManager = DataStore.getInstance().getAudioManager();

        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while parsing the call details: " + e.getLocalizedMessage());
            e.printStackTrace();
        }

        ivAccept.setOnClickListener(v -> onAcceptClick());

        ivDecline.setOnClickListener(v -> callDeclined(null));

        if (getActivity() != null && extras.getString(getActivity().getString(R.string.call_answer)) != null) {
            onAcceptClick();
        }
        return view;
    }

    private void initViews() {
        ivDecline = view.findViewById(R.id.iv_decline);
        ivAccept = view.findViewById(R.id.iv_accept);
        tvContext = view.findViewById(R.id.tv_callContext);
        ivLogo = view.findViewById(R.id.iv_brand_logo);
        tvCallScreenLabel = view.findViewById(R.id.tv_callScreen_label);
        rootViewBackground = view.findViewById(R.id.root_view_background);
        tvPoweredBy = view.findViewById(R.id.tv_poweredBy);
    }

    private void renderActiveTemplateUI() {
        try {
            if (templateType.equals(getString(R.string.template_full_image))) {
                ImageView ivBg = view.findViewById(R.id.iv_bg);
                Glide.with(this)
                        .load(templateUrl)
                        .into(ivBg);
            } else if (templateType.equals(getString(R.string.template_banner_image)) || templateType.equals(getString(R.string.template_gif))) {
                bannerPlaceholder = view.findViewById(R.id.banner_placeholder);
                ImageView ivBg = view.findViewById(R.id.iv_banner_image_gif);
                bannerPlaceholder.setVisibility(View.VISIBLE);
                ivBg.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(templateUrl)
                        .into(ivBg);
            } else if (templateType.equals(getString(R.string.template_video))) {
                bannerPlaceholder = view.findViewById(R.id.banner_placeholder);
                videoView = view.findViewById(R.id.banner_video);
                bannerPlaceholder.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.VISIBLE);
                videoView.setZOrderOnTop(true);
                videoView.setVideoPath(templateUrl);
                videoView.setOnPreparedListener(mp -> {
                    videoView.start();
                    mp.setLooping(true);
                });
                videoView.setOnInfoListener((mediaPlayer, i, i1) -> {
                    if (i == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        //first frame was buffered
                        videoView.setZOrderOnTop(false);
                        return true;
                    }
                    return false;
                });
                videoView.setOnErrorListener((mp, what, extra) -> {
                    videoView.setVisibility(View.INVISIBLE);
                    return true;
                });
            }else if (templateType.equals(getString(R.string.template_pinview))) {
                directCallPinView = view.findViewById(R.id.direct_call_pin_view);
                tvPinLabel = view.findViewById(R.id.tv_pin_label);
                String pinViewConfigJson = StorageHelper.getString(getContext(), KEY_TEMPLATE_PIN_VIEW_CONFIG, null);

                if(pinViewConfigJson != null){
                    DirectCallTemplates.PinView pinViewConfig = DirectCallTemplates.PinView.fromJson(new JSONObject(pinViewConfigJson));
                    pinViewItemCount = pinViewConfig.getItemCount();
                    if(pinViewItemCount !=0){
                        directCallPinView.setItemCount(pinViewConfig.getItemCount());
                    }else {
                        pinViewItemCount = DEFAULT_PIN_VIEW_ITEM_COUNT;
                    }

                    directCallPinView.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                            try {
                                directCallPinView.setError(null);
                                if(charSequence.length() == pinViewItemCount){
                                    if(PinViewObservable.getInstance().getPinviewTextObserver() != null){
                                        PinViewObservable.getInstance().getPinviewTextObserver()
                                                .onPinTextCompleted(String.valueOf(charSequence), new PinViewObservable.PinTextVerificationHandler() {
                                                    @Override
                                                    public void onSuccess() {
                                                        onCallPicked();
                                                    }

                                                    @Override
                                                    public void onFailure() {
                                                        directCallPinView.setError(getString(R.string.wrong_pin));
                                                    }
                                                });
                                    }
                                }
                            }catch (Exception e){
                                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while observing the text changes in Pinview: " + e.getLocalizedMessage());
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void afterTextChanged(Editable editable) {

                        }
                    });
                }
            }else if(templateType.equals(getString(R.string.template_scratchcard))){
                View clScratchCardView = view.findViewById(R.id.cl_scratch_card);
                scratchView = view.findViewById(R.id.scratch_view);
                mScratchOuterLayer = view.findViewById(R.id.scratch_outer_layer);
                TextView tvScratchHeader = view.findViewById(R.id.tv_scratch_header);
                TextView tvScratchFooter = view.findViewById(R.id.tv_scratch_footer);
                View viewInnerOverlay = view.findViewById(R.id.view_inner_overlay);
                TextView tvScratchInner = view.findViewById(R.id.tv_scratch_inner);
                ImageView ivScratchInner = view.findViewById(R.id.iv_scratch_inner);

                String scratchCardConfigJson = StorageHelper.getString(getContext(), KEY_TEMPLATE_SCRATCHCARD_CONFIG, null);

                if(scratchCardConfigJson != null) {
                    DirectCallTemplates.ScratchCard scratchCardConfig = DirectCallTemplates.ScratchCard.fromJson(new JSONObject(scratchCardConfigJson));
                    if(scratchCardConfig!=null){
                        if(scratchCardConfig.getOuterDrawableRes()!=0 &&
                                scratchCardConfig.getInnerText()!=null && !scratchCardConfig.getInnerText().equals("") &&
                                scratchCardConfig.getInnerDrawableRes()!=0){
                            clScratchCardView.setVisibility(View.VISIBLE);
                            scratchView.setOuterScratchDrawable(scratchCardConfig.getOuterDrawableRes()); //Outer overlay
                            tvScratchHeader.setText(scratchCardConfig.getOuterTextHeader()); //Outer text header
                            tvScratchFooter.setText(scratchCardConfig.getOuterTextFooter()); //Outer text footer
                            viewInnerOverlay.setBackgroundResource(scratchCardConfig.getInnerBackgroundColorRes()); //Inner Background Color
                            tvScratchInner.setText(scratchCardConfig.getInnerText()); //Inner text
                            ivScratchInner.setImageResource(scratchCardConfig.getInnerDrawableRes()); //Inner image

                            scratchView.setOnScratchListener((DCScratchCard, visiblePercent) -> {
                                if (visiblePercent > 0.4) {
                                    setScratchVisibility(true);
                                    mScratchOuterLayer.setVisibility(View.GONE);
                                    DataStore.getInstance().setScratchCardScratched(true);
                                }
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Failed to parse the template details: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void updateUI() {
        tvContext.setText(callContext);

        setBranding();

        final Animation animShake = AnimationUtils.loadAnimation(getContext(), R.anim.ct_anim_shake);

        ivDecline.startAnimation(animShake);
        ivAccept.startAnimation(animShake);
    }

    private int getActiveTemplateLayout(String templateType) {
        if (templateType.equals(getString(R.string.template_full_image))) {
            return R.layout.fragment_direct_call_incoming_full_image_screen;
        } else if (templateType.equals(getString(R.string.template_video)) ||
                templateType.equals(getString(R.string.template_banner_image)) ||
                templateType.equals(getString(R.string.template_gif)) ||
                templateType.equals(getString(R.string.template_scratchcard))) {
            return R.layout.fragment_direct_call_incoming_banner_screen;
        } else {
            return R.layout.fragment_direct_call_incoming_banner_screen;
        }
    }

    private void resetTemplateSettings() {
        if(DataStore.getInstance()!=null)
        DataStore.getInstance().setScratchCardScratched(false);
    }

    private void setScratchVisibility(boolean isScratched) {
        if (isScratched) {
            scratchView.setVisibility(View.INVISIBLE);
        } else {
            scratchView.setVisibility(View.VISIBLE);
        }
    }

    private void onCallPicked() {
        int PERMISSION_ALL = 1;
        try {
            List<String> permissionList = new ArrayList<>();
            if (DataStore.getInstance().getReadPhoneStatePermission()) {
                permissionList.add(Manifest.permission.READ_PHONE_STATE);
            }
            permissionList.add(android.Manifest.permission.RECORD_AUDIO);

            String[] permissions = new String[permissionList.size()];
            permissions = permissionList.toArray(permissions);

            if (!Utils.getInstance().hasPermissions(getContext(), permissions)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(permissions, PERMISSION_ALL);
                }
            } else {
                DataStore.getInstance().getIncomingTimer().cancel();

                freeResources();

                callAccepted();
                //removing incoming notification
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                } else {
                    Utils.getInstance().dismissCallNotificationService(getActivity());
                }

                updateViewFragment(getActivity());
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Problem occured while picking up the call: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }


    private void setBranding() {
        Map<String, View> brandingViewParams = new HashMap<>();

        brandingViewParams.put(CallScreenUtil.BrandingViews.ivLogo, ivLogo);
        brandingViewParams.put(CallScreenUtil.BrandingViews.rootViewBackground, rootViewBackground);
        brandingViewParams.put(CallScreenUtil.BrandingViews.tvContext, tvContext);
        brandingViewParams.put(CallScreenUtil.BrandingViews.tvCallScreenLabel, tvCallScreenLabel);
        brandingViewParams.put(CallScreenUtil.BrandingViews.tvPoweredBy, tvPoweredBy);

        CallScreenUtil.getInstance().setBranding(getContext(), brandingViewParams, CallScreenUtil.DCCallScreenType.INCOMING);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1) {
            try {
                // If request is cancelled, the result arrays are empty.
                for (int index = 0; index < permissions.length; index++) {
                    String permission = permissions[index];
                    if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
                        if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                            try {
                                DataStore.getInstance().getIncomingTimer().cancel();

                                freeResources();

                                callAccepted();
                                //removing incoming notification
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                    callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                                } else {
                                    Utils.getInstance().dismissCallNotificationService(getActivity());
                                }

                                updateViewFragment(getActivity());

                            } catch (Exception e) {
                                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error occured post microphone permission is granted: " + e.getLocalizedMessage());
                                e.printStackTrace();
                            }
                        } else {
                            // permission denied, boo! Disabling the functionality that depends on this permission.
                            if (grantResults[index] == PackageManager.PERMISSION_DENIED
                                    && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                                //user opted never ask again, so storing it into the prefs
                                if (getActivity() != null && getActivity().getApplicationContext() != null) {
                                    StorageHelper.putBoolean(getActivity().getApplicationContext(), KEY_NEVER_ASK_AGAIN_PERMISSION, true);
                                }
                            }

                            callDeclined(CallStatus.DeclineReason.MICROPHONE_PERMISSION_NOT_GRANTED);
                        }
                    }
                }
            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in microphone permission handling: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            String callId = FcmUtil.getInstance(getContext()).getExpiredCallId();
            if (callId != null && callId.equals(callDetails.getString(getString(R.string.scall)))) {
                onCancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * called when the user has answered the incoming call.
     */
    public void callAccepted() {
        try {
            //if incoming call is routed via FCM so it can be a chance that callee answered the call before getting authenticate that time
            // role of FcmCacheManger comes into play
            if (!SocketIOManager.isSocketConnected()) {
                FcmCacheManger.getInstance().setAnswered(true);
            }
            String id = callDetails.getJSONObject("from").getString(getString(R.string.id));
            String accountId = callDetails.getString(KEY_ACCOUNT_ID);
            String call = callDetails.getString(getString(R.string.scall));
            String sid = callDetails.has(getString(R.string.sid)) ? callDetails.getString(getString(R.string.sid)) : "";

            JSONObject data = new JSONObject();
            data.put("responseSid", id + "_" + accountId);
            data.put("callId", call);
            data.put(getString(R.string.sid), sid);
            if (SocketIOManager.getSocket() != null)
                SocketIOManager.getSocket().emit(getString(R.string.s_answer), data, new Ack() {
                    @Override
                    public void call(Object... args) {
                        //no-op
                    }
                });
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in handling when call is answered: " + e.getLocalizedMessage());
            e.printStackTrace();
        }

    }

    /**
     * called when current user has declined the incoming call
     * @param declineReason
     */
    public void callDeclined(CallStatus.DeclineReason declineReason) {
        try {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
            } else {
                Utils.getInstance().dismissCallNotificationService(getActivity());
            }

            DataStore dataStore = DataStore.getInstance();
            String id = callDetails.getJSONObject("from").getString(getString(R.string.id));
            String accountId = callDetails.getString(KEY_ACCOUNT_ID);
            String callId = callDetails.getString(getString(R.string.scall));
            String sid = callDetails.has(getString(R.string.sid)) ? callDetails.getString(getString(R.string.sid)) : "";

            JSONObject data = new JSONObject();
            data.put("responseSid", id + "_" + accountId);
            data.put("callId", callId);
            data.put(getString(R.string.sid), sid);
            if (declineReason != null) {
                data.put(KEY_DECLINE_REASON, declineReason);
                data.put(KEY_DECLINE_REASON_CODE, REASON_CODE_MICROPHONE_PERMISSION_NOT_GRANTED);
            }

            if (SocketIOManager.getSocket() != null && SocketIOManager.isSocketConnected()) {
                dataStore.getSocket().emit(getString(R.string.s_decline), data, (Ack) args -> {
                    //Raising the system event for incoming call
                    DCSystemEventInfo dcSystemEventInfo = new DCSystemEventInfo.Builder(callId, callContext)
                            .setCallStatus(CallStatus.CALL_DECLINED)
                            .setDeclineReason(declineReason)
                            .build();

                    DirectCallAPI.getInstance().pushDCSystemEvent(CTSystemEvent.DC_END, dcSystemEventInfo);
                });
            } else {
                //if incoming call is routed via FCM so it can be a chance that callee answered the call before getting authenticate that time
                // role of FcmCacheManger comes into play
                FcmCacheManger.getInstance().setRejected(true);
                if (declineReason != null) {
                    FcmCacheManger.getInstance().setRejectedReason(declineReason);
                    FcmCacheManger.getInstance().setRejectedReasonCode(401);
                }
            }

            freeResources();
            closeScreen();
            reset();

        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in handling when call is declined: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * called when opposite party(i.e. caller) of the call cancelled the call
     */
    @Override
    public void onCancel() {
        try {

            freeResources();
            closeScreen();
            reset();

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
            } else {
                Utils.getInstance().dismissCallNotificationService(getActivity());
            }

            if (fromCuID != null)
                callNotificationHandler.showCallNotification(
                        new JSONObject().
                                put("context", callContext).
                                put("fromCuid", fromCuID).
                                put("toCuid", toCuID),
                        NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);

        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error in handling when call is cancelled from Initiator's end: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        instance = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (callNotificationHandler.getCallNotificationStatus() != null &&
                        callNotificationHandler.getCallNotificationStatus().equals(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL)
                        && callContext != null)
                    callNotificationHandler.rebuildNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while updating the details on call-notification: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void closeScreen() {
        if (getActivity() != null) {
            getActivity().finishAndRemoveTask();
        }
        if( DataStore.getInstance().getIncomingTimer() != null)
            DataStore.getInstance().getIncomingTimer().cancel();
    }

    private void reset() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> DataStore.getInstance().setClientbusyOnVoIP(false), 1000);
    }

    private void freeResources() {
        Utils.getInstance().stopAudio(getActivity());
        Utils.getInstance().releaseAudio(getActivity());
    }

    private void updateViewFragment(FragmentActivity activity){
        if(activity != null && callDetails != null){
            DirectOngoingCallFragment ongoingCallFragment = new DirectOngoingCallFragment();
            Bundle args = new Bundle();
            args.putString(getString(R.string.callDetails), callDetails.toString());
            ongoingCallFragment.setArguments(args);
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, ongoingCallFragment);
            ft.addToBackStack(null);
            ft.remove(DirectIncomingCallFragment.this);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onActionClick(String action) {
        if(action != null)
        switch (action) {
            case "Answer":
                onAcceptClick();
                break;
            case "Decline":
                callDeclined(null);
                break;
        }
    }

    private void onAcceptClick() {
        if (templateType.equals(getString(R.string.template_pinview))){
            if(directCallPinView !=null)
                directCallPinView.setVisibility(View.VISIBLE);
            if(tvPinLabel!=null)
                tvPinLabel.setVisibility(View.VISIBLE);
        }else if(templateType.equals(getString(R.string.template_video))){
            if(videoView!=null){
                videoView.stopPlayback();
                videoView.setVisibility(View.GONE);
            }
            onCallPicked();
        }
        else {
            onCallPicked();
        }
    }
}

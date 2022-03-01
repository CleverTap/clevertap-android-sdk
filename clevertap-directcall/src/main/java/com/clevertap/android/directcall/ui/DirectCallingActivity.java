package com.clevertap.android.directcall.ui;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.ui.DirectIncomingCallFragment.afChangeListener;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.clevertap.android.directcall.Constants;
import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.recievers.CallNotificationActionReceiver;
import com.clevertap.android.directcall.utils.NotificationHandler.CallNotificationHandler;
import com.clevertap.android.directcall.utils.Utils;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;

public class DirectCallingActivity extends AppCompatActivity {
    static AudioManager audioManager;
    private CallNotificationHandler callNotificationHandler;
    private Timer invalidLaunchDetectScheduler;
    private Boolean previousSpeakerState = false;

    private static DirectCallingActivity instance;

    public static DirectCallingActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_call_screen);

        instance = this;

        Intent intent = getIntent();
        if(intent != null) {
            String screenType = intent.hasExtra(getString(R.string.screen)) ? intent.getStringExtra(getString(R.string.screen)) : null;
            if(screenType != null) {
                DataStore.getInstance().setCallDirection(screenType);
                startInvalidLaunchDetectTimer(screenType);
            }

            setup();

            try {
                String callData = getIntent().getStringExtra(getString(R.string.callDetails));
                String callContext = new JSONObject(callData).getString(getString(R.string.scontext));
                String sid = getIntent().hasExtra(getString(R.string.sid)) ? getIntent().getStringExtra(getString(R.string.sid)) : "";

                DataStore.getInstance().setCallContext(callContext);
                DataStore.getInstance().setAudioManager(audioManager);
                DataStore.getInstance().setOutgoingActivity(DirectCallingActivity.this);

                Bundle args = new Bundle();
                args.putString(getString(R.string.callDetails), callData);

                if (screenType != null) {
                    Fragment fragment;
                    if (screenType.equals(getString(R.string.outgoing))) {
                        fragment = new DirectOutgoingCallFragment();

                        callNotificationHandler.showCallNotification(new JSONObject().put("context", callContext),
                                CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

                        setBuiltInSpeaker();

                    } else if (screenType.equals(getString(R.string.incoming))) {
                        fragment = new DirectIncomingCallFragment();
                        args.putString(getString(R.string.sid), sid);

                        if (intent.hasExtra(getString(R.string.call_answer))
                                && intent.getAction().equals(getString(R.string.call_answer))) {
                            //Case: when User answered the call via call-notification's action button
                            args.putString(getString(R.string.call_answer), getString(R.string.call_answer));
                        }
                    } else {
                        fragment = new DirectOngoingCallFragment();

                        callNotificationHandler.showCallNotification(new JSONObject().put("context", callContext),
                                CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

                        setBuiltInSpeaker();

                        String activityLaunchReason = intent.hasExtra(Constants.ACTIVITY_RELAUNCH_REASON) ?
                                intent.getStringExtra(Constants.ACTIVITY_RELAUNCH_REASON) : null;
                        if (activityLaunchReason != null &&
                                activityLaunchReason.equals(getString(R.string.microphone_permission_not_granted))) {
                            args.putString(Constants.ACTIVITY_RELAUNCH_REASON, getString(R.string.microphone_permission_not_granted));
                        }
                    }

                    replaceFragment(fragment, args);
                }
            } catch (Exception e) {
                e.printStackTrace();
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX,
                        "Exception while looking into the call screen configuration: " + e.getLocalizedMessage()
                );
            }
        }
    }

    public void replaceFragment(Fragment fragment, Bundle args) {
        try {
            fragment.setArguments(args);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, fragment);
            ft.commit();
        } catch (Exception e) {
            e.printStackTrace();
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX,
                    "Exception while launching the call screen: " + e.getLocalizedMessage()
            );
        }
    }

    private void setBuiltInSpeaker() {
        if (audioManager.isSpeakerphoneOn()) {
            audioManager.setSpeakerphoneOn(false);
        }
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    private void setup() {
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        previousSpeakerState = audioManager.isSpeakerphoneOn();

        callNotificationHandler = CallNotificationHandler.getInstance(this);

    }

    private void startInvalidLaunchDetectTimer(String screenType) {
        invalidLaunchDetectScheduler = new Timer();
        if (screenType != null && screenType.equals(getString(R.string.incoming))) {
            invalidLaunchDetectScheduler.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!DataStore.getInstance().isClientbusyOnVoIP()) {
                        //client is not busy on any VoIP call it means opening this callingActivity is not valid.
                        finish();
                    }
                }
            }, 0, 100);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            instance = null;

            reset();
            freeResources();

        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while finishing the DC calling screen: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void freeResources() {
        Vibrator vib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        vib.cancel();

        if (invalidLaunchDetectScheduler != null) {
            invalidLaunchDetectScheduler.cancel();
        }

        releaseAudioResource();

        callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
        callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            Utils.getInstance().dismissCallNotificationService(getApplicationContext());
        } else {
            callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
        }
    }

    private void releaseAudioResource() {
        Utils.getInstance().stopAudio(getApplicationContext());

        if (audioManager != null) {
            audioManager.abandonAudioFocus(afChangeListener);
        }
    }

    public void reset() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (CallNotificationActionReceiver.isAnswerClickEnabled) {
                CallNotificationActionReceiver.isAnswerClickEnabled = false;
            }
        }

        //Free VoIP client from busy state
        DataStore.getInstance().setClientbusyOnVoIP(false);

        if (audioManager != null) {
            if (audioManager.getMode() != AudioManager.MODE_NORMAL) {
                audioManager.setMode(AudioManager.MODE_NORMAL);
            }
            if (audioManager.isSpeakerphoneOn() == previousSpeakerState) {
                audioManager.setSpeakerphoneOn(previousSpeakerState);
            }
        }

        DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Cleared the call details cache");
    }
}



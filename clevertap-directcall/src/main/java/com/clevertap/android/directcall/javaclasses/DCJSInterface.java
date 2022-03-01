package com.clevertap.android.directcall.javaclasses;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.webkit.JavascriptInterface;

import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.interfaces.CallType;
import com.clevertap.android.directcall.ui.DirectOngoingCallFragment;
import com.clevertap.android.directcall.utils.CustomHandler;

import org.json.JSONObject;

public class DCJSInterface implements AudioManager.OnAudioFocusChangeListener {
    private Context context;
    private AudioManager audioManager;
    private AudioFocusRequest mAFRequest;
    AudioManager.OnAudioFocusChangeListener afChangeListener;
    CustomHandler customHandler = CustomHandler.getInstance();

    public DCJSInterface(Context con, AudioManager audioManager) {
        this.context = con;
        this.audioManager = audioManager;
    }

    @JavascriptInterface
    public String getCalldetail() {
        String data = "";
        try {
            DataStore dataStore = DataStore.getInstance();
            data = dataStore.getCallData();
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception in fetching the callDetails on SIP client: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return data;
    }

    public void setSecretToCallDetail(String secret) {
        try {
            DataStore dataStore = DataStore.getInstance();
            String callData = dataStore.getCallData();
            JSONObject jsonObject = new JSONObject(callData);
            jsonObject.put("secret", secret);
            dataStore.setCallData(jsonObject.toString());
        } catch (Exception e) {
            //no-op
        }
    }

    @JavascriptInterface
    public void callVoip() {
        try {
            CallType callType = DataStore.getInstance().getCallType();
            callType.callVoip();
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Problem occured while sending confirmation of SIP registration: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * called when the call is ended. This function is called from the JS.
     */
    @JavascriptInterface
    public void endCall() {
        try {
            //raising system event
            DirectOngoingCallFragment.getInstance().raiseDcEndSystemEvent();

            audioManager.setSpeakerphoneOn(false);
            releaseAudio();
            if (DataStore.getInstance().getOutgoingCallResponse() != null) {
                DirectCallAPI.sdkReady = true;
                customHandler.sendCallAnnotation(DataStore.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, null, VoIPCallStatus.CALL_OVER);
            }
            DataStore.getInstance().setClientbusyOnVoIP(false);
            ((Activity) context).finishAndRemoveTask();
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Problem occured while ending the call of SIP channel: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * called when the audio is to be started. This function is called from the JS.
     */
    @JavascriptInterface
    public void startAudio()  {
        try {
            audioManager.setSpeakerphoneOn(false);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                this.mAFRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAcceptsDelayedFocusGain(false)
                        .setOnAudioFocusChangeListener(this)
                        .setWillPauseWhenDucked(true)
                        .build();
                int res = this.audioManager.requestAudioFocus(mAFRequest);
                final Object mFocusLock = new Object();
                synchronized (mFocusLock) {
                    if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                    //Log.d("DirectCall: ", "Failed to get audio focus");
                    } else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        audioManager.setSpeakerphoneOn(false);
                    //Log.d("DirectCall: ", "Gained Audio Focus");
                    } else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
                        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        audioManager.setSpeakerphoneOn(false);
                    //Log.d("DirectCall: ", "Delay in getting audio focus");
                    }
                }
            } else {
                int result = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    audioManager.setSpeakerphoneOn(false);
                }
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Problem occured while getting the audioFocus " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void speaker() {
        if(audioManager != null){
            audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
        }
    }

    /**
     * called when the audio is released by the webview. This is called from the JS.
     */
    @JavascriptInterface
    public void releaseAudio() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(this.mAFRequest);
                this.audioManager.setMode(AudioManager.MODE_NORMAL);
            } else {
                audioManager.abandonAudioFocus(afChangeListener);
                this.audioManager.setMode(AudioManager.MODE_NORMAL);
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Problem occured while releasing the audio focus: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        //no-op
    }
}

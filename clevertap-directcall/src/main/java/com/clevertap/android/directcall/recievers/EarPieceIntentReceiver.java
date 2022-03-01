package com.clevertap.android.directcall.recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.clevertap.android.directcall.ui.DirectOngoingCallFragment;
import com.clevertap.android.directcall.javaclasses.DataStore;

public class EarPieceIntentReceiver extends BroadcastReceiver {
    AudioManager audioManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if ((Intent.ACTION_HEADSET_PLUG).equals(intent.getAction())) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        if(DirectOngoingCallFragment.getInstance() != null &&
                                !DirectOngoingCallFragment.getInstance().getIsSpeakerOn()){
                            //this handling is required b/c ejecting the earpiece sets the speaker as output device for phone.
                            audioManager = DataStore.getInstance().getAudioManager();
                            if(audioManager != null && audioManager.isSpeakerphoneOn()){
                                audioManager.setSpeakerphoneOn(false);
                            }
                        }
                        break;
                    case 1:
                        if(DirectOngoingCallFragment.getInstance() != null)
                            DirectOngoingCallFragment.getInstance().setSpeakerOff();
                        break;
                }
            }

        }catch (Exception e){
            //no-op
        }
    }
}
package com.clevertap.android.directcall.recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.clevertap.android.directcall.ui.DirectOngoingCallFragment;
import com.clevertap.android.directcall.javaclasses.DataStore;

public class PhoneStateReceiver extends BroadcastReceiver {

    private static Boolean isAnswered = false;
    public void onReceive(final Context context, Intent intent) {
        try {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            /*if(state.equals(TelephonyManager.EXTRA_STATE_RINGING)){
            }*/
            if ((state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))){
                if(!isAnswered){
                    DataStore.getInstance().setClientbusyOnPstn(true);
                    isAnswered = true;
                    if(DirectOngoingCallFragment.getInstance().getIsFragmentVisible()){
                        DirectOngoingCallFragment.getInstance().switchHoldState();
                    }
                }
            }
            if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)){
                if(isAnswered){
                    isAnswered = false;
                    DataStore.getInstance().setClientbusyOnPstn(false);
                    if(DirectOngoingCallFragment.getInstance().getIsFragmentVisible()){
                        DirectOngoingCallFragment.getInstance().switchHoldState();
                    }
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
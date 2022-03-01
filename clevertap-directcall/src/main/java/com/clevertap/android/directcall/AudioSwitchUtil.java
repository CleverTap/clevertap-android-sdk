package com.clevertap.android.directcall;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;

public class AudioSwitchUtil {

    private AudioManager audioManager;
    public static AudioSwitchUtil mInstance;

    private AudioSwitchUtil(AudioManager manager){
        this.audioManager = manager;
    }

    public static AudioSwitchUtil getInstance(AudioManager manager){
        if(mInstance == null) {
            mInstance = new AudioSwitchUtil(manager);
        }
        return mInstance;
    }


    public void setupAudioOutputDevice(boolean shouldEnableExternalSpeaker, boolean isSpeakerOn) {
        if (shouldEnableExternalSpeaker) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            audioManager.setSpeakerphoneOn(isSpeakerOn);
            /*if (isBlueToothConnected) {
                // 1. case - bluetooth device
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.startBluetoothSco();
                audioManager.setBluetoothScoOn(true);
            } else {
                // 2. case - wired device
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
                audioManager.setSpeakerphoneOn(false);
            }*/
        } else {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
            audioManager.setSpeakerphoneOn(isSpeakerOn);
        }
    }

    public boolean isWiredHeadphonePlugged() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo deviceInfo : audioDevices) {
                if (deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                    return true;
                }
            }
        } else {
            return audioManager.isWiredHeadsetOn();
        }
        return false;
    }

    public boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }
}

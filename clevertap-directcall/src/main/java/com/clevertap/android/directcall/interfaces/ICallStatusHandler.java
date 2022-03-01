package com.clevertap.android.directcall.interfaces;

public interface ICallStatusHandler {
    void onAnswer();
    void onDecline();
    void onMiss();
    void onIosApf(String data);

     interface incomingCallStatus {
        void onCancel();
    }
}

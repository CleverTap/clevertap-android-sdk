package com.clevertap.android.directcall.utils;

public class PinViewObservable {

    PinViewTextObserver pinviewTextObserver;

    private static PinViewObservable instance = null;
    public static PinViewObservable getInstance() {
        if (instance == null) {
            instance = new PinViewObservable();
        }
        return instance;
    }

    private PinViewObservable() {
    }

    public PinViewTextObserver getPinviewTextObserver() {
        return pinviewTextObserver;
    }

    public void setPinviewTextObserver(PinViewTextObserver pinviewTextObserver) {
        this.pinviewTextObserver = pinviewTextObserver;
    }

    public interface PinViewTextObserver {
        void onPinTextCompleted(String pin, PinTextVerificationHandler pinTextVerificationHandler);
    }

    public interface PinTextVerificationHandler {
        void onSuccess();

        void onFailure();
    }
}

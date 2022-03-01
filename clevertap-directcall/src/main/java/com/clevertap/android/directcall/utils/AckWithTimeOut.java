package com.clevertap.android.directcall.utils;

import static com.clevertap.android.directcall.Constants.SOCKET_NO_ACK;

import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Ack;

public class AckWithTimeOut implements Ack {
    private Timer timer;
    private long timeOut = 0;
    private boolean called = false;

    public AckWithTimeOut(long timeout_after) {
        if (timeout_after <= 0)
            return;
        this.timeOut = timeout_after;
        startTimer();
    }

    public void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callback(SOCKET_NO_ACK);
            }
        }, timeOut);
    }

    public void resetTimer() {
        if (timer != null) {
            timer.cancel();
            startTimer();
        }
    }

    public void cancelTimer() {
        if (timer != null)
            timer.cancel();
    }

    void callback(Object... args) {
        if (called) return;
        called = true;
        cancelTimer();
        call(args);
    }

    @Override
    public void call(Object... args) {

    }
}
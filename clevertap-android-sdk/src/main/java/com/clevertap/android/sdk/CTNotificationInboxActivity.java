package com.clevertap.android.sdk;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import java.lang.ref.WeakReference;

public class CTNotificationInboxActivity extends FragmentActivity {
    interface InboxActivityListener{
        void messageDidShow();
        void messageDidClick();
    }

    private CTInboxMessage inboxMessage;
    private CleverTapInstanceConfig config;
    private WeakReference<InboxActivityListener> listenerWeakReference;

    void setListener(InboxActivityListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    InboxActivityListener getListener() {
        InboxActivityListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger().verbose(config.getAccountId(),"InboxActivityListener is null for notification inbox " );
        }
        return listener;
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inbox_activity);

    }

}

package com.clevertap.android.sdk;

import android.view.View;
import android.widget.Button;

class CTInboxButtonClickListener implements View.OnClickListener {

    private int position;
    private CTInboxMessage inboxMessage;
    private Button button;

    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage){
        this.position = position;
        this.inboxMessage = inboxMessage;
    }

    CTInboxButtonClickListener(int position, CTInboxMessage inboxMessage, Button button){
        this.position = position;
        this.inboxMessage = inboxMessage;
        this.button = button;
    }

    @Override
    public void onClick(View v) {
        //TODO handle click of buttons and message
    }
}

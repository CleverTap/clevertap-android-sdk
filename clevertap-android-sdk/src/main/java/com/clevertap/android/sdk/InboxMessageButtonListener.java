package com.clevertap.android.sdk;

import java.util.HashMap;

public interface InboxMessageButtonListener {

    /**
     * callback to transfer payload when inbox button is clicked
     */
    void onInboxButtonClick(HashMap<String, String> payload);
}
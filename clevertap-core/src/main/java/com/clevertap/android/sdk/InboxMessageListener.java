package com.clevertap.android.sdk;

import com.clevertap.android.sdk.inbox.CTInboxMessage;

public interface InboxMessageListener {
    /**
     * callback to transfer payload when inbox button is clicked
     */
    void onInboxItemClicked(CTInboxMessage message);
}

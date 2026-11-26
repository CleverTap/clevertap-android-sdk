package com.clevertap.android.sdk

import java.util.HashMap

public fun interface InboxMessageButtonListener {

    /**
     * callback to transfer payload when inbox button is clicked
     */
    public fun onInboxButtonClick(payload: HashMap<String, String>?)
}

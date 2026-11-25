package com.clevertap.android.sdk

import java.util.HashMap

fun interface InboxMessageButtonListener {

    /**
     * callback to transfer payload when inbox button is clicked
     */
    fun onInboxButtonClick(payload: HashMap<String, String>?)
}

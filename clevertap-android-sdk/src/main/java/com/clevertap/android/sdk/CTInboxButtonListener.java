package com.clevertap.android.sdk;

import java.util.HashMap;

public interface CTInboxButtonListener {

    /**
     * callback to transfer payload when inbox button is clicked
     */
    void onClick(HashMap<String, String> payload);
}
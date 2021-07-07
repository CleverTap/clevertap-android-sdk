package com.clevertap.android.sdk;

import org.json.JSONObject;

public interface SyncListener {

    void profileDataUpdated(JSONObject updates);

    /**
     * Notifies Listener when deviceID is generated successfully.
     *
     * @param CleverTapID Identifier, can be Custom CleverTapID, Google AD ID or SDK generated CleverTapID
     *
     *                    <p><br><span style="color:red;background:#ffcc99" >&#9888; Callback will be received on main
     *                    thread, so avoid doing any lengthy operations from this callback </span></p>
     */
    void profileDidInitialize(String CleverTapID);
}

package com.clevertap.android.sdk;

import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener;
import org.json.JSONObject;

/**
 * <p style="color:red;font-size: 25px;margin-left:10px">&#9760;</p>
 * <b><span style="color:#4d2e00;background:#ffcc99" >Deprecated as of version <code>4.2.0</code> and will be
 * removed in future versions</span>
 * </b><br>
 * <code>Use {@link CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener)} instead</code>
 */
@Deprecated
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

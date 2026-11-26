package com.clevertap.android.sdk

import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener
import org.json.JSONObject

/**
 * <p style="color:red;font-size: 25px;margin-left:10px">&#9760;</p>
 * <b><span style="color:#4d2e00;background:#ffcc99" >Deprecated as of version <code>4.2.0</code> and will be
 * removed in future versions</span>
 * </b><br>
 * <code>Use [CleverTapAPI.getCleverTapID] with an [OnInitCleverTapIDListener] instead</code>
 */
@Deprecated("Deprecated as of version 4.2.0 and will be removed in future versions. Use CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener) instead")
public interface SyncListener {

    public fun profileDataUpdated(updates: JSONObject?)

    /**
     * Notifies Listener when deviceID is generated successfully.
     *
     * @param CleverTapID Identifier, can be Custom CleverTapID, Google AD ID or SDK generated CleverTapID
     *
     *                    <p><br><span style="color:red;background:#ffcc99" >&#9888; Callback will be received on main
     *                    thread, so avoid doing any lengthy operations from this callback </span></p>
     */
    public fun profileDidInitialize(CleverTapID: String?)
}

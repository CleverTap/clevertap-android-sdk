package com.clevertap.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener;

import org.json.JSONObject;

/**
 * <p style="color:red;font-size: 25px;margin-left:10px">&#9760;</p>
 * <b><span style="color:#4d2e00;background:#ffcc99" >Deprecated as of version <code>4.2.0</code> and will be
 * removed in future versions</span>
 * </b><br>
 * <code>Use {@link CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener)} instead</code>
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameters carry
 * androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin override
 * must declare the same nullability. Do not convert this file to Kotlin.
 *
 * @deprecated Deprecated as of version 4.2.0 and will be removed in future versions. Use
 * {@link CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener)} instead
 */
@Deprecated
public interface SyncListener {

    /**
     * Intended to report profile changes. The SDK no longer calls this method; it is kept only so
     * existing implementations still compile.
     *
     * @param updates the changed profile properties
     */
    void profileDataUpdated(@Nullable JSONObject updates);

    /**
     * Notifies Listener when deviceID is generated successfully.
     *
     * @param CleverTapID Identifier, can be Custom CleverTapID, Google AD ID or SDK generated CleverTapID
     *
     *                    <p><br><span style="color:red;background:#ffcc99" >&#9888; Callback will be received on main
     *                    thread, so avoid doing any lengthy operations from this callback </span></p>
     */
    void profileDidInitialize(@NonNull String CleverTapID);
}

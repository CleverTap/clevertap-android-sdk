package com.clevertap.android.sdk.interfaces;

/**
 * Notifies about CleverTapID generation through callback
 */
public interface OnInitCleverTapIDListener {

    /**
     * Callback to hand over generated cleverTapID to listener
     *
     * @param cleverTapID Identifier, can be Custom CleverTapID, Google AD ID or SDK generated CleverTapID
     *                    <p><br><span style="color:red;background:#ffcc99" >&#9888; Callback will be received on main
     *                    thread, so avoid doing any lengthy operations from this callback </span></p>
     */
    void onInitCleverTapID(String cleverTapID);
}

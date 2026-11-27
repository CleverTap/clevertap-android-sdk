package com.clevertap.android.sdk.interfaces;

/**
 * Notifies about CleverTapID generation through callback
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees the parameter as a platform type, so wrapper SDKs
 * and apps can override it with a nullable or non-null type. Do not convert this file to Kotlin.
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

package com.clevertap.android.sdk.interfaces;

import androidx.annotation.NonNull;

/**
 * Notifies about CleverTapID generation through callback
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameter
 * carries androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin
 * override must declare the same nullability. Do not convert this file to Kotlin.
 */
public interface OnInitCleverTapIDListener {

    /**
     * Callback to hand over generated cleverTapID to listener
     *
     * @param cleverTapID Identifier, can be Custom CleverTapID, Google AD ID or SDK generated CleverTapID
     *                    <p><br><span style="color:red;background:#ffcc99" >&#9888; Callback will be received on main
     *                    thread, so avoid doing any lengthy operations from this callback </span></p>
     */
    void onInitCleverTapID(@NonNull String cleverTapID);
}

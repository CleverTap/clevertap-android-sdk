package com.clevertap.android.sdk.interfaces

/**
 * Notifies about CleverTapID generation through callback
 */
fun interface OnInitCleverTapIDListener {

    /**
     * Callback to hand over generated cleverTapID to listener
     *
     * @param cleverTapID Identifier, can be Custom CleverTapID, Google AD ID or SDK generated CleverTapID
     *
     * &#9888; Callback will be received on main thread, so avoid doing any lengthy operations from this
     * callback
     */
    fun onInitCleverTapID(cleverTapID: String?)
}

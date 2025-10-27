package com.clevertap.android.sdk.features.callbacks

import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener
import com.clevertap.android.sdk.interfaces.SCDomainListener

internal class CoreClientCallbacks {

    private val onInitCleverTapIDListeners: MutableList<OnInitCleverTapIDListener> = mutableListOf()

    var scDomainListener: SCDomainListener? = null

    fun addOnInitCleverTapIDListener(onInitCleverTapIDListener: OnInitCleverTapIDListener) {
        synchronized(onInitCleverTapIDListeners) {
            onInitCleverTapIDListeners.add(onInitCleverTapIDListener)
        }
    }

    fun removeOnInitCleverTapIDListener(listener: OnInitCleverTapIDListener) {
        synchronized(onInitCleverTapIDListeners) {
            onInitCleverTapIDListeners.remove(listener)
        }
    }

    fun notifyCleverTapIDChanged(id: String) {
        synchronized(onInitCleverTapIDListeners) {
            for (listener in onInitCleverTapIDListeners) {
                listener.onInitCleverTapID(id)
            }
        }
    }
}
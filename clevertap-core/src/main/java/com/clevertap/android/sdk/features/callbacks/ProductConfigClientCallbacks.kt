package com.clevertap.android.sdk.features.callbacks

import com.clevertap.android.sdk.product_config.CTProductConfigListener
import java.lang.ref.WeakReference

internal class ProductConfigClientCallbacks {
    private var productConfigListener: WeakReference<CTProductConfigListener> = WeakReference(null)
    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("")
    fun getProductConfigListener(): CTProductConfigListener? {
        return productConfigListener.get()
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("")
    fun setProductConfigListener(productConfigListener: CTProductConfigListener?) {
        if (productConfigListener != null) {
            this.productConfigListener = WeakReference(productConfigListener)
        }
    }
}
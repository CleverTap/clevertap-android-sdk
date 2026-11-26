package com.clevertap.android.sdk.product_config

/**
 * Interface definition for a callback to be invoked when Product Config APIs are invoked
 */
@Deprecated("Product Config has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
public interface CTProductConfigListener {

    /**
     * Receives a callback whenever Product Config gets activated.
     */
    @Deprecated(
        "Product Config has been deprecated since v5.0.0 and will be removed in the future versions of this SDK."
    )
    public fun onActivated()

    /**
     * Receives a callback whenever Product Config is fetched.
     */
    @Deprecated(
        "Product Config has been deprecated since v5.0.0 and will be removed in the future versions of this SDK."
    )
    public fun onFetched()

    /**
     * Receives a callback whenever Product Config initialises.
     */
    @Deprecated(
        "Product Config has been deprecated since v5.0.0 and will be removed in the future versions of this SDK."
    )
    public fun onInit()
}

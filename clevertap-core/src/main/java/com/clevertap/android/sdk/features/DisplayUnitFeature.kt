package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.network.ContentFetchManager
import com.clevertap.android.sdk.response.ContentFetchResponse
import com.clevertap.android.sdk.response.DisplayUnitResponse
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Display Unit feature
 * Manages display units and content fetching for display units
 */
internal data class DisplayUnitFeature(
    val contentFetchManager: ContentFetchManager,
    val contentFetchResponse: ContentFetchResponse,
    val displayUnitResponse: DisplayUnitResponse
) : CleverTapFeature, DisplayUnitNotifier {
    var displayUnitListener: WeakReference<DisplayUnitListener> = WeakReference(null)

    var controller: CTDisplayUnitController? = null

    lateinit var coreContract: CoreContract

    fun attachListener(listener: DisplayUnitListener) {
        displayUnitListener = WeakReference(listener)
    }

    override fun notifyDisplayUnitsLoaded(displayUnits: ArrayList<CleverTapDisplayUnit>?) {
        displayUnitListener.get()?.onDisplayUnitsLoaded(displayUnits)
    }

    override fun updateController(controller: CTDisplayUnitController) {
        this.controller = controller
        this.displayUnitResponse.setController(controller)
        this.displayUnitResponse.setDisplayUnitNotifier(this)
    }

    override fun coreContract(coreContract: CoreContract) {
        this.coreContract = coreContract
    }

    override fun handleApiData(
        response: JSONObject,
        stringBody: String,
        context: Context
    ) {
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing Display Unit response"
            )
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing Content Fetch response"
            )
            // process feature flag response
            return
        }
        displayUnitResponse.processResponse(response, stringBody, context)
        contentFetchResponse.processResponse(response, context.packageName, contentFetchManager)
    }

    fun pushDisplayUnitClickedEventForID(unitID: String?) {
        try {
            controller?.getDisplayUnitForID(unitID)?.wzrkFields?.let { data: JSONObject ->
                coreContract.analytics().pushDisplayUnitClickedEvent(data)
            } ?: coreContract.logger().verbose("${Constants.FEATURE_DISPLAY_UNIT} Failed to push Display Unit clicked event")
        } catch (t: Throwable) {
            // We won't get here
            coreContract.config().logger.verbose(
                coreContract.config().accountId,
                 "${Constants.FEATURE_DISPLAY_UNIT} Failed to push Display Unit clicked event $t"
            )
        }
    }

    fun pushDisplayUnitViewedEventForID(unitID: String?) {
        try {
            controller?.getDisplayUnitForID(unitID)?.wzrkFields?.let { data: JSONObject ->
                coreContract.analytics().pushDisplayUnitViewedEvent(data)
            } ?: coreContract.logger().verbose("${Constants.FEATURE_DISPLAY_UNIT} Failed to push Display Unit viewed event")
        } catch (t: Throwable) {
            // We won't get here
            coreContract.config().logger.verbose(
                coreContract.config().accountId,
                "${Constants.FEATURE_DISPLAY_UNIT} Failed to push Display Unit viewed event $t"
            )
        }
    }
}

interface DisplayUnitNotifier {
    fun notifyDisplayUnitsLoaded(displayUnits: ArrayList<CleverTapDisplayUnit>?)

    fun updateController(controller: CTDisplayUnitController)
}

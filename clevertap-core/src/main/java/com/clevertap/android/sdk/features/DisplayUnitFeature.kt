package com.clevertap.android.sdk.features

import android.content.Context
import android.os.Bundle
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.network.ContentFetchManager
import com.clevertap.android.sdk.response.ContentFetchResponse
import com.clevertap.android.sdk.response.DisplayUnitResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Display Unit feature
 * Manages display units and content fetching for display units
 */
internal class DisplayUnitFeature(
    var displayUnitListener: WeakReference<DisplayUnitListener> = WeakReference(null),
    var controller: CTDisplayUnitController? = null
) : CleverTapFeature, DisplayUnitNotifier {

    lateinit var coreContract: CoreContract

    // Lazy-initialized DisplayUnit dependencies (initialized after coreContract is set)
    val contentFetchManager: ContentFetchManager by lazy {
        ContentFetchManager(
            logger = coreContract.logger(),
            ctApiWrapper = coreContract.apiWrapper()
        )
    }

    val contentFetchResponse: ContentFetchResponse by lazy {
        ContentFetchResponse(
            coreContract.config().accountId,
            coreContract.logger()
        )
    }

    val displayUnitResponse: DisplayUnitResponse by lazy {
        DisplayUnitResponse(
            coreContract.config().accountId,
            coreContract.logger()
        ).apply {
            controller?.let { setController(it) }
            setDisplayUnitNotifier(this@DisplayUnitFeature)
        }
    }

    fun attachListener(listener: DisplayUnitListener) {
        displayUnitListener = WeakReference(listener)
    }

    override fun notifyDisplayUnitsLoaded(displayUnits: ArrayList<CleverTapDisplayUnit>?) {
        displayUnitListener.get()?.onDisplayUnitsLoaded(displayUnits)
    }

    override fun updateController(controller: CTDisplayUnitController) {
        this.controller = controller
        // Update displayUnitResponse if already initialized
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
        displayUnitResponse.processResponse(response)
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

    @WorkerThread
    fun handleSendTest(extras: Bundle) {
        val pushJsonPayload = extras.getString(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY)
            ?: run {
                coreContract.logger().verbose("Display unit preview push payload not found in extras bundle.")
                return
            }

        coreContract.logger().verbose("Received Display Unit via push payload: $pushJsonPayload")

        try {
            val testPushObject = JSONObject(pushJsonPayload)

            val displayUnits = JSONArray().apply {
                put(testPushObject)
            }
            val responseJson = JSONObject().apply {
                put(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY, displayUnits)
            }

            handleApiData(responseJson, "", coreContract.context())

        } catch (e: JSONException) {
            coreContract.logger().verbose("Failed to parse display unit preview JSON from payload: $pushJsonPayload", e)
        }
    }
}

interface DisplayUnitNotifier {
    fun notifyDisplayUnitsLoaded(displayUnits: ArrayList<CleverTapDisplayUnit>?)

    fun updateController(controller: CTDisplayUnitController)
}

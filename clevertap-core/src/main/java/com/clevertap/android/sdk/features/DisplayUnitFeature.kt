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
    var controller: CTDisplayUnitController? = null,
    private val packageNameFetcher: (Context) -> String = { context -> context.packageName }
) : CleverTapFeature, DisplayUnitContract {

    lateinit var coreContract: CoreContract

    // Lazy-initialized DisplayUnit dependencies (initialized after coreContract is set)
    val contentFetchManager: ContentFetchManager by lazy {
        ContentFetchManager(
            logger = coreContract.logger(),
            ctApiWrapper = coreContract.apiWrapper(),
            coreContract = coreContract
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
        )
    }

    fun attachListener(listener: DisplayUnitListener) {
        displayUnitListener = WeakReference(listener)
    }

    override fun notifyDisplayUnitsLoaded(displayUnits: ArrayList<CleverTapDisplayUnit>?) {
        displayUnitListener.get()?.onDisplayUnitsLoaded(displayUnits)
    }

    /**
     * Provides a singleton instance of [CTDisplayUnitController].
     *
     * @return The singleton [CTDisplayUnitController] instance.
     */
    override fun provideDisplayUnitController(): CTDisplayUnitController {
        return controller ?: synchronized(this) {
            controller ?: CTDisplayUnitController()
        }.also {
            controller = it
        }
    }

    override fun handleApiData(
        response: JSONObject,
        isFullResponse: Boolean,
        isUserSwitching: Boolean
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
        displayUnitResponse.processResponse(response, this)
        contentFetchResponse.processResponse(
            jsonBody = response,
            packageName = packageNameFetcher(coreContract.context()),
            contentFetchManager = contentFetchManager
        )
    }

    /**
     * Pushes a "Notification Clicked" event to CleverTap for the given display unit ID.
     *
     * This function retrieves the display unit's data using its ID, extracts the
     * necessary fields (`wzrkFields`), and then raises a click event. This is
     * essential for tracking user engagement with specific display units.
     *
     * @param unitID The unique identifier of the display unit that was clicked.
     */
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

    /**
     * Pushes the "Notification Viewed" event for the given display unit ID.
     *
     * This function retrieves the display unit from the controller using the provided `unitID`.
     * If found, it extracts the `wzrk_fields` from the unit and uses them to raise a
     * "Display Unit Viewed" analytics event. This event is crucial for tracking user
     *  engagement with specific display units.
     *
     * @param unitID The unique identifier of the display unit that was viewed. Can be null.
     */
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

    /**
     * Handles the "Send Test" push notification for Display Units.
     * This method is triggered when a test push containing a Display Unit payload is received.
     * It extracts the Display Unit JSON from the notification's extras, wraps it
     * in a standard response format, and then processes it as if it were received
     * from a regular API call. This allows developers to receive Display Unit on a test device.
     *
     * @param extras The Bundle containing the push notification data. Expected to have a
     *               String under the key [Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY].
     */
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

            handleApiData(response = responseJson)

        } catch (e: JSONException) {
            coreContract.logger().verbose("Failed to parse display unit preview JSON from payload: $pushJsonPayload", e)
        }
    }

    /**
     * Resets the Display Units controller cache
     */
    fun reset() {
        if (controller != null) {
            controller!!.reset()
        } else {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                Constants.FEATURE_DISPLAY_UNIT + "Can't reset Display Units, DisplayUnitController is null"
            )
        }
    }
}

interface DisplayUnitContract {
    fun notifyDisplayUnitsLoaded(displayUnits: ArrayList<CleverTapDisplayUnit>?)
    fun provideDisplayUnitController(): CTDisplayUnitController
}

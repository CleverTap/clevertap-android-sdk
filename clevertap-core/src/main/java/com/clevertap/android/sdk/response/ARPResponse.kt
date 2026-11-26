package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.ArpRepo
import com.clevertap.android.sdk.product_config.CTProductConfigController
import com.clevertap.android.sdk.validation.ValidationConfig
import org.json.JSONException
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class ARPResponse(
    private val config: CleverTapInstanceConfig,
    private val validationConfig: ValidationConfig?,
    controllerManager: ControllerManager,
    private val arpRepo: ArpRepo
) : CleverTapResponseDecorator() {

    private val ctProductConfigController: CTProductConfigController? = controllerManager.ctProductConfigController

    private val logger: Logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        // Handle "arp" (additional request parameters)
        try {
            val response = checkNotNull(jsonBody) { "response JSON is null" }
            if (!response.has("arp")) {
                return
            }
            val arp = response.get("arp") as JSONObject
            if (arp.length() > 0) {
                ctProductConfigController?.setArpValue(arp)
                //Handle Discarded events in ARP
                try {
                    processDiscardedEventsList(arp)
                } catch (t: Throwable) {
                    logger.verbose("Error handling discarded events response: ${t.localizedMessage}")
                }
                arpRepo.handleARPUpdate(context, arp)
            }
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "Failed to process ARP", t)
        }
    }

    /**
     * Dashboard has a feature where marketers can discard event. We get that list in the ARP response,
     * SDK then checks if the event is in the discarded list before sending it to LC
     *
     * @param response response from server
     */
    private fun processDiscardedEventsList(response: JSONObject) {
        if (!response.has(Constants.DISCARDED_EVENT_JSON_KEY)) {
            logger.verbose(config.accountId, "ARP doesn't contain the Discarded Events key")
            return
        }

        try {
            val discardedEventsArray = response.getJSONArray(Constants.DISCARDED_EVENT_JSON_KEY)
            val discardedEventsSet = (0 until discardedEventsArray.length())
                .mapTo(HashSet()) { discardedEventsArray.getString(it) }
            if (validationConfig != null) {
                validationConfig.updateDiscardedEventNames(discardedEventsSet)
            } else {
                logger.verbose(config.accountId, "ValidationConfig object is NULL")
            }
        } catch (e: JSONException) {
            logger.verbose(config.accountId, "Error parsing discarded events list${e.localizedMessage}")
        }
    }
}

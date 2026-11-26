package com.clevertap.android.sdk

import android.os.Bundle
import android.webkit.JavascriptInterface
import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.inapp.CTInAppAction
import com.clevertap.android.sdk.inapp.InAppWebInteraction
import java.lang.ref.WeakReference
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * This class helps WebViews to interact with CleverTapAPI via pre-defined methods
 *
 * @constructor Internal constructor used by the SDK's own in-app WebViews. Kotlin `internal` (not
 * mangled, still JVM-public) because [InAppWebInteraction] is an internal type; it was
 * `@RestrictTo(LIBRARY_GROUP)` in Java.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class CTWebInterface @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) internal constructor(
    instance: CleverTapAPI?,
    host: InAppWebInteraction?
) {

    private val cleverTapWr: WeakReference<CleverTapAPI> = WeakReference(instance)

    private val hostWr: WeakReference<InAppWebInteraction> = WeakReference(host)

    constructor(instance: CleverTapAPI?) : this(instance, null) {
        instance?.let { it.coreState.coreMetaData.isWebInterfaceInitializedExternally = true }
    }

    /**
     * Method to be called from WebView Javascript to request permission for notification
     * for Android 13 and above
     */
    @JavascriptInterface
    open fun promptPushPermission(shouldShowFallbackSettings: Boolean) {
        withCleverTap { cleverTapAPI ->
            //Dismisses current IAM and proceeds to call promptForPushPermission()
            dismissInAppNotification()
            cleverTapAPI.promptForPushPermission(shouldShowFallbackSettings)
        }
    }

    /**
     * Method to be called from WebView Javascript to dismiss the InApp notification
     */
    @JavascriptInterface
    open fun dismissInAppNotification() {
        withCleverTap {
            //Dismisses current IAM and proceeds to call promptForPushPermission()
            hostWr.get()?.didDismiss(null)
        }
    }

    /**
     * Method to be called from WebView Javascript to add profile properties in CleverTap
     *
     * @param key   [String] value of profile property key
     * @param value [String] value of profile property value
     */
    @JavascriptInterface
    open fun addMultiValueForKey(key: String?, value: String?) {
        withCleverTap { it.addMultiValueForKey(key, value) }
    }

    /**
     * Method to be called from WebView Javascript to increase the value of a particular property.
     * The key must hold numeric value
     *
     * @param key   [String] value of profile property key
     * @param value [Double] value of increment
     */
    @JavascriptInterface
    open fun incrementValue(key: String?, value: Double) {
        withCleverTap { it.incrementValue(key, value) }
    }

    /**
     * Method to be called from WebView Javascript to decrease the value of a particular property.
     * The key must hold numeric value
     *
     * @param key   [String] value of profile property key
     * @param value [Double] value of decrement
     */
    @JavascriptInterface
    open fun decrementValue(key: String?, value: Double) {
        withCleverTap { it.decrementValue(key, value) }
    }

    /**
     * Method to be called from WebView Javascript to add profile properties in CleverTap
     *
     * @param key    [String] value of profile property key
     * @param values Stringified [JSONArray] of profile property values
     */
    @JavascriptInterface
    open fun addMultiValuesForKey(key: String?, values: String?) {
        withCleverTap { cleverTapAPI ->
            withMultiValues(key, values) { validKey, valueList ->
                cleverTapAPI.addMultiValuesForKey(validKey, valueList)
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to raise Charged event in CleverTap
     *
     * @param chargeDetails Stringified [JSONObject] of charged event details
     *                      Stringified [JSONObject] will be converted to a HashMap<String,Object>
     * @param items         A Stringified [JSONArray] which contains up to 15 JSON Object objects,
     *                      where each JSON Object object describes a particular item purchased
     *                      [JSONArray] of [JSONObject]s will be converted to an [ArrayList] of
     *                      [HashMap]<String,Object>
     */
    @JavascriptInterface
    open fun pushChargedEvent(chargeDetails: String?, items: String?) {
        withCleverTap { cleverTapAPI ->
            if (chargeDetails == null) {
                Logger.v("chargeDetails passed to CTWebInterface is null")
                return
            }
            val chargeDetailsHashMap: HashMap<String, Any> = try {
                Utils.convertJSONObjectToHashMap(JSONObject(chargeDetails))
            } catch (e: JSONException) {
                Logger.v("Unable to parse chargeDetails for Charged Event from WebView ${e.localizedMessage}")
                HashMap()
            }
            if (items == null) {
                return
            }
            val itemsArrayList: ArrayList<HashMap<String, Any>>? = try {
                Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(JSONArray(items))
            } catch (e: JSONException) {
                Logger.v("Unable to parse items for Charged Event from WebView ${e.localizedMessage}")
                null
            }
            cleverTapAPI.pushChargedEvent(chargeDetailsHashMap, itemsArrayList)
        }
    }

    /**
     * Method to be called from WebView Javascript to raise event in CleverTap
     *
     * @param eventName [String] value of event name
     */
    @JavascriptInterface
    open fun pushEvent(eventName: String?) {
        withCleverTap { it.pushEvent(eventName) }
    }

    /**
     * Method to be called from WebView Javascript to raise event with event properties in CleverTap
     *
     * @param eventName    [String] value of event name
     * @param eventActions Stringified [JSONObject] of event properties
     */
    @JavascriptInterface
    open fun pushEvent(eventName: String?, eventActions: String?) {
        withCleverTap { cleverTapAPI ->
            if (eventActions == null) {
                Logger.v("eventActions passed to CTWebInterface is null")
                return
            }
            try {
                cleverTapAPI.pushEvent(eventName, Utils.convertJSONObjectToHashMap(JSONObject(eventActions)))
            } catch (e: JSONException) {
                Logger.v("Unable to parse eventActions from WebView ${e.localizedMessage}")
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to push profile properties in CleverTap
     *
     * @param profile Stringified [JSONObject] of profile properties
     */
    @JavascriptInterface
    open fun pushProfile(profile: String?) {
        withCleverTap { cleverTapAPI ->
            withProfile(profile) { cleverTapAPI.pushProfile(it) }
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties in CleverTap
     *
     * @param key   [String] value of profile property key
     * @param value [String] value of profile property value
     */
    @JavascriptInterface
    open fun removeMultiValueForKey(key: String?, value: String?) {
        withCleverTap { cleverTapAPI ->
            if (key == null) {
                Logger.v("Key passed to CTWebInterface is null")
                return
            }
            if (value == null) {
                Logger.v("Value passed to CTWebInterface is null")
                return
            }
            cleverTapAPI.removeMultiValueForKey(key, value)
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties in CleverTap
     *
     * @param key    [String] value of profile property key
     * @param values Stringified [JSONArray] of profile property values
     */
    @JavascriptInterface
    open fun removeMultiValuesForKey(key: String?, values: String?) {
        withCleverTap { cleverTapAPI ->
            withMultiValues(key, values) { validKey, valueList ->
                cleverTapAPI.removeMultiValuesForKey(validKey, valueList)
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties for given key in CleverTap
     *
     * @param key [String] value of profile property key
     */
    @JavascriptInterface
    open fun removeValueForKey(key: String?) {
        withCleverTap { cleverTapAPI ->
            if (key == null) {
                Logger.v("Key passed to CTWebInterface is null")
                return
            }
            cleverTapAPI.removeValueForKey(key)
        }
    }

    /**
     * Method to be called from WebView Javascript to set profile properties in CleverTap
     *
     * @param key    [String] value of profile property key
     * @param values Stringified [JSONArray] of profile property values
     */
    @JavascriptInterface
    open fun setMultiValueForKey(key: String?, values: String?) {
        withCleverTap { cleverTapAPI ->
            withMultiValues(key, values) { validKey, valueList ->
                cleverTapAPI.setMultiValuesForKey(validKey, valueList)
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to push profile/properties in CleverTap after
     * User Login
     *
     * @param profile Stringified [JSONObject] of profile properties
     */
    @JavascriptInterface
    open fun onUserLogin(profile: String?) {
        withCleverTap { cleverTapAPI ->
            withProfile(profile) { cleverTapAPI.onUserLogin(it) }
        }
    }

    /**
     * Trigger an in-app action (close, open url, button click, custom template, key-value). This method will also
     * push a "Notification Clicked" event for the currently displayed in-app notification. The notification will be
     * dismissed after the action is triggered.
     *
     * @param actionJson   Stringified JSON of the action that is triggered
     * @param callToAction A string that will be stored as param to the "Notification Clicked" event
     * @param buttonId     A string that will be stored as param to the "Notification Clicked" event
     */
    @JavascriptInterface
    open fun triggerInAppAction(actionJson: String?, callToAction: String?, buttonId: String?) {
        if (cleverTapWr.get() == null) {
            Logger.d("CTWebInterface CleverTap Instance is null.")
            return
        }

        val host = hostWr.get()
        if (host == null) {
            Logger.d("CTWebInterface host is null")
            return
        }

        if (actionJson == null) {
            Logger.d("CTWebInterface action JSON is null")
            return
        }

        try {
            val action = CTInAppAction.createFromJson(JSONObject(actionJson))
            if (action == null) {
                Logger.d("CTWebInterface invalid action JSON: $actionJson")
                return
            }

            val actionData = Bundle()
            // Element identity flows only through wzrk_element_id; button_id is no longer emitted.
            buttonId?.let { actionData.putString(Constants.KEY_WZRK_ELEMENT_ID, it) }

            host.triggerAction(action, callToAction, actionData)
        } catch (je: JSONException) {
            Logger.d("CTWebInterface invalid action JSON: $actionJson")
        }
    }

    /**
     * Retrieve the version code of the CleverTap SDK.
     */
    @JavascriptInterface
    open fun getSdkVersion(): Int = BuildConfig.VERSION_CODE

    /** Kotlin property syntax, equivalent to [getSdkVersion]. */
    val sdkVersion: Int
        @JvmSynthetic @JvmName("ktGetSdkVersion") inline get() = getSdkVersion()

    /** Runs [action] with the CleverTap instance, or logs that it is gone. */
    private inline fun withCleverTap(action: (CleverTapAPI) -> Unit) {
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            action(cleverTapAPI)
        }
    }

    /**
     * Validates a JS multi-value call (non-null key, stringified [JSONArray] of values) and passes the
     * parsed values on, logging what was wrong otherwise.
     */
    private inline fun withMultiValues(
        key: String?,
        values: String?,
        action: (key: String, values: ArrayList<String>) -> Unit
    ) {
        if (key == null) {
            Logger.v("Key passed to CTWebInterface is null")
            return
        }
        if (values == null) {
            Logger.v("values passed to CTWebInterface is null")
            return
        }
        try {
            action(key, Utils.convertJSONArrayToArrayList(JSONArray(values)))
        } catch (e: JSONException) {
            Logger.v("Unable to parse values from WebView ${e.localizedMessage}")
        }
    }

    /** Parses a stringified profile [JSONObject] and passes it on, logging what was wrong otherwise. */
    private inline fun withProfile(profile: String?, action: (HashMap<String, Any>) -> Unit) {
        if (profile == null) {
            Logger.v("profile passed to CTWebInterface is null")
            return
        }
        try {
            action(Utils.convertJSONObjectToHashMap(JSONObject(profile)))
        } catch (e: JSONException) {
            Logger.v("Unable to parse profile from WebView ${e.localizedMessage}")
        }
    }
}

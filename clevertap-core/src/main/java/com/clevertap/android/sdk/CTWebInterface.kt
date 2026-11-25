package com.clevertap.android.sdk

import android.os.Bundle
import android.webkit.JavascriptInterface
import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.inapp.CTInAppAction
import com.clevertap.android.sdk.inapp.InAppWebInteraction
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.HashMap
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * This class helps WebViews to interact with CleverTapAPI via pre-defined methods
 */
@Suppress("MemberVisibilityCanBePrivate")
open class CTWebInterface {

    private var cleverTapWr: WeakReference<CleverTapAPI> = WeakReference(null)

    private var hostWr: WeakReference<InAppWebInteraction> = WeakReference(null)

    constructor(instance: CleverTapAPI?) {
        this.cleverTapWr = WeakReference(instance)
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI != null) {
            val coreState: CoreState? = cleverTapAPI.coreState
            if (coreState != null) {
                coreState.coreMetaData.isWebInterfaceInitializedExternally = true
            }
        }
    }

    /**
     * Internal constructor used by the SDK's own in-app WebViews. Kotlin `internal` (not mangled,
     * still JVM-public) because [InAppWebInteraction] is an internal type; it was
     * `@RestrictTo(LIBRARY_GROUP)` in Java.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal constructor(instance: CleverTapAPI?, host: InAppWebInteraction?) {
        this.cleverTapWr = WeakReference(instance)
        this.hostWr = WeakReference(host)
    }

    /**
     * Method to be called from WebView Javascript to request permission for notification
     * for Android 13 and above
     */
    @JavascriptInterface
    open fun promptPushPermission(shouldShowFallbackSettings: Boolean) {
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            //Dismisses current IAM and proceeds to call promptForPushPermission()
            val host = hostWr.get()
            if (host != null) {
                host.didDismiss(null)
            }
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            cleverTapAPI.addMultiValueForKey(key, value)
        }
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            cleverTapAPI.incrementValue(key, value)
        }
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            cleverTapAPI.decrementValue(key, value)
        }
    }

    /**
     * Method to be called from WebView Javascript to add profile properties in CleverTap
     *
     * @param key    [String] value of profile property key
     * @param values Stringified [JSONArray] of profile property values
     */
    @JavascriptInterface
    open fun addMultiValuesForKey(key: String?, values: String?) {
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            if (key == null) {
                Logger.v("Key passed to CTWebInterface is null")
                return
            }
            if (values != null) {
                try {
                    val valuesArray = JSONArray(values)
                    cleverTapAPI.addMultiValuesForKey(key, Utils.convertJSONArrayToArrayList(valuesArray))
                } catch (e: JSONException) {
                    Logger.v("Unable to parse values from WebView " + e.localizedMessage)
                }
            } else {
                Logger.v("values passed to CTWebInterface is null")
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            var chargeDetailsHashMap: HashMap<String, Any> = HashMap()
            if (chargeDetails != null) {
                try {
                    val chargeDetailsObject = JSONObject(chargeDetails)
                    chargeDetailsHashMap = Utils.convertJSONObjectToHashMap(chargeDetailsObject)
                } catch (e: JSONException) {
                    Logger.v(
                        "Unable to parse chargeDetails for Charged Event from WebView " + e
                            .localizedMessage
                    )
                }
            } else {
                Logger.v("chargeDetails passed to CTWebInterface is null")
                return
            }
            var itemsArrayList: ArrayList<HashMap<String, Any>>? = null
            if (items != null) {
                try {
                    val itemsArray = JSONArray(items)
                    itemsArrayList = Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(itemsArray)
                } catch (e: JSONException) {
                    Logger.v("Unable to parse items for Charged Event from WebView " + e.localizedMessage)
                }
            } else {
                return
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            cleverTapAPI.pushEvent(eventName)
        }
    }

    /**
     * Method to be called from WebView Javascript to raise event with event properties in CleverTap
     *
     * @param eventName    [String] value of event name
     * @param eventActions Stringified [JSONObject] of event properties
     */
    @JavascriptInterface
    open fun pushEvent(eventName: String?, eventActions: String?) {
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            if (eventActions != null) {
                try {
                    val eventActionsObject = JSONObject(eventActions)
                    cleverTapAPI.pushEvent(eventName, Utils.convertJSONObjectToHashMap(eventActionsObject))
                } catch (e: JSONException) {
                    Logger.v("Unable to parse eventActions from WebView " + e.localizedMessage)
                }
            } else {
                Logger.v("eventActions passed to CTWebInterface is null")
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            if (profile != null) {
                try {
                    val profileObject = JSONObject(profile)
                    cleverTapAPI.pushProfile(Utils.convertJSONObjectToHashMap(profileObject))
                } catch (e: JSONException) {
                    Logger.v("Unable to parse profile from WebView " + e.localizedMessage)
                }
            } else {
                Logger.v("profile passed to CTWebInterface is null")
            }
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            if (key == null) {
                Logger.v("Key passed to CTWebInterface is null")
                return
            }
            if (values != null) {
                try {
                    val valuesArray = JSONArray(values)
                    cleverTapAPI.removeMultiValuesForKey(key, Utils.convertJSONArrayToArrayList(valuesArray))
                } catch (e: JSONException) {
                    Logger.v("Unable to parse values from WebView " + e.localizedMessage)
                }
            } else {
                Logger.v("values passed to CTWebInterface is null")
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            if (key == null) {
                Logger.v("Key passed to CTWebInterface is null")
                return
            }
            if (values != null) {
                try {
                    val valuesArray = JSONArray(values)
                    cleverTapAPI.setMultiValuesForKey(key, Utils.convertJSONArrayToArrayList(valuesArray))
                } catch (e: JSONException) {
                    Logger.v("Unable to parse values from WebView " + e.localizedMessage)
                }
            } else {
                Logger.v("values passed to CTWebInterface is null")
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.")
        } else {
            if (profile != null) {
                try {
                    val profileObject = JSONObject(profile)
                    cleverTapAPI.onUserLogin(Utils.convertJSONObjectToHashMap(profileObject))
                } catch (e: JSONException) {
                    Logger.v("Unable to parse profile from WebView " + e.localizedMessage)
                }
            } else {
                Logger.v("profile passed to CTWebInterface is null")
            }
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
        val cleverTapAPI = cleverTapWr.get()
        if (cleverTapAPI == null) {
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
            if (buttonId != null) {
                actionData.putString(Constants.KEY_WZRK_ELEMENT_ID, buttonId)
            }

            host.triggerAction(action, callToAction, actionData)
        } catch (je: JSONException) {
            Logger.d("CTWebInterface invalid action JSON: $actionJson")
        }
    }

    /**
     * Retrieve the version code of the CleverTap SDK.
     */
    @get:JavascriptInterface
    open val sdkVersion: Int
        get() = BuildConfig.VERSION_CODE
}

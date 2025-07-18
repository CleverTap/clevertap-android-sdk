package com.clevertap.android.sdk;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.inapp.CTInAppAction;
import com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFragment;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class helps WebViews to interact with CleverTapAPI via pre-defined methods
 */
@SuppressWarnings("WeakerAccess")
public class CTWebInterface {

    private WeakReference<CleverTapAPI> cleverTapWr = new WeakReference<>(null);

    private WeakReference<CTInAppBaseFragment> fragmentWr = new WeakReference<>(null);

    public CTWebInterface(CleverTapAPI instance) {
        this.cleverTapWr = new WeakReference<>(instance);
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI != null) {
            CoreState coreState = cleverTapAPI.getCoreState();
            if (coreState != null) {
                coreState.getCoreMetaData().setWebInterfaceInitializedExternally(true);
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public CTWebInterface(CleverTapAPI instance, CTInAppBaseFragment inAppBaseFragment) {
        this.cleverTapWr = new WeakReference<>(instance);
        this.fragmentWr = new WeakReference<>(inAppBaseFragment);
    }

    /**
     * Method to be called from WebView Javascript to request permission for notification
     * for Android 13 and above
     */
    @JavascriptInterface
    public void promptPushPermission(boolean shouldShowFallbackSettings) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            //Dismisses current IAM and proceeds to call promptForPushPermission()
            dismissInAppNotification();
            cleverTapAPI.promptForPushPermission(shouldShowFallbackSettings);
        }
    }

    /**
     * Method to be called from WebView Javascript to dismiss the InApp notification
     */
    @JavascriptInterface
    public void dismissInAppNotification() {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            //Dismisses current IAM and proceeds to call promptForPushPermission()
            CTInAppBaseFragment fragment = fragmentWr.get();
            if (fragment != null) {
                fragment.didDismiss(null);
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to add profile properties in CleverTap
     *
     * @param key   {@link String} value of profile property key
     * @param value {@link String} value of profile property value
     */
    @JavascriptInterface
    public void addMultiValueForKey(String key, String value) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.addMultiValueForKey(key, value);
        }
    }

    /**
     * Method to be called from WebView Javascript to increase the value of a particular property.
     * The key must hold numeric value
     *
     * @param key   {@link String} value of profile property key
     * @param value {@link Double} value of increment
     */
    @JavascriptInterface
    public void incrementValue(String key, double value) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.incrementValue(key, value);
        }
    }

    /**
     * Method to be called from WebView Javascript to decrease the value of a particular property.
     * The key must hold numeric value
     *
     * @param key   {@link String} value of profile property key
     * @param value {@link Double} value of decrement
     */
    @JavascriptInterface
    public void decrementValue(String key, double value) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.decrementValue(key, value);
        }
    }

    /**
     * Method to be called from WebView Javascript to add profile properties in CleverTap
     *
     * @param key    {@link String} value of profile property key
     * @param values Stringified {@link JSONArray} of profile property values
     */
    @JavascriptInterface
    public void addMultiValuesForKey(String key, String values) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if (key == null) {
                Logger.v("Key passed to CTWebInterface is null");
                return;
            }
            if (values != null) {
                try {
                    JSONArray valuesArray = new JSONArray(values);
                    cleverTapAPI.addMultiValuesForKey(key, Utils.convertJSONArrayToArrayList(valuesArray));
                } catch (JSONException e) {
                    Logger.v("Unable to parse values from WebView " + e.getLocalizedMessage());
                }
            } else {
                Logger.v("values passed to CTWebInterface is null");
            }
        }

    }

    /**
     * Method to be called from WebView Javascript to raise Charged event in CleverTap
     *
     * @param chargeDetails Stringified {@link JSONObject} of charged event details
     *                      Stringified {@link JSONObject} will be converted to a HashMap<String,Object>
     * @param items         A Stringified {@link JSONArray} which contains up to 15 JSON Object objects,
     *                      where each JSON Object object describes a particular item purchased
     *                      {@link JSONArray} of {@link JSONObject}s will be converted to an {@link ArrayList} of
     *                      {@link HashMap<String,Object>}
     */
    @JavascriptInterface
    @SuppressWarnings({"JavaDoc"})
    public void pushChargedEvent(String chargeDetails, String items) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            HashMap<String, Object> chargeDetailsHashMap = new HashMap<>();
            if (chargeDetails != null) {
                try {
                    JSONObject chargeDetailsObject = new JSONObject(chargeDetails);
                    chargeDetailsHashMap = Utils.convertJSONObjectToHashMap(chargeDetailsObject);
                } catch (JSONException e) {
                    Logger.v("Unable to parse chargeDetails for Charged Event from WebView " + e
                            .getLocalizedMessage());
                }
            } else {
                Logger.v("chargeDetails passed to CTWebInterface is null");
                return;
            }
            ArrayList<HashMap<String, Object>> itemsArrayList = null;
            if (items != null) {
                try {
                    JSONArray itemsArray = new JSONArray(items);
                    itemsArrayList = Utils.convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(itemsArray);
                } catch (JSONException e) {
                    Logger.v("Unable to parse items for Charged Event from WebView " + e.getLocalizedMessage());
                }
            } else {
                return;
            }
            cleverTapAPI.pushChargedEvent(chargeDetailsHashMap, itemsArrayList);
        }
    }

    /**
     * Method to be called from WebView Javascript to raise event in CleverTap
     *
     * @param eventName {@link String} value of event name
     */
    @JavascriptInterface
    public void pushEvent(String eventName) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushEvent(eventName);
        }
    }

    /**
     * Method to be called from WebView Javascript to raise event with event properties in CleverTap
     *
     * @param eventName    {@link String} value of event name
     * @param eventActions Stringified {@link JSONObject} of event properties
     */
    @JavascriptInterface
    public void pushEvent(String eventName, String eventActions) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if (eventActions != null) {
                try {
                    JSONObject eventActionsObject = new JSONObject(eventActions);
                    cleverTapAPI.pushEvent(eventName, Utils.convertJSONObjectToHashMap(eventActionsObject));
                } catch (JSONException e) {
                    Logger.v("Unable to parse eventActions from WebView " + e.getLocalizedMessage());
                }
            } else {
                Logger.v("eventActions passed to CTWebInterface is null");
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to push profile properties in CleverTap
     *
     * @param profile Stringified {@link JSONObject} of profile properties
     */
    @JavascriptInterface
    public void pushProfile(String profile) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if (profile != null) {
                try {
                    JSONObject profileObject = new JSONObject(profile);
                    cleverTapAPI.pushProfile(Utils.convertJSONObjectToHashMap(profileObject));
                } catch (JSONException e) {
                    Logger.v("Unable to parse profile from WebView " + e.getLocalizedMessage());
                }
            } else {
                Logger.v("profile passed to CTWebInterface is null");
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties in CleverTap
     *
     * @param key   {@link String} value of profile property key
     * @param value {@link String} value of profile property value
     */
    @JavascriptInterface
    public void removeMultiValueForKey(String key, String value) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if (key == null) {
                Logger.v("Key passed to CTWebInterface is null");
                return;
            }
            if (value == null) {
                Logger.v("Value passed to CTWebInterface is null");
                return;
            }
            cleverTapAPI.removeMultiValueForKey(key, value);
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties in CleverTap
     *
     * @param key    {@link String} value of profile property key
     * @param values Stringified {@link JSONArray} of profile property values
     */
    @JavascriptInterface
    public void removeMultiValuesForKey(String key, String values) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if (key == null) {
                Logger.v("Key passed to CTWebInterface is null");
                return;
            }
            if (values != null) {
                try {
                    JSONArray valuesArray = new JSONArray(values);
                    cleverTapAPI.removeMultiValuesForKey(key, Utils.convertJSONArrayToArrayList(valuesArray));
                } catch (JSONException e) {
                    Logger.v("Unable to parse values from WebView " + e.getLocalizedMessage());
                }
            } else {
                Logger.v("values passed to CTWebInterface is null");
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties for given key in CleverTap
     *
     * @param key {@link String} value of profile property key
     */
    @JavascriptInterface
    public void removeValueForKey(String key) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if (key == null) {
                Logger.v("Key passed to CTWebInterface is null");
                return;
            }
            cleverTapAPI.removeValueForKey(key);
        }
    }

    /**
     * Method to be called from WebView Javascript to set profile properties in CleverTap
     *
     * @param key    {@link String} value of profile property key
     * @param values Stringified {@link JSONArray} of profile property values
     */
    @JavascriptInterface
    public void setMultiValueForKey(String key, String values) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if (key == null) {
                Logger.v("Key passed to CTWebInterface is null");
                return;
            }
            if (values != null) {
                try {
                    JSONArray valuesArray = new JSONArray(values);
                    cleverTapAPI.setMultiValuesForKey(key, Utils.convertJSONArrayToArrayList(valuesArray));
                } catch (JSONException e) {
                    Logger.v("Unable to parse values from WebView " + e.getLocalizedMessage());
                }
            } else {
                Logger.v("values passed to CTWebInterface is null");
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to push profile/properties in CleverTap after
     * User Login
     *
     * @param profile Stringified {@link JSONObject} of profile properties
     */
    @JavascriptInterface
    public void onUserLogin(String profile) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if (profile != null) {
                try {
                    JSONObject profileObject = new JSONObject(profile);
                    cleverTapAPI.onUserLogin(Utils.convertJSONObjectToHashMap(profileObject));
                } catch (JSONException e) {
                    Logger.v("Unable to parse profile from WebView " + e.getLocalizedMessage());
                }
            } else {
                Logger.v("profile passed to CTWebInterface is null");
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
    public void triggerInAppAction(String actionJson, String callToAction, String buttonId) {
        CleverTapAPI cleverTapAPI = cleverTapWr.get();
        if (cleverTapAPI == null) {
            Logger.d("CTWebInterface CleverTap Instance is null.");
            return;
        }

        CTInAppBaseFragment fragment = fragmentWr.get();
        if (fragment == null) {
            Logger.d("CTWebInterface Fragment is null");
            return;
        }

        if (actionJson == null) {
            Logger.d("CTWebInterface action JSON is null");
            return;
        }

        try {
            CTInAppAction action = CTInAppAction.createFromJson(new JSONObject(actionJson));
            if (action == null) {
                Logger.d("CTWebInterface invalid action JSON: " + actionJson);
                return;
            }

            Bundle actionData = new Bundle();
            if (buttonId != null) {
                actionData.putString("button_id", buttonId);
            }

            fragment.triggerAction(action, callToAction, actionData);
        } catch (JSONException je) {
            Logger.d("CTWebInterface invalid action JSON: " + actionJson);
        }
    }

    /**
     * Retrieve the version code of the CleverTap SDK.
     */
    @JavascriptInterface
    public int getSdkVersion() {
        return BuildConfig.VERSION_CODE;
    }
}

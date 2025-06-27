package com.clevertap.android.sdk.events;

import static com.clevertap.android.sdk.Constants.DATE_PREFIX;
import static com.clevertap.android.sdk.Constants.KEY_NEW_VALUE;
import static com.clevertap.android.sdk.Constants.KEY_OLD_VALUE;
import static com.clevertap.android.sdk.Constants.keysToSkipForUserAttributesEvaluation;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.LocalDataStore;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ProfileValueHandler;
import com.clevertap.android.sdk.network.NetworkRepo;
import com.clevertap.android.sdk.validation.Validator;
import com.clevertap.android.sdk.variables.JsonUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EventMediator {

    private final CoreMetaData cleverTapMetaData;

    private final CleverTapInstanceConfig config;

    private final LocalDataStore localDataStore;

    private final NetworkRepo networkRepo;

    private final ProfileValueHandler profileValueHandler;

    public EventMediator(CleverTapInstanceConfig config, CoreMetaData coreMetaData,
                         LocalDataStore localDataStore, ProfileValueHandler profileValueHandler, NetworkRepo networkRepo) {
        this.config = config;
        this.localDataStore = localDataStore;
        this.networkRepo = networkRepo;
        this.profileValueHandler = profileValueHandler;
        cleverTapMetaData = coreMetaData;
    }

    public boolean shouldDeferProcessingEvent(JSONObject event, int eventType) {
        //noinspection SimplifiableIfStatement
        if (eventType == Constants.DEFINE_VARS_EVENT) {
            return false;
        }
        if (config.isCreatedPostAppLaunch()) {
            return false;
        }
        if (event.has("evtName")) {
            try {
                if (Arrays.asList(Constants.SYSTEM_EVENTS).contains(event.getString("evtName"))) {
                    return false;
                }
            } catch (JSONException e) {
                //no-op
            }
        }
        return (eventType == Constants.RAISED_EVENT && !cleverTapMetaData.isAppLaunchPushed());
    }

    public boolean shouldDropEvent(JSONObject event, int eventType) {

        if (eventType == Constants.FETCH_EVENT || eventType == Constants.DEFINE_VARS_EVENT) {
            return false;
        }

        if (networkRepo.isMuted()) {
            config.getLogger()
                    .verbose(config.getAccountId(), "CleverTap is muted, dropping event - " + event.toString());
            return true;
        }

        if (!cleverTapMetaData.isCurrentUserOptedOut()) {
            return false;
        }

        if (!cleverTapMetaData.getEnabledSystemEvents()) {
            config.getLogger().debug(config.getAccountId(), "Current user is opted out dropping event: " + event);
            // opted-out and system events disabled
            return true;
        }

        if (eventType != Constants.RAISED_EVENT && eventType != Constants.NV_EVENT) {
            // opted-out and system events enabled
            return false;
        }

        // opted-out and system events enabled
        // check for Constants.RAISED_EVENT and Constants.NV_EVENT event special cases
        String eName = event != null ? getEventName(event) : null;
        boolean isSystemEvent = Arrays.asList(Validator.restrictedNames).contains(eName);
        boolean dropEvent = !isSystemEvent;
        if (dropEvent) {
            config.getLogger().debug(config.getAccountId(), "Current user is opted out dropping event: " + event);
        }
        return dropEvent;
    }

    public boolean isAppLaunchedEvent(JSONObject event) {
        try {
            return event.has(Constants.KEY_EVT_NAME)
                    && event.getString(Constants.KEY_EVT_NAME).equals(Constants.APP_LAUNCHED_EVENT);
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean isEvent(JSONObject event) {
        return event.has(Constants.KEY_EVT_NAME);
    }


    public String getEventName(JSONObject event) {
        try {
            return event.getString(Constants.KEY_EVT_NAME);
        } catch (JSONException e) {
            return null;
        }
    }

    public Map<String, Object> getEventProperties(JSONObject event) {
        if (event.has(Constants.KEY_EVT_NAME) && event.has(Constants.KEY_EVT_DATA)) {
            try {
                return JsonUtil.mapFromJson(event.getJSONObject(Constants.KEY_EVT_DATA));
            } catch (JSONException e) {
                Logger.v("Could not convert JSONObject to Map - " + e
                        .getMessage());
            }
        }
        return new HashMap<>();
    }

    public boolean isChargedEvent(JSONObject event) {
        try {
            return event.has(Constants.KEY_EVT_NAME)
                    && event.getString(Constants.KEY_EVT_NAME).equals(Constants.CHARGED_EVENT);
        } catch (JSONException e) {
            return false;
        }
    }

    public List<Map<String, Object>> getChargedEventItemDetails(JSONObject event) {
        try {
            return JsonUtil.listFromJson(
                    event.getJSONObject(Constants.KEY_EVT_DATA).getJSONArray(Constants.KEY_ITEMS));
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getChargedEventDetails(JSONObject event) {
        try {
            final Object items = event.getJSONObject(Constants.KEY_EVT_DATA).remove(Constants.KEY_ITEMS);
            final Map<String, Object> chargedDetails = JsonUtil.mapFromJson(
                    event.getJSONObject(Constants.KEY_EVT_DATA));
            event.getJSONObject(Constants.KEY_EVT_DATA).put(Constants.KEY_ITEMS, items);
            return chargedDetails;
        } catch (JSONException e) {
            return new HashMap<>();
        }
    }

    /**
     * This function computes the newValue and the oldValue for each user attribute of the event
     * It also updates the user properties in the local cache and db
     *
     * @param event - profile event
     * @return - a map representing the oldValue and newValue of each user-attribute in event
     */
    public Map<String, Map<String, Object>> computeUserAttributeChangeProperties(final JSONObject event) {
        Map<String, Map<String, Object>> userAttributesChangeProperties = new HashMap<>();
        Map<String, Object> fieldsToPersistLocally = new HashMap<>();
        JSONObject profile = event.optJSONObject(Constants.PROFILE);

        if (profile == null) {
            return userAttributesChangeProperties;
        }

        Iterator<String> keys = profile.keys();

        while (keys.hasNext()) {
            String key = keys.next();

            try {
                if (keysToSkipForUserAttributesEvaluation.contains(key)) {
                    continue;
                }
                Object oldValue = localDataStore.getProfileProperty(key);
                Object newValue = profile.get(key);

                // if newValue is a JSONObject, it will have a structure of {"$command":value}.
                // In such a case handle this command to compute newValue
                if (newValue instanceof JSONObject) {
                    JSONObject obj = (JSONObject) newValue;
                    String commandIdentifier = obj.keys().next();
                    switch (commandIdentifier) {
                        case Constants.COMMAND_INCREMENT:
                        case Constants.COMMAND_DECREMENT:
                            newValue = profileValueHandler.handleIncrementDecrementValues(
                                    (Number) obj.get(commandIdentifier), commandIdentifier, (Number) oldValue);
                            break;
                        case Constants.COMMAND_DELETE:
                            newValue = null;
                            break;
                        case Constants.COMMAND_SET:
                        case Constants.COMMAND_ADD:
                        case Constants.COMMAND_REMOVE:
                            newValue = profileValueHandler.handleMultiValues(key,
                                    ((JSONArray) obj.get(commandIdentifier)), commandIdentifier, oldValue);
                            break;
                    }
                } else if (newValue instanceof String) {
                    // Remove the date prefix before evaluation and persisting
                    if (((String) newValue).startsWith(DATE_PREFIX)) {
                        newValue = Long.parseLong(((String) newValue).substring(DATE_PREFIX.length()));
                    }
                }

                Map<String, Object> properties = new HashMap<>();

                // Skip multivalued user attributes for evaluation
                if (oldValue != null && !(oldValue instanceof JSONArray)) {
                    properties.put(KEY_OLD_VALUE, oldValue);
                }
                if (newValue != null && !(newValue instanceof JSONArray)) {
                    properties.put(KEY_NEW_VALUE, newValue);
                }

                // Skip evaluation if both newValue or oldValue are null
                if (!properties.isEmpty()) {
                    userAttributesChangeProperties.put(key, properties);
                }

                fieldsToPersistLocally.put(key, newValue);
            } catch (JSONException e) {
                config.getLogger()
                        .debug(config.getAccountId(), "Error getting user attribute changes for key: " + key + e);
            }
        }

        localDataStore.updateProfileFields(fieldsToPersistLocally);
        return userAttributesChangeProperties;
    }
}

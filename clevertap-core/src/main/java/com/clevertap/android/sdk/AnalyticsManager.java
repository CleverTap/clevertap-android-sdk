package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.CTJsonConverter.getErrorObject;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class AnalyticsManager {

    private final BaseQueueManager mBaseQueueManager;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private final ValidationResultStack mValidationResultStack;

    private final Validator mValidator;
    private final CoreMetaData mCoreMetaData;
    private final PostAsyncSafelyHandler mPostAsyncSafelyHandler;
    private final DeviceInfo mDeviceInfo;

    private final LocalDataStore mLocalDataStore;

    private final NetworkManager mNetworkManager;

    AnalyticsManager(CoreState coreState) {
        mBaseQueueManager = coreState.getBaseEventQueueManager();

        mValidator = coreState.getValidator();
        mValidationResultStack = coreState.getValidationResultStack();
        mConfig = coreState.getConfig();
        mContext = coreState.getContext();
        mCoreMetaData = coreState.getCoreMetaData();
        mPostAsyncSafelyHandler = coreState.getPostAsyncSafelyHandler();
        mLocalDataStore = coreState.getLocalDataStore();
        mDeviceInfo = coreState.getDeviceInfo();
        mNetworkManager = (NetworkManager) coreState.getNetworkManager();
    }

    public void pushEvent(String eventName, Map<String, Object> eventActions) {

        if (eventName == null || eventName.equals("")) {
            return;
        }

        ValidationResult validationResult = mValidator.isRestrictedEventName(eventName);
        // Check for a restricted event name
        if (validationResult.getErrorCode() > 0) {
            mValidationResultStack.pushValidationResult(validationResult);
            return;
        }

        ValidationResult discardedResult = mValidator.isEventDiscarded(eventName);
        // Check for a discarded event name
        if (discardedResult.getErrorCode() > 0) {
            mValidationResultStack.pushValidationResult(discardedResult);
            return;
        }

        if (eventActions == null) {
            eventActions = new HashMap<>();
        }

        JSONObject event = new JSONObject();
        try {
            // Validate
            ValidationResult vr = mValidator.cleanEventName(eventName);

            // Check for an error
            if (vr.getErrorCode() != 0) {
                event.put(Constants.ERROR_KEY, getErrorObject(vr));
            }

            eventName = vr.getObject().toString();
            JSONObject actions = new JSONObject();
            for (String key : eventActions.keySet()) {
                Object value = eventActions.get(key);
                vr = mValidator.cleanObjectKey(key);
                key = vr.getObject().toString();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                try {
                    vr = mValidator.cleanObjectValue(value, Validator.ValidationContext.Event);
                } catch (IllegalArgumentException e) {
                    // The object was neither a String, Boolean, or any number primitives
                    ValidationResult error = ValidationResultFactory
                            .create(512, Constants.PROP_VALUE_NOT_PRIMITIVE, eventName, key,
                                    value != null ? value.toString() : "");
                    mConfig.getLogger().debug(mConfig.getAccountId(), error.getErrorDesc());
                    mValidationResultStack.pushValidationResult(error);
                    // Skip this record
                    continue;
                }
                value = vr.getObject();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                actions.put(key, value);
            }
            event.put("evtName", eventName);
            event.put("evtData", actions);
            mBaseQueueManager.queueEvent(mContext, event, Constants.RAISED_EVENT);
        } catch (Throwable t) {
            // We won't get here
        }
    }

    @SuppressWarnings({"unused"})
    public void pushError(final String errorMessage, final int errorCode) {
        final HashMap<String, Object> props = new HashMap<>();
        props.put("Error Message", errorMessage);
        props.put("Error Code", errorCode);

        try {
            final String activityName = mCoreMetaData.getCurrentActivityName();
            if (activityName != null) {
                props.put("Location", activityName);
            } else {
                props.put("Location", "Unknown");
            }
        } catch (Throwable t) {
            // Ignore
            props.put("Location", "Unknown");
        }

        pushEvent("Error Occurred", props);
    }


    public void pushProfile(final Map<String, Object> profile) {
        if (profile == null || profile.isEmpty()) {
            return;
        }

        mPostAsyncSafelyHandler.postAsyncSafely("profilePush", new Runnable() {
            @Override
            public void run() {
                _push(profile);
            }
        });
    }

    private void _push(Map<String, Object> profile) {
        if (profile == null || profile.isEmpty()) {
            return;
        }

        try {
            ValidationResult vr;
            JSONObject customProfile = new JSONObject();
            JSONObject fieldsToUpdateLocally = new JSONObject();
            for (String key : profile.keySet()) {
                Object value = profile.get(key);

                vr = mValidator.cleanObjectKey(key);
                key = vr.getObject().toString();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    mValidationResultStack.pushValidationResult(vr);
                }

                if (key.isEmpty()) {
                    ValidationResult keyError = ValidationResultFactory.create(512, Constants.PUSH_KEY_EMPTY);
                    mValidationResultStack.pushValidationResult(keyError);
                    mConfig.getLogger().debug(mConfig.getAccountId(), keyError.getErrorDesc());
                    // Skip this property
                    continue;
                }

                try {
                    vr = mValidator.cleanObjectValue(value, Validator.ValidationContext.Profile);
                } catch (Throwable e) {
                    // The object was neither a String, Boolean, or any number primitives
                    ValidationResult error = ValidationResultFactory.create(512,
                            Constants.OBJECT_VALUE_NOT_PRIMITIVE_PROFILE,
                            value != null ? value.toString() : "", key);
                    mValidationResultStack.pushValidationResult(error);
                    mConfig.getLogger().debug(mConfig.getAccountId(), error.getErrorDesc());
                    // Skip this property
                    continue;
                }
                value = vr.getObject();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    mValidationResultStack.pushValidationResult(vr);
                }

                // test Phone:  if no device country code, test if phone starts with +, log but always send
                if (key.equalsIgnoreCase("Phone")) {
                    try {
                        value = value.toString();
                        String countryCode = mDeviceInfo.getCountryCode();
                        if (countryCode == null || countryCode.isEmpty()) {
                            String _value = (String) value;
                            if (!_value.startsWith("+")) {
                                ValidationResult error = ValidationResultFactory
                                        .create(512, Constants.INVALID_COUNTRY_CODE, _value);
                                mValidationResultStack.pushValidationResult(error);
                                mConfig.getLogger().debug(mConfig.getAccountId(), error.getErrorDesc());
                            }
                        }
                        mConfig.getLogger().verbose(mConfig.getAccountId(),
                                "Profile phone is: " + value + " device country code is: " + ((countryCode != null)
                                        ? countryCode : "null"));
                    } catch (Exception e) {
                        mValidationResultStack
                                .pushValidationResult(ValidationResultFactory.create(512, Constants.INVALID_PHONE));
                        mConfig.getLogger().debug(mConfig.getAccountId(), "Invalid phone number: " + e.getLocalizedMessage());
                        continue;
                    }
                }

                // add to the local profile update object
                fieldsToUpdateLocally.put(key, value);
                customProfile.put(key, value);
            }

            mConfig.getLogger().verbose(mConfig.getAccountId(), "Constructed custom profile: " + customProfile.toString());

            // update local profile values
            if (fieldsToUpdateLocally.length() > 0) {
                mLocalDataStore.setProfileFields(fieldsToUpdateLocally);
            }

            mBaseQueueManager.pushBasicProfile(customProfile);

        } catch (Throwable t) {
            // Will not happen
            mConfig.getLogger().verbose(mConfig.getAccountId(), "Failed to push profile", t);
        }
    }

    //Event
    public void forcePushAppLaunchedEvent() {
        mCoreMetaData.setAppLaunchPushed(false);
        pushAppLaunchedEvent();
    }

    public void pushAppLaunchedEvent() {
        if (mConfig.isDisableAppLaunchedEvent()) {
            mCoreMetaData.setAppLaunchPushed(true);
            mConfig.getLogger().debug(mConfig.getAccountId(), "App Launched Events disabled in the Android Manifest file");
            return;
        }
        if (mCoreMetaData.isAppLaunchPushed()) {
            mConfig.getLogger()
                    .verbose(mConfig.getAccountId(), "App Launched has already been triggered. Will not trigger it ");
            return;
        } else {
            mConfig.getLogger().verbose(mConfig.getAccountId(), "Firing App Launched event");
        }
        mCoreMetaData.setAppLaunchPushed(true);
        JSONObject event = new JSONObject();
        try {
            event.put("evtName", Constants.APP_LAUNCHED_EVENT);

            event.put("evtData", mNetworkManager
                    .getAppLaunchedFields());//TODO move this outside n/w mgr
        } catch (Throwable t) {
            // We won't get here
        }
        mBaseQueueManager.queueEvent(mContext, event, Constants.RAISED_EVENT);
    }
}
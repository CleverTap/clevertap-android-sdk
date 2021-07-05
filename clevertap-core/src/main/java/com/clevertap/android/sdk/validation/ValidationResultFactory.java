package com.clevertap.android.sdk.validation;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.Constants;

/**
 * Groups all error messages in one place
 */
@RestrictTo(Scope.LIBRARY)
public class ValidationResultFactory {

    /**
     * Returns error object containing error code and message based on given parameters
     *
     * @param errorCode   error code
     * @param messageCode message code belonging to error code
     * @param values      values to add in error message
     * @return instance of {@link ValidationResult}
     */
    public static ValidationResult create(int errorCode, int messageCode, String... values) {
        ValidationResult error = new ValidationResult();
        error.setErrorCode(errorCode);
        String msg = "";

        try {
            switch (errorCode) {
                case 512:
                    switch (messageCode) {
                        case Constants.INVALID_MULTI_VALUE:
                            msg = "Invalid multi value for key " + values[0]
                                    + ", profile multi value operation aborted.";
                            break;
                        case Constants.INVALID_INCREMENT_DECREMENT_VALUE:
                            msg = "Increment/Decrement value for profile key " +  values[0]
                                   + ", cannot be zero or negative";
                            break;
                        case Constants.PUSH_KEY_EMPTY:
                            msg = "Profile push key is empty";
                            break;
                        case Constants.OBJECT_VALUE_NOT_PRIMITIVE_PROFILE:
                            msg = "Object value wasn't a primitive (" + values[0] + ") for profile field "
                                    + values[1];
                            break;
                        case Constants.INVALID_COUNTRY_CODE:
                            msg = "Device country code not available and profile phone: " + values[0]
                                    + " does not appear to start with country code";
                            break;
                        case Constants.INVALID_PHONE:
                            msg = "Invalid phone number";
                            break;
                        case Constants.KEY_EMPTY:
                            msg = "Key is empty, profile removeValueForKey aborted.";
                            break;
                        case Constants.PROP_VALUE_NOT_PRIMITIVE:
                            msg = "For event \"" + values[0] + "\": Property value for property " + values[1]
                                    + " wasn't a primitive (" + values[2] + ")";
                            break;
                        case Constants.CHANNEL_ID_MISSING_IN_PAYLOAD:
                            msg
                                    =
                                    "Unable to render notification, channelId is required but not provided in the notification payload: "
                                            + values[0];
                            break;
                        case Constants.CHANNEL_ID_NOT_REGISTERED:
                            msg = "Unable to render notification, channelId: " + values[0]
                                    + " not registered by the app.";
                            break;
                        case Constants.NOTIFICATION_VIEWED_DISABLED:
                            msg
                                    =
                                    "Recording of Notification Viewed is disabled in the CleverTap Dashboard for notification payload: "
                                            + values[0];
                            break;
                    }
                    break;
                case 521:
                    switch (messageCode) {
                        case Constants.VALUE_CHARS_LIMIT_EXCEEDED:
                            msg = values[0] + "... exceeds the limit of " + values[1] + " characters. Trimmed";
                            break;
                        case Constants.MULTI_VALUE_CHARS_LIMIT_EXCEEDED:
                            msg = "Multi value property for key " + values[0] + " exceeds the limit of " + values[1]
                                    + " items. Trimmed";
                            break;
                        case Constants.INVALID_PROFILE_PROP_ARRAY_COUNT:
                            msg = "Invalid user profile property array count - " + values[0] + " max is - "
                                    + values[1];
                            break;
                    }
                    break;
                case 520:
                case 510:
                    switch (messageCode) {
                        case Constants.VALUE_CHARS_LIMIT_EXCEEDED:
                            msg = values[0] + "... exceeds the limit of " + values[1] + " characters. Trimmed";
                            break;
                        case Constants.EVENT_NAME_NULL:
                            msg = "Event Name is null";
                            break;
                    }
                    break;
                case 511:
                    switch (messageCode) {
                        case Constants.PROP_VALUE_NOT_PRIMITIVE:
                            msg = "For event " + values[0] + ": Property value for property " + values[1]
                                    + " wasn't a primitive (" + values[2] + ")";
                            break;
                        case Constants.OBJECT_VALUE_NOT_PRIMITIVE:
                            msg = "An item's object value for key " + values[0] + " wasn't a primitive (" + values[1]
                                    + ")";
                            break;
                    }
                    break;
                case 513:
                    switch (messageCode) {
                        case Constants.RESTRICTED_EVENT_NAME:
                            msg = values[0] + " is a restricted event name. Last event aborted.";
                            break;
                        case Constants.DISCARDED_EVENT_NAME:
                            msg = values[0] + " is a discarded event name. Last event aborted.";
                            break;
                    }
                    break;
                case 514:
                    switch (messageCode) {
                        case Constants.USE_CUSTOM_ID_FALLBACK:
                            msg
                                    = "CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml/Instance Configuration. CleverTap SDK will create a fallback device ID";
                            break;
                        case Constants.USE_CUSTOM_ID_MISSING_IN_MANIFEST:
                            msg
                                    = "CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml. Custom CleverTap ID passed will not be used.";
                            break;
                        case Constants.UNABLE_TO_SET_CT_CUSTOM_ID:
                            msg = "CleverTap ID - " + values[0]
                                    + " already exists. Unable to set custom CleverTap ID - " + values[1];
                            break;
                        case Constants.INVALID_CT_CUSTOM_ID:
                            msg = "Attempted to set invalid custom CleverTap ID - " + values[0]
                                    + ", falling back to default error CleverTap ID - " + values[1];
                            break;
                    }
                    break;
                case 522:
                    msg = "Charged event contained more than 50 items.";
                    break;
                case 523:
                    switch (messageCode) {
                        case Constants.INVALID_MULTI_VALUE_KEY:
                            msg = "Invalid multi-value property key " + values[0];
                            break;
                        case Constants.RESTRICTED_MULTI_VALUE_KEY:
                            msg = values[0]
                                    + "... is a restricted key for multi-value properties. Operation aborted.";
                            break;
                    }
                    break;
                case 531:
                    msg = "Profile Identifiers mismatch with the previously saved ones";
                    break;
            }
        } catch (Exception e) {
            msg = "";
        }

        error.setErrorDesc(msg);
        return error;
    }

    @SuppressWarnings("SameParameterValue")
    public static ValidationResult create(int errorCode) {
        return create(errorCode, -1);
    }

}
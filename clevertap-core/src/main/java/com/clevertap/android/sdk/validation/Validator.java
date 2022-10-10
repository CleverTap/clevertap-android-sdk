package com.clevertap.android.sdk.validation;

import androidx.annotation.NonNull;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides methods to validate various entities.
 */
public final class Validator {

    public enum ValidationContext {
        Profile(), Event()
    }

    private enum RestrictedMultiValueFields {
        Name(), Email(), Education(),
        Married(), DOB(), Gender(),
        Phone(), Age(), FBID(), GPID(), Birthday()
    }

    public static final String ADD_VALUES_OPERATION = "multiValuePropertyAddValues";

    public static final String REMOVE_VALUES_OPERATION = "multiValuePropertyRemoveValues";

    private static final String[] eventNameCharsNotAllowed = {".", ":", "$", "'", "\"", "\\"};

    private static final String[] objectKeyCharsNotAllowed = {".", ":", "$", "'", "\"", "\\"};

    private static final String[] objectValueCharsNotAllowed = {"'", "\"", "\\"};

    private static final String[] restrictedNames = {"Stayed", "Notification Clicked",
            "Notification Viewed", "UTM Visited", "Notification Sent", "App Launched", "wzrk_d",
            "App Uninstalled", "Notification Bounced", Constants.GEOFENCE_ENTERED_EVENT_NAME,
            Constants.GEOFENCE_EXITED_EVENT_NAME, Constants.DC_OUTGOING_EVENT_NAME,
            Constants.DC_INCOMING_EVENT_NAME, Constants.DC_END_EVENT_NAME};

    private ArrayList<String> discardedEvents;

    /**
     * Cleans the event name to the following guidelines:
     * <p>
     * The following characters are removed:
     * dot, colon, dollar sign, single quote, double quote, and backslash.
     * Additionally, the event name is limited to 32 characters.
     * </p>
     *
     * @param name The event name to be cleaned
     * @return The {@link ValidationResult} object containing the object,
     * and the error code(if any)
     */
    public ValidationResult cleanEventName(String name) {
        ValidationResult vr = new ValidationResult();

        name = name.trim();
        for (String x : eventNameCharsNotAllowed) {
            name = name.replace(x, "");
        }

        if (name.length() > Constants.MAX_VALUE_LENGTH) {
            name = name.substring(0, Constants.MAX_VALUE_LENGTH - 1);
            ValidationResult error = ValidationResultFactory
                    .create(510, Constants.VALUE_CHARS_LIMIT_EXCEEDED, name.trim(), Constants.MAX_VALUE_LENGTH + "");
            vr.setErrorDesc(error.getErrorDesc());
            vr.setErrorCode(error.getErrorCode());
        }

        vr.setObject(name.trim());
        return vr;
    }

    /**
     * Cleans a multi-value property key.
     *
     * @param name Name of the property key
     * @return The {@link ValidationResult} object containing the key,
     * and the error code(if any)
     * <p/>
     * First calls cleanObjectKey
     * Known property keys are reserved for multi-value properties, subsequent validation is done for those
     */
    public ValidationResult cleanMultiValuePropertyKey(String name) {
        ValidationResult vr = cleanObjectKey(name);

        name = (String) vr.getObject();

        // make sure its not a known property key (reserved in the case of multi-value)

        try {
            RestrictedMultiValueFields rf = RestrictedMultiValueFields.valueOf(name);
            //noinspection ConstantConditions
            if (rf != null) {
                ValidationResult error = ValidationResultFactory
                        .create(523, Constants.RESTRICTED_MULTI_VALUE_KEY, name);
                vr.setErrorDesc(error.getErrorDesc());
                vr.setErrorCode(error.getErrorCode());
                vr.setObject(null);
            }
        } catch (Throwable t) {
            //no-op
        }

        return vr;
    }

    /**
     * Cleans a multi-value property value.
     * <p/>
     * trims whitespace, forces lowercase
     * removes reserved characters
     * trims byte len to currently 40 bytes
     *
     * @param value the property value
     * @return The {@link ValidationResult} object containing the value,
     * and the error code(if any)
     */
    public ValidationResult cleanMultiValuePropertyValue(@NonNull String value) {
        ValidationResult vr = new ValidationResult();

        // trim whitespace and force lowercase
        value = value.trim().toLowerCase();

        // remove reserved characters
        for (String x : objectValueCharsNotAllowed) {
            value = value.replace(x, "");
        }

        try {
            if (value.length() > Constants.MAX_MULTI_VALUE_LENGTH) {
                value = value.substring(0, Constants.MAX_MULTI_VALUE_LENGTH - 1);
                ValidationResult error = ValidationResultFactory
                        .create(521, Constants.VALUE_CHARS_LIMIT_EXCEEDED, value,
                                Constants.MAX_MULTI_VALUE_LENGTH + "");
                vr.setErrorDesc(error.getErrorDesc());
                vr.setErrorCode(error.getErrorCode());
            }
        } catch (Exception ignore) {
            // We really shouldn't get here
            // Ignore
        }

        vr.setObject(value);

        return vr;

    }

    /**
     * Cleans the object key.
     *
     * @param name Name of the object key
     * @return The {@link ValidationResult} object containing the object,
     * and the error code(if any)
     */
    public ValidationResult cleanObjectKey(String name) {
        ValidationResult vr = new ValidationResult();
        name = name.trim();
        for (String x : objectKeyCharsNotAllowed) {
            name = name.replace(x, "");
        }

        if (name.length() > Constants.MAX_KEY_LENGTH) {
            name = name.substring(0, Constants.MAX_KEY_LENGTH - 1);
            ValidationResult error = ValidationResultFactory
                    .create(520, Constants.VALUE_CHARS_LIMIT_EXCEEDED, name.trim(), Constants.MAX_KEY_LENGTH + "");
            vr.setErrorDesc(error.getErrorDesc());
            vr.setErrorCode(error.getErrorCode());
        }

        vr.setObject(name.trim());

        return vr;
    }

    /**
     * Cleans the object value, only if it is a string, otherwise, it simply returns the object.
     * <p/>
     * It also accepts a {@link Date} object, and converts it to a CleverTap
     * specific date format.
     * <p/>
     *
     * @param o Object to be cleaned(only if it is a string)
     * @return The cleaned object
     */
    @SuppressWarnings("unchecked")
    public ValidationResult cleanObjectValue(Object o, ValidationContext validationContext)
            throws IllegalArgumentException {
        ValidationResult vr = new ValidationResult();
        // If it's any type of number, send it back
        if (o instanceof Integer
                || o instanceof Float
                || o instanceof Boolean
                || o instanceof Double
                || o instanceof Long) {
            vr.setObject(o);
            return vr;
        } else if (o instanceof String || o instanceof Character) {
            String value;
            if (o instanceof Character) {
                value = String.valueOf(o);
            } else {
                value = (String) o;
            }
            value = value.trim();

            for (String x : objectValueCharsNotAllowed) {
                value = value.replace(x, "");
            }

            try {
                if (value.length() > Constants.MAX_VALUE_LENGTH) {
                    value = value.substring(0, Constants.MAX_VALUE_LENGTH - 1);
                    ValidationResult error = ValidationResultFactory
                            .create(521, Constants.VALUE_CHARS_LIMIT_EXCEEDED, value.trim(),
                                    Constants.MAX_VALUE_LENGTH + "");
                    vr.setErrorDesc(error.getErrorDesc());
                    vr.setErrorCode(error.getErrorCode());
                }
            } catch (Exception ignore) {
                // We really shouldn't get here
                // Ignore
            }
            vr.setObject(value.trim());
            return vr;
        } else if (o instanceof Date) {
            String date = "$D_" + ((Date) o).getTime() / 1000;
            vr.setObject(date);
            return vr;
        } else if ((o instanceof String[] || o instanceof ArrayList) && validationContext
                .equals(ValidationContext.Profile)) {
            ArrayList<String> valuesList = null;
            if (o instanceof ArrayList) {
                valuesList = (ArrayList<String>) o;
            }
            String[] values = null;
            if (o instanceof String[]) {
                values = (String[]) o;
            }

            ArrayList<String> allStrings = new ArrayList<>();
            if (values != null) {
                for (String value : values) {
                    try {
                        allStrings.add(value);
                    } catch (Exception e) {
                        //no-op
                    }
                }
            } else {
                for (String value : valuesList) {
                    try {
                        allStrings.add(value);
                    } catch (Exception e) {
                        //no-op
                    }
                }
            }
            values = allStrings.toArray(new String[0]);
            if (values.length > 0 && values.length <= Constants.MAX_MULTI_VALUE_ARRAY_LENGTH) {
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = new JSONObject();
                for (String value : values) {
                    jsonArray.put(value);
                }
                try {
                    jsonObject.put(Constants.COMMAND_SET, jsonArray);
                } catch (JSONException e) {
                    //no-op
                }
                vr.setObject(jsonObject);
            } else {
                ValidationResult error = ValidationResultFactory
                        .create(521, Constants.INVALID_PROFILE_PROP_ARRAY_COUNT, values.length + "",
                                Constants.MAX_MULTI_VALUE_ARRAY_LENGTH + "");
                vr.setErrorDesc(error.getErrorDesc());
                vr.setErrorCode(error.getErrorCode());
            }
            return vr;
        } else {
            throw new IllegalArgumentException("Not a String, Boolean, Long, Integer, Float, Double, or Date");
        }
    }

    /**
     * Checks whether the specified event name has been discarded from Dashboard. If it is,
     * then create a pending error, and abort.
     *
     * @param name The event name
     * @return Boolean indication whether the event name has been discarded from Dashboard
     */
    public ValidationResult isEventDiscarded(String name) {
        ValidationResult error = new ValidationResult();
        if (name == null) {
            ValidationResult vr = ValidationResultFactory.create(510, Constants.EVENT_NAME_NULL);
            error.setErrorCode(vr.getErrorCode());
            error.setErrorDesc(vr.getErrorDesc());
            return error;
        }
        if (getDiscardedEvents() != null) {
            for (String x : getDiscardedEvents()) {
                if (name.equalsIgnoreCase(x)) {
                    // The event name is discarded
                    ValidationResult vr = ValidationResultFactory.create(513, Constants.DISCARDED_EVENT_NAME, name);
                    error.setErrorCode(vr.getErrorCode());
                    error.setErrorDesc(vr.getErrorDesc());
                    Logger.d(name
                            + " s a discarded event name as per CleverTap. Dropping event at SDK level. Check discarded events in CleverTap Dashboard settings.");
                    return error;
                }
            }
        }
        return error;
    }

    /**
     * Checks whether the specified event name is restricted. If it is,
     * then create a pending error, and abort.
     *
     * @param name The event name
     * @return Boolean indication whether the event name is restricted
     */
    public ValidationResult isRestrictedEventName(String name) {
        ValidationResult error = new ValidationResult();
        if (name == null) {
            ValidationResult vr = ValidationResultFactory.create(510, Constants.EVENT_NAME_NULL);
            error.setErrorCode(vr.getErrorCode());
            error.setErrorDesc(vr.getErrorDesc());
            return error;
        }
        for (String x : restrictedNames) {
            if (name.equalsIgnoreCase(x)) {
                // The event name is restricted
                ValidationResult vr = ValidationResultFactory.create(513, Constants.RESTRICTED_EVENT_NAME, name);
                error.setErrorCode(vr.getErrorCode());
                error.setErrorDesc(vr.getErrorDesc());
                Logger.v(vr.getErrorDesc());
                return error;
            }
        }
        return error;
    }

    /**
     * Merges a multi-value property JSONArray.
     * <p/>
     * trims to max length currently 100 items, on a FIFO basis
     * <p/>
     * please clean the key and newValues values before calling this
     *
     * @param currentValues current JSONArray property value
     * @param newValues     JSONArray of new values
     * @param action        String the action to take relative to the new values ($add, $remove)
     * @param key           String the property key
     * @return The {@link ValidationResult} object containing the merged value,
     * and the error code(if any)
     */
    public ValidationResult mergeMultiValuePropertyForKey(JSONArray currentValues, JSONArray newValues, String action,
            String key) {
        ValidationResult vr = new ValidationResult();
        boolean remove = REMOVE_VALUES_OPERATION.equals(action);
        return _mergeListInternalForKey(key, currentValues, newValues, remove, vr);
    }

    /**
     * scans right to left until max to maintain latest max values for the multi-value property specified by key.
     *
     * @param key    the property key
     * @param currentValues   original list
     * @param newValues  new list
     * @param remove if remove new list from original
     * @param vr     ValidationResult for error and merged list return
     */
    private ValidationResult _mergeListInternalForKey(String key, JSONArray currentValues, JSONArray newValues, boolean remove, ValidationResult vr) {

        if (currentValues == null) {
            vr.setObject(null);
            return vr;
        }

        if (newValues == null) {
            vr.setObject(currentValues);
            return vr;
        }

        int maxValNum = Constants.MAX_MULTI_VALUE_ARRAY_LENGTH;

        JSONArray mergedList = new JSONArray();

        HashSet<String> set = new HashSet<>();

        int currentValsLength = currentValues.length(), newValsLength = newValues.length();

        BitSet additionBitSet = null;

        if (!remove) {
            additionBitSet = new BitSet(currentValsLength + newValsLength);
        }

        int currentValsStartIdx = 0;

        int newValsStartIdx = scan(newValues, set, additionBitSet, currentValsLength);

        if (!remove && set.size() < maxValNum) {
            currentValsStartIdx = scan(currentValues, set, additionBitSet, 0);
        }

        for (int i = currentValsStartIdx; i < currentValsLength; i++) {
            try {
                if (remove) {
                    String currentValue = (String) currentValues.get(i);

                    if (!set.contains(currentValue)) {
                        mergedList.put(currentValue);
                    }
                } else if (!additionBitSet.get(i)) {
                    mergedList.put(currentValues.get(i));
                }

            } catch (Throwable t) {
                //no-op
            }
        }

        if (!remove && mergedList.length() < maxValNum) {

            for (int i = newValsStartIdx; i < newValsLength; i++) {

                try {
                    if (!additionBitSet.get(i + currentValsLength)) {
                        mergedList.put(newValues.get(i));
                    }
                } catch (Throwable t) {
                    //no-op
                }
            }
        }

        // check to see if the list got trimmed in the merge
        if (newValsStartIdx > 0 || currentValsStartIdx > 0) {
            ValidationResult error = ValidationResultFactory.create(521, Constants.MULTI_VALUE_CHARS_LIMIT_EXCEEDED, key, maxValNum + "");
            vr.setErrorCode(error.getErrorCode());
            vr.setErrorDesc(error.getErrorDesc());
        }

        vr.setObject(mergedList);

        return vr;
    }

    private ArrayList<String> getDiscardedEvents() {
        return discardedEvents;
    }

    // multi-value list operations

    public void setDiscardedEvents(ArrayList<String> discardedEvents) {
        this.discardedEvents = discardedEvents;
    }

    private int scan(JSONArray list, Set<String> set, BitSet additionBitSet, int off) {

        if (list != null) {

            int maxValNum = Constants.MAX_MULTI_VALUE_ARRAY_LENGTH;

            for (int i = list.length() - 1; i >= 0; i--) {

                try {
                    Object obj = list.get(i);

                    String n = obj != null ? obj.toString() : null;

                    if (additionBitSet == null) { /* remove */
                        if (n != null) {
                            set.add(n);
                        }
                    } else {
                        if (n == null || set.contains(n)) {
                            additionBitSet.set(i + off, true);
                        } else {
                            set.add(n);

                            if (set.size() == maxValNum) {
                                return i;
                            }
                        }
                    }

                } catch (Throwable t) {
                    // no-op
                }
            }
        }

        return 0;
    }
}

package com.clevertap.android.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides methods to validate various entities.
 */
final class Validator {
    private static final String[] eventNameCharsNotAllowed = {".", ":", "$", "'", "\"", "\\"};
    private static final String[] objectKeyCharsNotAllowed = {".", ":", "$", "'", "\"", "\\"};
    private static final String[] objectValueCharsNotAllowed = {"'", "\"", "\\"};
    private static final String[] restrictedNames = {"Stayed", "Notification Clicked",
            "Notification Viewed", "UTM Visited", "Notification Sent", "App Launched", "wzrk_d",
            "App Uninstalled", "Notification Bounced"};

    static final String ADD_VALUES_OPERATION = "multiValuePropertyAddValues";
    static final String REMOVE_VALUES_OPERATION = "multiValuePropertyRemoveValues";


    @SuppressWarnings("unused")
    private enum RestrictedMultiValueFields {
        Name(), Email(), Education(),
        Married(), DOB(), Gender(),
        Phone(), Age(), FBID(), GPID(), Birthday()
    }

    enum ValidationContext {
        Profile(), Event()
    }

    private ArrayList<String> discardedEvents;

    private ArrayList<String> getDiscardedEvents() {
        return discardedEvents;
    }

    public void setDiscardedEvents(ArrayList<String> discardedEvents) {
        this.discardedEvents = discardedEvents;
    }

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
    ValidationResult cleanEventName(String name) {
        ValidationResult vr = new ValidationResult();

        name = name.trim();
        for (String x : eventNameCharsNotAllowed)
            name = name.replace(x, "");

        if (name.length() > Constants.MAX_VALUE_LENGTH) {
            name = name.substring(0, Constants.MAX_VALUE_LENGTH-1);
            vr.setErrorDesc(name.trim() + "... exceeds the limit of "+ Constants.MAX_VALUE_LENGTH +" characters. Trimmed");
            vr.setErrorCode(510);
        }

        vr.setObject(name.trim());
        return vr;
    }


    /**
     * Cleans the object key.
     *
     * @param name Name of the object key
     * @return The {@link ValidationResult} object containing the object,
     * and the error code(if any)
     */
    ValidationResult cleanObjectKey(String name) {
        ValidationResult vr = new ValidationResult();
        name = name.trim();
        for (String x : objectKeyCharsNotAllowed)
            name = name.replace(x, "");

        if (name.length() > Constants.MAX_KEY_LENGTH) {
            name = name.substring(0, Constants.MAX_KEY_LENGTH-1);
            vr.setErrorDesc(name.trim() + "... exceeds the limit of "+ Constants.MAX_KEY_LENGTH + " characters. Trimmed");
            vr.setErrorCode(520);
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
    ValidationResult cleanMultiValuePropertyKey(String name) {
        ValidationResult vr = cleanObjectKey(name);

        name = (String) vr.getObject();

        // make sure its not a known property key (reserved in the case of multi-value)

        try {
            RestrictedMultiValueFields rf = RestrictedMultiValueFields.valueOf(name);
            //noinspection ConstantConditions
            if (rf != null) {
                vr.setErrorDesc(name + "... is a restricted key for multi-value properties. Operation aborted.");
                vr.setErrorCode(523);
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
    ValidationResult cleanMultiValuePropertyValue(String value) {
        ValidationResult vr = new ValidationResult();

        // trim whitespace and force lowercase
        value = value.trim().toLowerCase();

        // remove reserved characters
        for (String x : objectValueCharsNotAllowed) {
            value = value.replace(x, "");
        }

        try {
            if (value.length() > Constants.MAX_MULTI_VALUE_LENGTH) {
                value = value.substring(0, Constants.MAX_MULTI_VALUE_LENGTH-1);
                vr.setErrorDesc(value + "... exceeds the limit of " + Constants.MAX_MULTI_VALUE_LENGTH + " chars. Trimmed");
                vr.setErrorCode(521);
            }
        } catch (Exception ignore) {
            // We really shouldn't get here
            // Ignore
        }

        vr.setObject(value);

        return vr;

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
    ValidationResult mergeMultiValuePropertyForKey(JSONArray currentValues, JSONArray newValues, String action, String key) {
        ValidationResult vr = new ValidationResult();
        boolean remove = REMOVE_VALUES_OPERATION.equals(action);
        return _mergeListInternalForKey(key, currentValues, newValues, remove, vr);
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
    ValidationResult cleanObjectValue(Object o, ValidationContext validationContext)
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
            if (o instanceof Character)
                value = String.valueOf(o);
            else
                value = (String) o;
            value = value.trim();

            for (String x : objectValueCharsNotAllowed) {
                value = value.replace(x, "");
            }

            try {
                if (value.length() > Constants.MAX_VALUE_LENGTH) {
                    value = value.substring(0, Constants.MAX_VALUE_LENGTH-1);
                    vr.setErrorDesc(value.trim() + "... exceeds the limit of "+ Constants.MAX_VALUE_LENGTH+" chars. Trimmed");
                    vr.setErrorCode(521);
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
        } else if ((o instanceof String[] || o instanceof ArrayList) && validationContext.equals(ValidationContext.Profile)) {
            ArrayList<String> valuesList = null;
            if(o instanceof ArrayList)
                //noinspection unchecked
                valuesList = (ArrayList<String>) o;
            String[] values = null;
            if(o instanceof String[])
                values = (String[])o;

            ArrayList<String> allStrings = new ArrayList<>();
            if(values!=null) {
                for (String value : values) {
                    try {
                        allStrings.add(value);
                    } catch (Exception e) {
                        //no-op
                    }
                }
            }else{
                for (String value : valuesList) {
                    try {
                        allStrings.add(value);
                    } catch (Exception e) {
                        //no-op
                    }
                }
            }
            values = allStrings.toArray(new String[0]);
            if(values.length>0 && values.length <= Constants.MAX_MULTI_VALUE_ARRAY_LENGTH){
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = new JSONObject();
                for(String value: values){
                    jsonArray.put(value);
                }
                try {
                    jsonObject.put(Constants.COMMAND_SET, jsonArray);
                }catch (JSONException e){
                    //no-op
                }
                vr.setObject(jsonObject);
            }else{
                vr.setErrorDesc("Invalid user profile property array count - "+ values.length +" max is - "+ Constants.MAX_MULTI_VALUE_ARRAY_LENGTH);
                vr.setErrorCode(521);
            }
            return vr;
        } else {
            throw new IllegalArgumentException("Not a String, Boolean, Long, Integer, Float, Double, or Date");
        }
    }

    /**
     * Checks whether the specified event name is restricted. If it is,
     * then create a pending error, and abort.
     *
     * @param name The event name
     * @return Boolean indication whether the event name is restricted
     */
    ValidationResult isRestrictedEventName(String name) {
        ValidationResult error = new ValidationResult();
        if (name == null) {
            error.setErrorCode(510);
            error.setErrorDesc("Event Name is null");
            return error;
        }
        for (String x : restrictedNames)
            if (name.equalsIgnoreCase(x)) {
                // The event name is restricted

                error.setErrorCode(513);
                error.setErrorDesc(name + " is a restricted event name. Last event aborted.");
                Logger.v(name + " is a restricted system event name. Last event aborted.");
                return error;
            }
        return error;
    }

    /**
     * Checks whether the specified event name has been discarded from Dashboard. If it is,
     * then create a pending error, and abort.
     *
     * @param name The event name
     * @return Boolean indication whether the event name has been discarded from Dashboard
     */
    ValidationResult isEventDiscarded(String name) {
        ValidationResult error = new ValidationResult();
        if (name == null) {
            error.setErrorCode(510);
            error.setErrorDesc("Event Name is null");
            return error;
        }
        if(getDiscardedEvents() != null) {
            for (String x : getDiscardedEvents())
                if (name.equalsIgnoreCase(x)) {
                    // The event name is discarded
                    error.setErrorCode(513);
                    error.setErrorDesc(name + " is a discarded event name. Last event aborted.");
                    Logger.d(name + " s a discarded event name as per CleverTap. Dropping event at SDK level. Check discarded events in CleverTap Dashboard settings.");
                    return error;
                }
        }
        return error;
    }


    // multi-value list operations

    /**
     * scans right to left until max to maintain latest max values for the multi-value property specified by key.
     *
     * @param key    the property key
     * @param left   original list
     * @param right  new list
     * @param remove if remove new list from original
     * @param vr     ValidationResult for error and merged list return
     */
    private ValidationResult _mergeListInternalForKey(String key, JSONArray left,
                                                             JSONArray right, boolean remove, ValidationResult vr) {

        if (left == null) {
            vr.setObject(null);
            return vr;
        }

        if (right == null) {
            vr.setObject(left);
            return vr;
        }

        int maxValNum = Constants.MAX_MULTI_VALUE_ARRAY_LENGTH;

        JSONArray mergedList = new JSONArray();

        HashSet<String> set = new HashSet<>();

        int lsize = left.length(), rsize = right.length();

        BitSet dupSetForAdd = null;

        if (!remove)
            dupSetForAdd = new BitSet(lsize + rsize);

        int lidx = 0;

        int ridx = scan(right, set, dupSetForAdd, lsize);

        if (!remove && set.size() < maxValNum) {
            lidx = scan(left, set, dupSetForAdd, 0);
        }

        for (int i = lidx; i < lsize; i++) {
            try {
                if (remove) {
                    String _j = (String) left.get(i);

                    if (!set.contains(_j)) {
                        mergedList.put(_j);
                    }
                } else if (!dupSetForAdd.get(i)) {
                    mergedList.put(left.get(i));
                }

            } catch (Throwable t) {
                //no-op
            }
        }

        if (!remove && mergedList.length() < maxValNum) {

            for (int i = ridx; i < rsize; i++) {

                try {
                    if (!dupSetForAdd.get(i + lsize)) {
                        mergedList.put(right.get(i));
                    }
                } catch (Throwable t) {
                    //no-op
                }
            }
        }

        // check to see if the list got trimmed in the merge
        if (ridx > 0 || lidx > 0) {
            vr.setErrorDesc("Multi value property for key " + key + " exceeds the limit of " + maxValNum + " items. Trimmed");
            vr.setErrorCode(521);
        }

        vr.setObject(mergedList);

        return vr;
    }


    private int scan(JSONArray list, Set<String> set, BitSet dupSetForAdd, int off) {

        if (list != null) {

            int maxValNum = Constants.MAX_MULTI_VALUE_ARRAY_LENGTH;

            for (int i = list.length() - 1; i >= 0; i--) {

                try {
                    Object obj = list.get(i);

                    String n = obj != null ? obj.toString() : null;

                    if (dupSetForAdd == null) { /* remove */
                        if (n != null) set.add(n);
                    } else {
                        if (n == null || set.contains(n)) {
                            dupSetForAdd.set(i + off, true);
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

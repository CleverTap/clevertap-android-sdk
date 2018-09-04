package com.clevertap.android.sdk;

import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;

@Deprecated
public class ProfileHandler {

    private WeakReference<CleverTapAPI> weakReference;

    ProfileHandler(CleverTapAPI cleverTapAPI) {
        this.weakReference = new WeakReference<>(cleverTapAPI);
    }

    /**
     * Set a collection of unique values as a multi-value user profile property, any existing value will be overwritten.
     * Max 100 values, on reaching 100 cap, oldest value(s) will be removed.
     * Values must be Strings and are limited to 512 characters.
     *
     * @param key    String
     * @param values {@link ArrayList} with String values
     * @deprecated use {@link CleverTapAPI#setMultiValuesForKey(String key, ArrayList values)}
     */
    @Deprecated
    public void setMultiValuesForKey(final String key, final ArrayList<String> values) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.setMultiValuesForKey(key, values);
        }
    }

    /**
     * Add a unique value to a multi-value user profile property
     * If the property does not exist it will be created
     * <p/>
     * Max 100 values, on reaching 100 cap, oldest value(s) will be removed.
     * Values must be Strings and are limited to 512 characters.
     * <p/>
     * If the key currently contains a scalar value, the key will be promoted to a multi-value property
     * with the current value cast to a string and the new value(s) added
     *
     * @param key   String
     * @param value String
     * @deprecated use {@link CleverTapAPI#addMultiValueForKey(String key, String value)}
     */
    @Deprecated
    public void addMultiValueForKey(String key, String value) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.addMultiValueForKey(key, value);
        }
    }

    /**
     * Add a collection of unique values to a multi-value user profile property
     * If the property does not exist it will be created
     * <p/>
     * Max 100 values, on reaching 100 cap, oldest value(s) will be removed.
     * Values must be Strings and are limited to 512 characters.
     * <p/>
     * If the key currently contains a scalar value, the key will be promoted to a multi-value property
     * with the current value cast to a string and the new value(s) added
     *
     * @param key    String
     * @param values {@link ArrayList} with String values
     * @deprecated use {@link CleverTapAPI#addMultiValuesForKey(String key, ArrayList values)}
     */
    @Deprecated
    public void addMultiValuesForKey(final String key, final ArrayList<String> values) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.addMultiValuesForKey(key, values);
        }
    }

    /**
     * Remove a unique value from a multi-value user profile property
     * <p/>
     * If the key currently contains a scalar value, prior to performing the remove operation
     * the key will be promoted to a multi-value property with the current value cast to a string.
     * If the multi-value property is empty after the remove operation, the key will be removed.
     *
     * @param key   String
     * @param value String
     * @deprecated use {@link CleverTapAPI#removeMultiValueForKey(String key, String value)}
     */
    @Deprecated
    public void removeMultiValueForKey(String key, String value) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.removeMultiValueForKey(key, value);
        }
    }

    /**
     * Remove a collection of unique values from a multi-value user profile property
     * <p/>
     * If the key currently contains a scalar value, prior to performing the remove operation
     * the key will be promoted to a multi-value property with the current value cast to a string.
     * <p/>
     * If the multi-value property is empty after the remove operation, the key will be removed.
     *
     * @param key    String
     * @param values {@link ArrayList} with String values
     * @deprecated use {@link CleverTapAPI#removeMultiValuesForKey(String key, ArrayList values)}
     */
    @Deprecated
    public void removeMultiValuesForKey(final String key, final ArrayList<String> values) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.removeMultiValuesForKey(key, values);
        }
    }

    /**
     * Remove the user profile property value specified by key from the user profile
     *
     * @param key String
     * @deprecated use {@link CleverTapAPI#removeValueForKey(String key)}
     */
    @Deprecated
    public void removeValueForKey(final String key) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.removeValueForKey(key);
        }
    }

    /**
     * Push a profile update.
     *
     * @param profile A {@link Map}, with keys as strings, and values as {@link String},
     *                {@link Integer}, {@link Long}, {@link Boolean}, {@link Float}, {@link Double},
     *                {@link java.util.Date}, or {@link Character}
     * @deprecated use {@link CleverTapAPI#pushProfile(Map profile)}
     */
    @Deprecated
    public void push(final Map<String, Object> profile) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushProfile(profile);
        }
    }

    /**
     * Pushes everything available in the JSON object returned by the Facebook GraphRequest
     *
     * @param graphUser The object returned from Facebook
     * @deprecated use {@link CleverTapAPI#pushFacebookUser(JSONObject graphUser)}
     */
    @Deprecated
    public void pushFacebookUser(final JSONObject graphUser) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushFacebookUser(graphUser);
        }
    }

    /**
     * Pushes everything useful within the Google Plus
     * {@link com.google.android.gms.plus.model.people.Person} object.
     *
     * @param person The {@link com.google.android.gms.plus.model.people.Person} object
     * @see com.google.android.gms.plus.model.people.Person
     * @deprecated use {@link CleverTapAPI#pushGooglePlusPerson(com.google.android.gms.plus.model.people.Person person)}
     */
    @Deprecated
    public void pushGooglePlusPerson(final com.google.android.gms.plus.model.people.Person person) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushGooglePlusPerson(person);
        }
    }

    /**
     * Return the user profile property value for the specified key
     *
     * @param name String
     * @return {@link JSONArray}, String or null
     * @deprecated use {@link CleverTapAPI#getProperty(String name)}
     */
    @Deprecated
    public Object getProperty(String name) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
            return null;
        } else {
            return cleverTapAPI.getProperty(name);
        }
    }
}

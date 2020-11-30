package com.clevertap.android.sdk.login;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Utils;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Wrapper class of identities & related behaviors
 */
@RestrictTo(Scope.LIBRARY)
public class IdentitySet {

    private final HashSet<String> identities = new HashSet<>();

    private IdentitySet(String[] keys) {
        init(keys);
    }

    private IdentitySet(HashSet<String> hashSet) {
        identities.addAll(hashSet);
    }

    /**
     * Checks if two identity sets contain same identities
     *
     * @param thatObj - the second object to which we are comparing
     * @return - true if all entries are same else false
     */
    @Override
    public boolean equals(final Object thatObj) {
        if (this == thatObj) {
            return true;
        }
        if (thatObj == null || getClass() != thatObj.getClass()) {
            return false;
        }
        final IdentitySet that = (IdentitySet) thatObj;
        return identities.equals(that.identities);
    }

    /**
     * Stringifies the identity set in comma separated string value.
     * Set           String
     * e.g   {Email, Phone} =>   Email,Phone
     *
     * @return String value of the identity set.
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iterator = identities.iterator();
        while (iterator.hasNext()) {
            String type = iterator.next();
            if (Constants.ALL_IDENTITY_KEYS.contains(type)) {
                stringBuilder.append(type)
                        .append(iterator.hasNext() ? Constants.SEPARATOR_COMMA : "");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * checks if a given key is an identity or not
     *
     * @param Key - String value of key
     * @return - true , if the given key is an identity key else false.
     */
    boolean contains(String Key) {
        return Utils.containsIgnoreCase(identities, Key);
    }

    /**
     * @return true - if the set contains at least one of the valid identities {@link Constants#ALL_IDENTITY_KEYS}
     */
    boolean isValid() {
        return !identities.isEmpty();
    }

    /**
     * Initialises the identity set using the string array items.
     * It also filters out invalid identities during initialisation
     *
     * @param keysArrays - String array containing identities.
     */
    private void init(final String[] keysArrays) {
        if (keysArrays != null && keysArrays.length > 0) {
            for (String key : keysArrays) {
                if (Utils.containsIgnoreCase(Constants.ALL_IDENTITY_KEYS, key)) {
                    identities.add(Utils.convertToTitleCase(key));
                }
            }
        }
    }

    /**
     * Prepares Identity set from comma separated string
     * String            Set
     * e.g email,phone => {email, phone}
     *
     * @param keysCommaSeparated - comma separated key string
     * @return IdentitySet containing valid identities.
     */
    static IdentitySet from(String keysCommaSeparated) {
        return new IdentitySet(keysCommaSeparated.split(Constants.SEPARATOR_COMMA));
    }

    /**
     * Prepares Identity set from array of string values
     * String array            Set
     * e.g [email,phone] => {email, phone}
     *
     * @param keysArray - array of string values
     * @return IdentitySet containing valid identities.
     */
    static IdentitySet from(String[] keysArray) {
        return new IdentitySet(keysArray);
    }

    /**
     * Returns Default Identity Set which is set to legacy identity set {@link Constants#LEGACY_IDENTITY_KEYS}
     */
    static IdentitySet getDefault() {
        return new IdentitySet(Constants.LEGACY_IDENTITY_KEYS);
    }
}
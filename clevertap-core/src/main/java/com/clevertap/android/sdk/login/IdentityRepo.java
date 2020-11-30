package com.clevertap.android.sdk.login;

import androidx.annotation.NonNull;

/**
 * Interface to provide Identities related functionality
 */
public interface IdentityRepo {

    IdentitySet getIdentitySet();

    /**
     * checks if a given key is an identity or not
     *
     * @param Key - String value of key
     * @return - true , if the given key is an identity key else false.
     */
    boolean hasIdentity(@NonNull String Key);

}
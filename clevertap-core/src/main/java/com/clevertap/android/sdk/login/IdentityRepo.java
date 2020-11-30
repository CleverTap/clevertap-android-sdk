package com.clevertap.android.sdk.login;

import androidx.annotation.NonNull;

/**
 * Handler Interface to provide Identities related functionality
 */
public interface IdentityRepo {

    /**
     * checks if a given key is an identity or not
     * @param Key - String value of key
     * @return - true , if the given key is an identity key else false.
     */
    boolean isIdentity(@NonNull String Key);

    IdentitySet identities();

}
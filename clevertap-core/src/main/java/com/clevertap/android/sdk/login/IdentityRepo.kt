package com.clevertap.android.sdk.login

/**
 * Interface to provide Identities related functionality
 */
internal interface IdentityRepo {

    val identitySet: IdentitySet

    /**
     * checks if a given key is an identity or not
     *
     * @param Key - String value of key
     * @return - true , if the given key is an identity key else false.
     */
    fun hasIdentity(Key: String): Boolean
}

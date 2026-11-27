package com.clevertap.android.sdk.login

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Utils
import androidx.annotation.RestrictTo

/**
 * Wrapper class of identities & related behaviors
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class IdentitySet private constructor(private val identities: Set<String>) {

    /**
     * Checks if two identity sets contain same identities
     *
     * @param other - the second object to which we are comparing
     * @return - true if all entries are same else false
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        return identities == (other as IdentitySet).identities
    }

    // Identity-based, as in the Java original (inconsistent with equals; kept as is).
    override fun hashCode(): Int = super.hashCode()

    /**
     * Stringifies the identity set in comma separated string value.
     * Set           String
     * e.g   {Email, Phone} =>   Email,Phone
     *
     * @return String value of the identity set.
     */
    override fun toString(): String =
        identities.filter { it in Constants.ALL_IDENTITY_KEYS }.joinToString(Constants.SEPARATOR_COMMA)

    /**
     * checks if a given key is an identity or not
     *
     * @param key - String value of key
     * @return - true , if the given key is an identity key else false.
     */
    fun contains(key: String?): Boolean = Utils.containsIgnoreCase(identities, key)

    /**
     * @return true - if the set contains at least one of the valid identities [Constants.ALL_IDENTITY_KEYS]
     */
    val isValid: Boolean
        get() = identities.isNotEmpty()

    companion object {

        /**
         * Prepares Identity set from comma separated string
         * String            Set
         * e.g email,phone => {email, phone}
         *
         * @param keysCommaSeparated - comma separated key string
         * @return IdentitySet containing valid identities.
         */
        fun from(keysCommaSeparated: String): IdentitySet {
            // Kotlin's split keeps trailing empty strings (Java's regex split drops them); empty keys are
            // filtered out as invalid, so the resulting set is identical.
            return from(keysCommaSeparated.split(Constants.SEPARATOR_COMMA).toTypedArray())
        }

        /**
         * Prepares Identity set from array of string values, filtering out invalid identities.
         * String array            Set
         * e.g [email,phone] => {Email, Phone}
         *
         * @param keysArray - array of string values
         * @return IdentitySet containing valid identities.
         */
        fun from(keysArray: Array<String>?): IdentitySet {
            val identities = keysArray.orEmpty()
                .filter { Utils.containsIgnoreCase(Constants.ALL_IDENTITY_KEYS, it) }
                .mapNotNullTo(HashSet()) { Utils.convertToTitleCase(it) }
            return IdentitySet(identities)
        }

        /**
         * Returns a new Default Identity Set which is set to legacy identity set [Constants.LEGACY_IDENTITY_KEYS]
         */
        fun default(): IdentitySet = IdentitySet(HashSet(Constants.LEGACY_IDENTITY_KEYS))
    }
}

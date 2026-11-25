package com.clevertap.android.sdk.login

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Utils

/**
 * Wrapper class of identities & related behaviors
 */
internal open class IdentitySet {

    private val identities = HashSet<String>()

    private constructor(keys: Array<String>?) {
        init(keys)
    }

    private constructor(hashSet: HashSet<String>) {
        identities.addAll(hashSet)
    }

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
        val that = other as IdentitySet
        return identities == that.identities
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    /**
     * Stringifies the identity set in comma separated string value.
     * Set           String
     * e.g   {Email, Phone} =>   Email,Phone
     *
     * @return String value of the identity set.
     */
    override fun toString(): String {
        val stringBuilder = StringBuilder()
        val iterator = identities.iterator()
        while (iterator.hasNext()) {
            val type = iterator.next()
            if (Constants.ALL_IDENTITY_KEYS.contains(type)) {
                stringBuilder.append(type)
                    .append(if (iterator.hasNext()) Constants.SEPARATOR_COMMA else "")
            }
        }
        return stringBuilder.toString()
    }

    /**
     * checks if a given key is an identity or not
     *
     * @param Key - String value of key
     * @return - true , if the given key is an identity key else false.
     */
    fun contains(Key: String?): Boolean {
        return Utils.containsIgnoreCase(identities, Key)
    }

    /**
     * @return true - if the set contains at least one of the valid identities [Constants.ALL_IDENTITY_KEYS]
     */
    val isValid: Boolean
        get() = identities.isNotEmpty()

    /**
     * Initialises the identity set using the string array items.
     * It also filters out invalid identities during initialisation
     *
     * @param keysArrays - String array containing identities.
     */
    private fun init(keysArrays: Array<String>?) {
        if (keysArrays != null && keysArrays.isNotEmpty()) {
            for (key in keysArrays) {
                if (Utils.containsIgnoreCase(Constants.ALL_IDENTITY_KEYS, key)) {
                    identities.add(Utils.convertToTitleCase(key))
                }
            }
        }
    }

    companion object {

        /**
         * Prepares Identity set from comma separated string
         * String            Set
         * e.g email,phone => {email, phone}
         *
         * @param keysCommaSeparated - comma separated key string
         * @return IdentitySet containing valid identities.
         */
        @JvmStatic
        fun from(keysCommaSeparated: String): IdentitySet {
            // Kotlin's split keeps trailing empty strings (Java's regex split drops them); empty keys are
            // filtered out by init(), so the resulting set is identical.
            return IdentitySet(keysCommaSeparated.split(Constants.SEPARATOR_COMMA).toTypedArray())
        }

        /**
         * Prepares Identity set from array of string values
         * String array            Set
         * e.g [email,phone] => {email, phone}
         *
         * @param keysArray - array of string values
         * @return IdentitySet containing valid identities.
         */
        @JvmStatic
        fun from(keysArray: Array<String>?): IdentitySet {
            return IdentitySet(keysArray)
        }

        /**
         * Returns Default Identity Set which is set to legacy identity set [Constants.LEGACY_IDENTITY_KEYS]
         */
        @JvmStatic
        fun getDefault(): IdentitySet {
            return IdentitySet(Constants.LEGACY_IDENTITY_KEYS)
        }
    }
}

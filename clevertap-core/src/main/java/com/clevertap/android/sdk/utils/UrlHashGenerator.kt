package com.clevertap.android.sdk.utils

import java.util.UUID

/**
 * Returns unique UUID for a url key, if there is failure in generating uuid then we simply call
 * hashcode for the url
 */
object UrlHashGenerator {

    fun hash() : (key: String) -> String = { key ->
        var nameUUIDFromBytes: UUID? = null
        try {
            nameUUIDFromBytes = UUID.nameUUIDFromBytes(key.toByteArray())
        } catch (e: InternalError) {
            key.hashCode().toString()
        }
        nameUUIDFromBytes?.toString()?: key.hashCode().toString()
    }

    fun hashWithTsSeed(): String =
            hash().invoke(java.lang.String.valueOf(System.currentTimeMillis()))
}
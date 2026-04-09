package com.clevertap.android.sdk.network

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.utils.Clock
import java.security.SecureRandom

internal class NetworkRepo(
    val context: Context,
    val config: CleverTapInstanceConfig,
    val generateRandomDelay: () -> Int = {
        val randomGen = SecureRandom()
        (randomGen.nextInt(10) + 1) * 1000
    },
    val clock: Clock = Clock.SYSTEM
) {

    companion object {
        const val KEY_DOMAIN_NAME: String = "comms_dmn"
        const val SPIKY_KEY_DOMAIN_NAME: String = "comms_dmn_spiky"
        const val KEY_LAST_TS: String = "comms_last_ts"
        const val KEY_FIRST_TS: String = "comms_first_ts"
        const val PUSH_DELAY_MS: Int = 1000
        const val MAX_DELAY_FREQUENCY: Int = 1000 * 60 * 10
    }

    fun getFirstRequestTs() : Int {
        return StorageHelper.getIntFromPrefs(
            context,
            config.accountId,
            KEY_FIRST_TS,
            0
        )
    }
    fun setFirstRequestTs(firstRequestTs: Int) {
        StorageHelper.putInt(
            context,
            config.accountId,
            KEY_FIRST_TS,
            firstRequestTs
        )
    }

    fun clearFirstRequestTs() {
        StorageHelper.putInt(
            context,
            config.accountId,
            KEY_FIRST_TS,
            0
        )
    }

    fun setLastRequestTs(lastRequestTs: Int) {
        StorageHelper.putInt(
            context,
            config.accountId,
            KEY_LAST_TS,
            lastRequestTs
        )
    }

    fun clearLastRequestTs() {
        StorageHelper.putInt(
            context,
            config.accountId,
            KEY_LAST_TS,
            0
        )
    }

    fun getLastRequestTs() : Int {
        return StorageHelper.getIntFromPrefs(
            context,
            config.accountId,
            KEY_LAST_TS,
            0
        )
    }

    /**
     * @return true if the current time is before the mute expiry timestamp.
     * Also handles migration from the old SDK's comms_mtd key (epoch seconds)
     * to the new comms_mute_expiry_ts key (absolute epoch milliseconds).
     */
    fun isMuted() : Boolean {
        val now = clock.currentTimeMillis()
        val muteExpiryMs = getMuteExpiry()
        if (muteExpiryMs > 0) {
            return now < muteExpiryMs
        }
        // Migration: check old key for an active mute from a previous SDK version
        @Suppress("DEPRECATION")
        val legacyMuteTs = getMuted()
        if (legacyMuteTs > 0) {
            val legacyExpiryMs = (legacyMuteTs + 24 * 60 * 60) * 1000L
            if (now < legacyExpiryMs) {
                // Migrate to new format so this path is only hit once
                setMuteExpiry(legacyExpiryMs)
                return true
            }
        }
        return false
    }

    /**
     * @return the mute expiry timestamp in epoch milliseconds, or 0 if not muted.
     */
    fun getMuteExpiry() : Long {
        return StorageHelper.getLongFromPrefs(
            context,
            config.accountId,
            Constants.KEY_MUTE_EXPIRY,
            0L,
            null
        )
    }

    /**
     * @return timestamp from epoch since SDK was muted in seconds.
     * @deprecated Use [getMuteExpiry] instead. Retained for migration from older SDK versions.
     */
    @Deprecated("Use getMuteExpiry() instead")
    fun getMuted() : Int {
        return StorageHelper.getIntFromPrefs(
            context,
            config.accountId,
            Constants.KEY_MUTED,
            0
        )
    }

    /**
     * Sets the mute state with the default 24-hour duration.
     * When muted, stores an absolute expiry timestamp (now + 24h) in milliseconds.
     */
    fun setMuted(mute: Boolean) {
        if (mute) {
            val expiryMs = clock.currentTimeMillis() + Constants.DEFAULT_MUTE_DURATION_MS
            setMuteExpiry(expiryMs)
        } else {
            setMuteExpiry(0L)
        }
    }

    /**
     * Sets the mute expiry to a specific absolute epoch timestamp in milliseconds.
     * The backend sends this value via the X-WZRK-MUTE-DURATION header.
     */
    fun setMuteExpiry(expiryMs: Long) {
        StorageHelper.putLong(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_MUTE_EXPIRY),
            expiryMs
        )
    }

    /**
     * Clears any active mute state, allowing the SDK to resume
     * normal event tracking and network operations immediately.
     */
    fun unmute() {
        setMuteExpiry(0L)
    }

    fun setDomain(domainName: String?) {
        StorageHelper.putString(
            context,
            config.accountId,
            KEY_DOMAIN_NAME,
            domainName
        )
    }

    fun getDomain() : String? {
        return StorageHelper.getStringFromPrefs(
            context,
            config.accountId,
            KEY_DOMAIN_NAME,
            null
        )
    }

    fun getSpikyDomain() : String? {
        return StorageHelper.getStringFromPrefs(
            context,
            config.accountId,
            SPIKY_KEY_DOMAIN_NAME,
            null
        )
    }

    fun setSpikyDomain(spikyDomainName: String) {
        StorageHelper.putString(
            context,
            config.accountId,
            SPIKY_KEY_DOMAIN_NAME,
            spikyDomainName
        )
    }

    fun getMinDelayFrequency(
        currentDelay: Int,
        networkRetryCount: Int
    ) : Int {
        config.logger.debug(config.accountId, "Network retry #$networkRetryCount")

        //Retry with delay as 1s for first 10 retries
        if (networkRetryCount < 10) {
            config.logger.debug(
                config.accountId,
                "Failure count is $networkRetryCount. Setting delay frequency to 1s"
            )
            return PUSH_DELAY_MS
        }

        if (config.accountRegion == null) {
            //Retry with delay as 1s if region is null in case of eu1
            config.logger.debug(config.accountId, "Setting delay frequency to 1s")
            return PUSH_DELAY_MS
        } else {
            //Retry with delay as minimum delay frequency and add random number of seconds to scatter traffic
            val  delayBy = currentDelay + generateRandomDelay()
            if (delayBy < MAX_DELAY_FREQUENCY) {
                config.logger.debug(
                    config.accountId,
                    "Setting delay frequency to $currentDelay"
                )
                return delayBy
            } else {
                return PUSH_DELAY_MS
            }
        }
    }
}
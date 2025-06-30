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
            config,
            KEY_FIRST_TS,
            0
        )
    }
    fun setFirstRequestTs(firstRequestTs: Int) {
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, KEY_FIRST_TS),
            firstRequestTs
        )
    }

    fun clearFirstRequestTs() {
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, KEY_FIRST_TS),
            0
        )
    }

    fun setLastRequestTs(lastRequestTs: Int) {
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, KEY_LAST_TS),
            lastRequestTs
        )
    }

    fun clearLastRequestTs() {
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, KEY_LAST_TS),
            0
        )
    }

    fun getLastRequestTs() : Int {
        return StorageHelper.getIntFromPrefs(
            context,
            config,
            KEY_LAST_TS,
            0
        )
    }

    /**
     * @return true if the mute command was sent anytime between now and now - 24 hours.
     */
    fun isMuted() : Boolean {
        val now = clock.currentTimeSecondsInt()
        val muteTS = getMuted()
        return now - muteTS < 24 * 60 * 60
    }

    /**
     * @return timestamp from epoch since SDK was muted in seconds.
     */
    fun getMuted() : Int {
        return StorageHelper.getIntFromPrefs(
            context,
            config,
            Constants.KEY_MUTED,
            0
        )
    }

    fun setMuted(mute: Boolean) {
        if (mute) {
            val now = clock.currentTimeSecondsInt()
            StorageHelper.putInt(
                context,
                StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_MUTED),
                now
            )
        } else {
            StorageHelper.putInt(
                context,
                StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_MUTED),
                0
            )
        }
    }

    fun setDomain(domainName: String?) {
        StorageHelper.putString(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, KEY_DOMAIN_NAME),
            domainName
        )
    }

    fun getDomain() : String? {
        return StorageHelper.getStringFromPrefs(
            context,
            config,
            KEY_DOMAIN_NAME,
            null
        )
    }

    fun getSpikyDomain() : String? {
        return StorageHelper.getStringFromPrefs(
            context,
            config,
            SPIKY_KEY_DOMAIN_NAME,
            null
        )
    }

    fun setSpikyDomain(spikyDomainName: String) {
        StorageHelper.putString(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, SPIKY_KEY_DOMAIN_NAME),
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
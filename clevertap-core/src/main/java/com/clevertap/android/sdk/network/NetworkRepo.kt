package com.clevertap.android.sdk.network

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import java.security.SecureRandom

internal class NetworkRepo(
    val context: Context,
    val config: CleverTapInstanceConfig,
    val generateRandomDelay: () -> Int = {
        val randomGen = SecureRandom()
        (randomGen.nextInt(10) + 1) * 1000
    }
) {

    fun getFirstRequestTs() : Int {
        return StorageHelper.getIntFromPrefs(
            context,
            config,
            Constants.KEY_FIRST_TS,
            0
        )
    }
    fun setFirstRequestTs(firstRequestTs: Int) {
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_FIRST_TS),
            firstRequestTs
        )
    }

    fun clearFirstRequestTs() {
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_FIRST_TS),
            0
        )
    }

    fun setLastRequestTs(lastRequestTs: Int) {
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_LAST_TS),
            lastRequestTs
        )
    }

    fun clearLastRequestTs() {
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_LAST_TS),
            0
        )
    }

    fun getLastRequestTs() : Int {
        return StorageHelper.getIntFromPrefs(
            context,
            config,
            Constants.KEY_LAST_TS,
            0
        )
    }

    fun setMuted(mute: Boolean) {
        if (mute) {
            val now = (System.currentTimeMillis() / 1000).toInt()
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
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_DOMAIN_NAME),
            domainName
        )
    }

    fun getDomain() : String? {
        return StorageHelper.getStringFromPrefs(
            context,
            config,
            Constants.KEY_DOMAIN_NAME,
            null
        )
    }

    fun getSpikyDomain() : String? {
        return StorageHelper.getStringFromPrefs(
            context,
            config,
            Constants.SPIKY_KEY_DOMAIN_NAME,
            null
        )
    }

    fun setSpikyDomain(spikyDomainName: String) {
        StorageHelper.putString(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, Constants.SPIKY_KEY_DOMAIN_NAME),
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
            return Constants.PUSH_DELAY_MS
        }

        if (config.accountRegion == null) {
            //Retry with delay as 1s if region is null in case of eu1
            config.logger.debug(config.accountId, "Setting delay frequency to 1s")
            return Constants.PUSH_DELAY_MS
        } else {
            //Retry with delay as minimum delay frequency and add random number of seconds to scatter traffic
            val  delayBy = currentDelay + generateRandomDelay()
            if (delayBy < Constants.MAX_DELAY_FREQUENCY) {
                config.logger.debug(
                    config.accountId,
                    "Setting delay frequency to $currentDelay"
                )
                return delayBy
            } else {
                return Constants.PUSH_DELAY_MS
            }
        }
    }
}
package com.clevertap.android.sdk.inapp.delay

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.store.db.DelayedLegacyInAppStore
import com.clevertap.android.sdk.utils.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.plus

@OptIn(ExperimentalCoroutinesApi::class)
internal object InAppSchedulerFactory {

    fun createDelayedInAppScheduler(
        accountId: String,
        logger: Logger,
        delayedLegacyInAppStore: DelayedLegacyInAppStore?=null,
        clock: Clock = Clock.SYSTEM,
        lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
    ): InAppScheduler<DelayedInAppResult> {

        // Single scope for both timer and scheduler operations
        val sharedScope = ProcessLifecycleOwner.get().lifecycleScope +
                Dispatchers.Default.limitedParallelism(20)

        val timerManager = InAppTimerManager(
            accountId,
            logger,
            clock,
            sharedScope,
            lifecycleOwner
        )
        val storageStrategy = DelayedInAppStorageStrategy(accountId, logger, delayedLegacyInAppStore)
        val dataExtractor = DelayedInAppDataExtractor()

        return InAppScheduler(
            timerManager,
            storageStrategy,
            dataExtractor,
            logger,
            accountId,
            sharedScope // Reuse the same scope
        )
    }

    fun createInActionScheduler(
        accountId: String,
        logger: Logger,
        clock: Clock = Clock.SYSTEM,
        lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
    ): InAppScheduler<InActionResult> {

        // Single scope for both timer and scheduler operations
        val sharedScope = ProcessLifecycleOwner.get().lifecycleScope +
                Dispatchers.Default.limitedParallelism(20) //TODO check if this is really shared or there will be 20 + 20 parallelism

        val timerManager = InAppTimerManager(
            accountId,
            logger,
            clock,
            sharedScope,
            lifecycleOwner
        )
        val storageStrategy = InActionStorageStrategy(logger, accountId)
        val dataExtractor = InActionDataExtractor()

        return InAppScheduler(
            timerManager,
            storageStrategy,
            dataExtractor,
            logger,
            accountId,
            sharedScope // Reuse the same scope
        )
    }
}
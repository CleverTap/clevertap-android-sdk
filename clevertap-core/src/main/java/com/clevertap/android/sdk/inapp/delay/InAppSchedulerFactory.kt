package com.clevertap.android.sdk.inapp.delay

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.store.db.DelayedLegacyInAppStore
import com.clevertap.android.sdk.utils.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.plus

@OptIn(ExperimentalCoroutinesApi::class)
internal object InAppSchedulerFactory {

    private const val PARALLEL_SCHEDULERS = 20

    fun createDelayedInAppScheduler(
        accountId: String,
        logger: ILogger,
        delayedLegacyInAppStore: DelayedLegacyInAppStore?=null,
        clock: Clock = Clock.SYSTEM,
        lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
        scope: CoroutineScope =  ProcessLifecycleOwner.get().lifecycleScope +
                Dispatchers.Default.limitedParallelism(PARALLEL_SCHEDULERS)
    ): InAppScheduler<DelayedInAppResult> {

        val timerManager = InAppTimerManager(
            accountId,
            logger,
            clock,
            scope,
            lifecycleOwner,
            "Delayed"
        )
        val storageStrategy = DelayedInAppStorageStrategy(accountId, logger, delayedLegacyInAppStore)
        val dataExtractor = DelayedInAppDataExtractor()

        return InAppScheduler(
            timerManager,
            storageStrategy,
            dataExtractor,
            logger,
            accountId
        )
    }

    fun createInActionScheduler(
        accountId: String,
        logger: ILogger,
        clock: Clock = Clock.SYSTEM,
        lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
        scope: CoroutineScope =  ProcessLifecycleOwner.get().lifecycleScope +
                Dispatchers.Default.limitedParallelism(PARALLEL_SCHEDULERS)
    ): InAppScheduler<InActionResult> {
         //TODO check if this is really shared or there will be 20 + 20 parallelism

        val timerManager = InAppTimerManager(
            accountId,
            logger,
            clock,
            scope,
            lifecycleOwner,
            "InAction"
        )
        val storageStrategy = InActionStorageStrategy(logger, accountId)
        val dataExtractor = InActionDataExtractor()

        return InAppScheduler(
            timerManager,
            storageStrategy,
            dataExtractor,
            logger,
            accountId
        )
    }
}
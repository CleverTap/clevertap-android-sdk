package com.clevertap.android.sdk

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.annotation.RestrictTo

/**
 * Notifies [onAppBackground] when the whole app moves to background.
 *
 * Backed by [ProcessLifecycleOwner] (same owner used by the in-app delay schedulers), whose ON_STOP
 * is dispatched only when no activity is started, so activity transitions and configuration changes
 * do not trigger it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AppBackgroundObserver(
    private val lifecycleOwner: LifecycleOwner,
    private val accountId: String,
    private val logger: ILogger,
    private val onAppBackground: () -> Unit
) : DefaultLifecycleObserver {

    fun register() {
        // lifecycleScope runs on Dispatchers.Main.immediate, as Lifecycle#addObserver requires the main thread
        // The try also covers lifecycleScope and launch, which throw when no Main dispatcher is available
        try {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    lifecycleOwner.lifecycle.addObserver(this@AppBackgroundObserver)
                } catch (t: Throwable) {
                    logger.verbose(accountId, "Failed to register app background observer", t)
                }
            }
        } catch (t: Throwable) {
            logger.verbose(accountId, "Failed to register app background observer", t)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        logger.verbose(accountId, "App moved to background")
        try {
            onAppBackground()
        } catch (t: Throwable) {
            logger.verbose(accountId, "Failed to handle app background", t)
        }
    }
}

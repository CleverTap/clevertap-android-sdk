package com.clevertap.android.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

import androidx.activity.OnBackPressedCallback
import androidx.annotation.RestrictTo
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity

import com.clevertap.android.sdk.inapp.CTInAppAction
import com.clevertap.android.sdk.inapp.InAppDisplayListener
import com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFullFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlCoverFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlHalfInterstitialFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlInterstitialFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeCoverFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeCoverImageFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeHalfInterstitialFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeHalfInterstitialImageFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeInterstitialFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeInterstitialImageFragment
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.CTInAppNotificationButton
import com.clevertap.android.sdk.inapp.CTInAppType
import com.clevertap.android.sdk.inapp.InAppActionType
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.InAppListener

import java.lang.ref.WeakReference

/**
 * Hosts full-screen in-app notifications and the push-permission prompt.
 *
 * Declared in the SDK manifest (keep the FQN). Kotlin `internal`: it implements SDK-internal
 * interfaces and is instantiated only by the framework, so it stays JVM-public.
 */
internal class InAppNotificationActivity : FragmentActivity(), InAppListener,
    DidClickForHardPermissionListener, PushPermissionHandler.PushPermissionResultCallback, InAppDisplayListener,
    InAppController.RestoredInAppListener {

    // Set by setUp; stays null while an in-app is being restored, or when setUp gave up early.
    private var config: CleverTapInstanceConfig? = null

    private var inAppNotification: CTInAppNotification? = null

    private var listenerWeakReference = WeakReference<InAppListener>(null)

    private var pushPermissionHandler: PushPermissionHandler? = null

    private var invokedCallbacks = false

    private val restoreTimeoutHandler = Handler(Looper.getMainLooper())

    private var restoreFinished = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        // getIntent() and getDisplayingInApp (a static field, null in a fresh process) are both
        // readable before super.onCreate, so the restore case can be decided first.
        val restoring = isInAppMissing()
        // When restoring, pass null to super so Android does not recreate the full-screen fragment
        // it saved for this activity. That fragment's in-app is no longer in memory, and recreating
        // it would run onCreateView against an unset in-app. restoreInApp adds a fresh fragment once
        // the in-app has been read back from storage.
        super.onCreate(if (restoring) null else savedInstanceState)
        if (restoring) {
            // The in-app is no longer in memory, e.g. Android recreated this activity after the app
            // process was killed. Try to show the in-app again from storage; close if that cannot
            // start.
            if (!restoreInApp()) {
                Logger.v(
                    "In-app ${intent.getStringExtra(Constants.INAPP_ID_KEY)} is no longer available, " +
                        "closing InAppNotificationActivity"
                )
                finish()
            }
            return
        }
        setUp(savedInstanceState)
    }

    private fun setUp(savedInstanceState: Bundle?) {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                didDismiss(null)
            }
        })

        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        val (inAppConfig, notification) = try {
            val intentExtras = intent.extras ?: throw IllegalArgumentException()

            val intentConfig = intentExtras.getParcelable<CleverTapInstanceConfig>(INTENT_EXTRA_CT_CONFIG)
                ?: throw IllegalArgumentException()
            config = intentConfig

            // Pass the application context: instanceWithConfig may construct a CleverTapAPI that
            // retains the Context for the instance's (long) lifetime, so handing it an Activity
            // context would leak this Activity.
            val cleverTapAPI = checkNotNull(CleverTapAPI.instanceWithConfig(applicationContext, intentConfig)) {
                "No CleverTap instance for the in-app config"
            }
            val ctState = cleverTapAPI.coreState

            pushPermissionHandler = PushPermissionHandler(
                intentConfig,
                ctState.callbackManager.pushPermissionResponseListenerList,
                this
            )

            if (intentExtras.getBoolean(INTENT_EXTRA_DISPLAY_PUSH_PERMISSION_PROMPT, false)) {
                showPushPermissionPrompt(
                    intentExtras.getBoolean(INTENT_EXTRA_PUSH_PERMISSION_FALLBACK_TO_SETTINGS, false)
                )
                return
            }

            listenerWeakReference = WeakReference(ctState.inAppController)
            ctState.inAppController.registerInAppDisplayListener(this)

            val inAppId = intentExtras.getString(Constants.INAPP_ID_KEY)
            val inApp = InAppController.getDisplayingInApp(inAppId)
            inAppNotification = inApp
            if (inApp == null) {
                // Not in memory (e.g. the app process was killed): close without tracking.
                intentConfig.logger.verbose(
                    intentConfig.accountId,
                    "In-app $inAppId is no longer available, closing InAppNotificationActivity"
                )
                finish()
                return
            }
            intentConfig.logger.verbose(
                intentConfig.accountId,
                "InAppNotificationActivity loaded in-app $inAppId from memory"
            )
            intentConfig to inApp
        } catch (t: Throwable) {
            Logger.v("Cannot find a valid notification bundle to show!", t)
            finish()
            return
        }

        //Allow rotation for all InApps but respect the flags sent from dashboard
        if (notification.isPortrait && !notification.isLandscape) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Logger.d("App in Landscape, dismissing portrait InApp Notification")
                finish()
                didDismiss(null)
                return
            } else {
                Logger.d("App in Portrait, displaying InApp Notification anyway")
            }
        }

        if (!notification.isPortrait && notification.isLandscape) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                Logger.d("App in Portrait, dismissing landscape InApp Notification")
                finish()
                didDismiss(null)
                return
            } else {
                Logger.d("App in Landscape, displaying InApp Notification anyway")
            }
        }

        if (savedInstanceState == null) {
            createContentFragment(notification, inAppConfig)?.let { contentFragment ->
                contentFragment.setArguments(notification, inAppConfig)
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                    .add(android.R.id.content, contentFragment, fragmentTag(inAppConfig))
                    .commitNow()
            }
        } else if (isAlertVisible) {
            createContentFragment(notification, inAppConfig)
        }
    }

    override fun onResume() {
        super.onResume()
        pushPermissionHandler?.onActivityResume(this) // null while an in-app is being restored
    }

    /**
     * Starts showing the in-app again from storage after the app process was killed.
     *
     * @return false when it cannot start, so the activity should close.
     */
    private fun restoreInApp(): Boolean {
        // After a process kill this is often the call that creates the SDK instance for the new
        // process. As in setUp, any error closes the screen instead of crashing the app.
        return try {
            val extras = intent.extras ?: return false
            val inAppId = extras.getString(Constants.INAPP_ID_KEY) ?: return false
            val restoreConfig = extras.getParcelable<CleverTapInstanceConfig>(INTENT_EXTRA_CT_CONFIG) ?: return false
            val cleverTapAPI = CleverTapAPI.instanceWithConfig(applicationContext, restoreConfig) ?: return false
            Logger.v("In-app $inAppId is not in memory, restoring it from storage")
            // This window is transparent and has no content yet, but it still covers the app and
            // would take every tap. Let taps through to the app until the in-app is ready.
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            restoreTimeoutHandler.postDelayed({ onRestoreTimeout() }, RESTORE_TIMEOUT_MS)
            cleverTapAPI.coreState.inAppController.restoreDisplayedInApp(inAppId, this)
            true
        } catch (t: Throwable) {
            Logger.v("Could not start restoring the in-app, closing InAppNotificationActivity", t)
            false
        }
    }

    /**
     * Result of [InAppController.restoreDisplayedInApp]. The activity is the listener itself,
     * so the controller can hold it weakly.
     */
    override fun onRestoreResult(restoredInApp: CTInAppNotification?): Boolean {
        if (restoreFinished || isFinishing || isDestroyed) {
            return false
        }
        restoreFinished = true
        restoreTimeoutHandler.removeCallbacksAndMessages(null)
        if (restoredInApp == null || supportFragmentManager.isStateSaved) {
            Logger.v(
                "In-app ${intent.getStringExtra(Constants.INAPP_ID_KEY)} could not be restored, " +
                    "closing InAppNotificationActivity"
            )
            finish()
            return false
        }
        // The restored in-app is now the one on screen, so setUp finds it in memory. Pass no saved
        // state so a new fragment is added; the one Android restored removed itself.
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        setUp(null)
        // setUp closes the activity in some cases (e.g. a portrait-only in-app in landscape). When it
        // did so through didDismiss, the controller already frees the in-app slot on that path, so
        // true keeps it from doing it twice. Otherwise false makes the controller free the slot.
        return !isFinishing || invokedCallbacks
    }

    private fun onRestoreTimeout() {
        if (restoreFinished) {
            return
        }
        restoreFinished = true
        Logger.v(
            "Restoring in-app ${intent.getStringExtra(Constants.INAPP_ID_KEY)} took too long, " +
                "closing InAppNotificationActivity"
        )
        finish()
    }

    @SuppressLint("WrongConstant")
    override fun finish() {
        super.finish()
        if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun inAppNotificationDidClick(
        inAppNotification: CTInAppNotification,
        button: CTInAppNotificationButton,
        buttonIndex: Int,
        activityContext: Context?
    ): Bundle? = currentListener()?.inAppNotificationDidClick(inAppNotification, button, buttonIndex, this)

    override fun inAppNotificationDidDismiss(inAppNotification: CTInAppNotification, formData: Bundle?) {
        didDismiss(formData)
    }

    override fun inAppNotificationDidShow(inAppNotification: CTInAppNotification, formData: Bundle?) {
        didShow(formData)
    }

    override fun inAppNotificationActionTriggered(
        inAppNotification: CTInAppNotification,
        action: CTInAppAction,
        callToAction: String,
        additionalData: Bundle?,
        activityContext: Context?
    ): Bundle? = currentListener()?.inAppNotificationActionTriggered(
        inAppNotification,
        action,
        callToAction,
        additionalData,
        this
    )

    override fun didClickForHardPermissionWithFallbackSettings(fallbackToSettings: Boolean) {
        showPushPermissionPrompt(fallbackToSettings)
    }

    // pushPermissionHandler is null only before setUp created it (in-app being restored, or setUp gave up).
    override fun didCancelPermissionRequest() {
        pushPermissionHandler?.notifyPushPermissionExternalListeners(this)
    }

    fun showPushPermissionPrompt(isFallbackSettingsEnabled: Boolean) {
        pushPermissionHandler?.requestPermission(this, isFallbackSettingsEnabled)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        pushPermissionHandler?.onRequestPermissionsResult(this, requestCode, grantResults)
    }

    override fun onPushPermissionResult(isGranted: Boolean) {
        val data = inAppNotification?.takeIf { it.isLocalInApp }?.let { notification ->
            Bundle().apply {
                putString(Constants.KEY_C2A, notification.buttons[0].text)
                putString(Constants.NOTIFICATION_ID_TAG, "")
            }
        }
        didDismiss(data)
    }

    fun didDismiss(data: Bundle?, killActivity: Boolean = true) {
        if (isAlertVisible) {
            isAlertVisible = false
        }

        if (!invokedCallbacks) {
            val listener = currentListener()
            val notification = inAppNotification
            if (listener != null && notification != null) {
                listener.inAppNotificationDidDismiss(notification, data)
            }
            invokedCallbacks = true
        }
        if (killActivity) {
            finish()
        }
    }

    private fun didShow(data: Bundle?) {
        val listener = currentListener() ?: return
        // The listener is only set together with the in-app in setUp.
        val notification = inAppNotification ?: return
        listener.inAppNotificationDidShow(notification, data)
    }

    private fun currentListener(): InAppListener? {
        val listener = listenerWeakReference.get()
        val notification = inAppNotification
        if (listener == null && notification != null) {
            config?.let {
                it.logger.verbose(
                    it.accountId,
                    "InAppActivityListener is null for notification: ${notification.jsonDescription}"
                )
            }
        }
        return listener
    }

    private fun didClick(
        notification: CTInAppNotification,
        button: CTInAppNotificationButton,
        buttonIndex: Int
    ): Bundle? = currentListener()?.inAppNotificationDidClick(notification, button, buttonIndex, this)

    private fun createContentFragment(
        notification: CTInAppNotification,
        config: CleverTapInstanceConfig
    ): CTInAppBaseFullFragment? = when (val type = notification.inAppType) {
        CTInAppType.CTInAppTypeCoverHTML -> CTInAppHtmlCoverFragment()
        CTInAppType.CTInAppTypeInterstitialHTML -> CTInAppHtmlInterstitialFragment()
        CTInAppType.CTInAppTypeHalfInterstitialHTML -> CTInAppHtmlHalfInterstitialFragment()
        CTInAppType.CTInAppTypeCover -> CTInAppNativeCoverFragment()
        CTInAppType.CTInAppTypeInterstitial -> CTInAppNativeInterstitialFragment()
        CTInAppType.CTInAppTypeHalfInterstitial -> CTInAppNativeHalfInterstitialFragment()
        CTInAppType.CTInAppTypeCoverImageOnly -> CTInAppNativeCoverImageFragment()
        CTInAppType.CTInAppTypeInterstitialImageOnly -> CTInAppNativeInterstitialImageFragment()
        CTInAppType.CTInAppTypeHalfInterstitialImageOnly -> CTInAppNativeHalfInterstitialImageFragment()
        CTInAppType.CTInAppTypeAlert -> {
            showAlertDialogForInApp(notification, config)
            null
        }
        else -> {
            config.logger.verbose("InAppNotificationActivity: Unhandled InApp Type: $type")
            null
        }
    }

    private fun fragmentTag(config: CleverTapInstanceConfig): String = "${config.accountId}:CT_INAPP_CONTENT_FRAGMENT"

    /**
     * True when this activity was opened for an in-app that is no longer in memory, which happens
     * when Android recreates it after the app process was killed.
     */
    private fun isInAppMissing(): Boolean {
        val extras = intent.extras
        if (extras == null || !extras.containsKey(Constants.INAPP_ID_KEY)) {
            return false
        }
        return InAppController.getDisplayingInApp(extras.getString(Constants.INAPP_ID_KEY)) == null
    }

    private fun showAlertDialogForInApp(notification: CTInAppNotification, config: CleverTapInstanceConfig) {
        val buttons = notification.buttons
        if (buttons.isEmpty()) {
            config.logger.debug("InAppNotificationActivity: Notification has no buttons, not showing Alert InApp")
            return
        }
        val positiveButton = buttons[0]

        val alertDialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
            .setCancelable(false)
            .setTitle(notification.title)
            .setMessage(notification.message)
            .setPositiveButton(positiveButton.text) { _, _ ->
                onAlertButtonClick(notification, positiveButton, 0, true)
            }
            .create()

        if (buttons.size == 2) {
            val negativeButton = buttons[1]
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, negativeButton.text) { _, _ ->
                onAlertButtonClick(notification, negativeButton, 1, false)
            }
        }

        //By default, we will allow 2 button alerts and set a third button if it is configured
        if (buttons.size > 2) {
            val button = buttons[2]
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, button.text) { _, _ ->
                onAlertButtonClickLegacy(notification, button, 2)
            }
        }

        alertDialog.show()
        isAlertVisible = true
        didShow(null)
    }

    private fun onAlertButtonClickLegacy(
        notification: CTInAppNotification,
        button: CTInAppNotificationButton,
        buttonIndex: Int
    ) {
        didDismiss(didClick(notification, button, buttonIndex))
    }

    private fun onAlertButtonClick(
        notification: CTInAppNotification,
        button: CTInAppNotificationButton,
        buttonIndex: Int,
        isPositive: Boolean
    ) {
        val clickData = didClick(notification, button, buttonIndex)

        if (notification.isLocalInApp) {
            if (isPositive) {
                showPushPermissionPrompt(notification.fallBackToNotificationSettings)
                return
            } else {
                didCancelPermissionRequest()
            }
        }

        val action = button.action
        if (action != null && InAppActionType.REQUEST_FOR_PERMISSIONS == action.type) {
            showPushPermissionPrompt(action.shouldFallbackToSettings)
            return
        }

        didDismiss(clickData)
    }

    override fun hideInApp() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        restoreTimeoutHandler.removeCallbacksAndMessages(null)

        config?.let { currentConfig ->
            CleverTapAPI.instanceWithConfig(applicationContext, currentConfig)
                ?.coreState
                ?.inAppController
                ?.unregisterInAppDisplayListener()
        }
        if (!isChangingConfigurations) {
            didDismiss(null, false)
        }
    }

    companion object {
        private const val INTENT_EXTRA_DISPLAY_PUSH_PERMISSION_PROMPT = "displayPushPermissionPrompt"
        private const val INTENT_EXTRA_PUSH_PERMISSION_FALLBACK_TO_SETTINGS = "shouldShowFallbackSettings"
        private const val INTENT_EXTRA_CT_CONFIG = "config"

        private var isAlertVisible = false

        // Restoring the in-app after a process kill: the activity has no content until it is loaded.
        private const val RESTORE_TIMEOUT_MS: Long = 4000

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun launchForPushPermissionPrompt(
            activity: Activity,
            config: CleverTapInstanceConfig?,
            showFallbackSettings: Boolean
        ) {
            if (activity !is InAppNotificationActivity) {
                val intent = Intent(activity, InAppNotificationActivity::class.java)
                    .putExtra(INTENT_EXTRA_CT_CONFIG, config)
                    .putExtra(INTENT_EXTRA_DISPLAY_PUSH_PERMISSION_PROMPT, true)
                    .putExtra(INTENT_EXTRA_PUSH_PERMISSION_FALLBACK_TO_SETTINGS, showFallbackSettings)
                activity.startActivity(intent)
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun launchForInAppNotification(
            context: Context,
            inAppNotification: CTInAppNotification,
            config: CleverTapInstanceConfig
        ) {
            // Only the id is sent; the in-app is read from InAppController. Sending the whole object
            // can exceed the Binder limit for large HTML (TransactionTooLargeException, SDK-6299).
            val intent = Intent(context, InAppNotificationActivity::class.java)
                .putExtra(Constants.INAPP_ID_KEY, inAppNotification.campaignId)
                .putExtra(INTENT_EXTRA_CT_CONFIG, config)
            config.logger.verbose(
                config.accountId,
                "Launching InAppNotificationActivity with in-app id: ${inAppNotification.campaignId}"
            )
            context.startActivity(intent)
        }
    }
}

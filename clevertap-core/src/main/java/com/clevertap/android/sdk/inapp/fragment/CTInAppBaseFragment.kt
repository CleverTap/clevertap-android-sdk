package com.clevertap.android.sdk.inapp.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.DidClickForHardPermissionListener
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.customviews.CloseImageView
import com.clevertap.android.sdk.inapp.CTInAppAction
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.CTInAppNotificationButton
import com.clevertap.android.sdk.inapp.InAppActionParser
import com.clevertap.android.sdk.inapp.InAppActionType
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.InAppWebInteraction
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.media.InAppMediaHandler
import com.clevertap.android.sdk.inapp.media.NoOpMediaHandler

import java.lang.ref.WeakReference

internal abstract class CTInAppBaseFragment : Fragment(), InAppWebInteraction {

    companion object {
        private const val KEY_ACTIVE_MEDIA_URL = "ct_active_media_url"

        fun showOnActivity(
            inAppFragment: CTInAppBaseFragment,
            activity: Activity,
            inAppNotification: CTInAppNotification,
            config: CleverTapInstanceConfig,
            logTag: String
        ): Boolean {
            try {
                val fragmentTransaction =
                    (activity as FragmentActivity).supportFragmentManager.beginTransaction()
                inAppFragment.setArguments(inAppNotification, config)
                fragmentTransaction.setCustomAnimations(
                    android.R.animator.fade_in, android.R.animator.fade_out
                )
                fragmentTransaction.add(
                    android.R.id.content, inAppFragment, inAppNotification.type
                )
                Logger.v(logTag, "calling InAppFragment " + inAppNotification.campaignId)
                fragmentTransaction.commitNow()
                return true
            } catch (e: ClassCastException) {
                Logger.v(
                    logTag,
                    "Fragment not able to render, please ensure your Activity is an instance of AppCompatActivity",
                    e
                )
                return false
            } catch (t: Throwable) {
                Logger.v(logTag, "Fragment not able to render", t)
                return false
            }
        }
    }

    protected inner class CTInAppNativeButtonClickListener : View.OnClickListener {

        override fun onClick(view: View) {
            val index = view.tag as? Int ?: return
            handleButtonClickAtIndex(index)
        }
    }

    protected lateinit var inAppNotification: CTInAppNotification
    protected lateinit var config: CleverTapInstanceConfig
    protected var currentOrientation: Int = 0
    protected var closeImageView: CloseImageView? = null
    protected var activeMediaUrl: String? = null
    protected lateinit var mediaHandler: InAppMediaHandler
    private var listenerWeakReference: WeakReference<InAppListener>? = null
    private var didClickForHardPermissionListener: DidClickForHardPermissionListener? = null

    protected abstract fun cleanup()
    protected abstract fun generateListener()

    protected open fun createMediaHandler(): InAppMediaHandler = NoOpMediaHandler

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = arguments
        if (bundle != null) {
            inAppNotification = bundle.getParcelable<CTInAppNotification>(Constants.INAPP_KEY)!!
            config = bundle.getParcelable<CleverTapInstanceConfig>(Constants.KEY_CONFIG)!!
            currentOrientation = resources.configuration.orientation
            generateListener()/*Initialize the below listener only when in app has InAppNotification activity as their host activity
            when requesting permission for notification.*/
            if (context is DidClickForHardPermissionListener) {
                didClickForHardPermissionListener = context
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeMediaUrl = savedInstanceState?.getString(KEY_ACTIVE_MEDIA_URL)
            ?: InAppMediaHandler.resolveMediaUrl(inAppNotification, currentOrientation)
        mediaHandler = createMediaHandler()
        lifecycle.addObserver(mediaHandler)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        activeMediaUrl?.let { outState.putString(KEY_ACTIVE_MEDIA_URL, it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setAccessibilityPaneTitle(view, getString(R.string.ct_inapp_message_shown))
        didShow(null)
    }

    fun setArguments(inAppNotification: CTInAppNotification, config: CleverTapInstanceConfig) {
        val bundle = Bundle()
        bundle.putParcelable(Constants.INAPP_KEY, inAppNotification)
        bundle.putParcelable(Constants.KEY_CONFIG, config)
        setArguments(bundle)
    }

    override fun triggerAction(
        action: CTInAppAction, callToAction: String?, additionalData: Bundle?
    ) {
        val parsed = InAppActionParser.parse(action, callToAction, additionalData, config)
        val actionData = notifyActionTriggered(parsed.action, parsed.callToAction ?: "", parsed.additionalData)
        didDismiss(actionData)
    }

    override fun openActionUrl(url: String) {
        triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, null)
    }

    /**
     * Close (X) button dismissal, raised as a click: `wzrk_element_id = closeButton`,
     * `wzrk_c2a = Dismiss Button`, `wzrk_action = close`, `wzrk_data = close`.
     */
    fun triggerCloseButtonAction() {
        val extras = Bundle().apply {
            putString(Constants.KEY_WZRK_ELEMENT_ID, Constants.INAPP_ELEMENT_ID_CLOSE)
        }
        triggerAction(
            CTInAppAction.CREATOR.createCloseAction(), Constants.INAPP_CTA_DISMISS_BUTTON, extras
        )
    }

    /**
     * Swipe-to-dismiss, raised as a click: `wzrk_c2a = Swipe to Dismiss`, `wzrk_action = close`,
     * `wzrk_data = close`. No `wzrk_element_id` (gesture, not an element).
     */
    fun triggerSwipeDismissAction() {
        triggerAction(
            CTInAppAction.CREATOR.createCloseAction(), Constants.INAPP_CTA_SWIPE_DISMISS, null
        )
    }

    /**
     * The swipe/pan dismiss gesture is enabled only when there is no close button and the campaign
     * allows swipe-to-dismiss. When disabled, the gesture must not be attached at all.
     */
    protected fun isSwipeToDismissEnabled(): Boolean =
        !inAppNotification.isShowClose && inAppNotification.swipeToDismiss

    override fun didDismiss(data: Bundle?) {
        cleanup()
        getListener()?.inAppNotificationDidDismiss(inAppNotification, data)
    }

    fun didShow(data: Bundle?) {
        getListener()?.inAppNotificationDidShow(inAppNotification, data)
    }


    fun getListener(): InAppListener? {
        val listener = listenerWeakReference?.get()
        if (listener == null) {
            config.logger.verbose(
                config.accountId,
                "InAppListener is null for notification: ${inAppNotification.jsonDescription}"
            )
        }
        return listener
    }

    fun setListener(listener: InAppListener) {
        listenerWeakReference = WeakReference<InAppListener>(listener)
    }

    fun getScaledPixels(raw: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, raw.toFloat(), resources.displayMetrics
        ).toInt()
    }

    fun handleButtonClickAtIndex(index: Int) {
        try {
            val button = inAppNotification.buttons[index]
            val clickData = didClick(button, index)

            if (inAppNotification.isLocalInApp && didClickForHardPermissionListener != null) {
                when (index) {
                    0 -> {
                        didClickForHardPermissionListener?.didClickForHardPermissionWithFallbackSettings(
                            inAppNotification.fallBackToNotificationSettings
                        )
                        return
                    }
                    1 -> {
                        didClickForHardPermissionListener?.didCancelPermissionRequest()
                    }
                }
            }

            val action = button.action
            if (action != null && InAppActionType.REQUEST_FOR_PERMISSIONS == action.type && didClickForHardPermissionListener != null) {
                didClickForHardPermissionListener?.didClickForHardPermissionWithFallbackSettings(
                    action.shouldFallbackToSettings
                )
                return
            }

            didDismiss(clickData)
        } catch (t: Throwable) {
            config.logger.debug("Error handling notification button click", t)
            didDismiss(null)
        }
    }

    fun resourceProvider(): FileResourceProvider {
        return FileResourceProvider.getInstance(requireContext(), config.logger)
    }

    private fun didClick(button: CTInAppNotificationButton, index: Int): Bundle? {
        var action = button.action
        if (action == null) {
            action = CTInAppAction.CREATOR.createCloseAction()
        }
        // Whole-image tap on image-only templates is tagged image-1; otherwise it is a 1-based CTA button.
        val isImageTap = inAppNotification.isImageOnlyInApp()
        val elementId = if (isImageTap) {
            Constants.INAPP_ELEMENT_ID_IMAGE
        } else {
            Constants.INAPP_ELEMENT_ID_BUTTON_PREFIX + (index + 1)
        }
        val extras = Bundle().apply { putString(Constants.KEY_WZRK_ELEMENT_ID, elementId) }
        // wzrk_c2a: for a whole-image tap use the element id ("image-1"); for a CTA button use its text.
        val callToAction = if (isImageTap) elementId else button.text
        return notifyActionTriggered(action, callToAction, extras)
    }

    private fun notifyActionTriggered(
        action: CTInAppAction, callToAction: String, additionalData: Bundle?
    ): Bundle? {
        return getListener()?.inAppNotificationActionTriggered(
            inAppNotification, action, callToAction, additionalData, activity
        )
    }
}

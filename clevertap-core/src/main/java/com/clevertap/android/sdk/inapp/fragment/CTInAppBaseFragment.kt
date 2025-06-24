package com.clevertap.android.sdk.inapp.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.DidClickForHardPermissionListener
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.customviews.CloseImageView
import com.clevertap.android.sdk.inapp.CTInAppAction
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.CTInAppNotificationButton
import com.clevertap.android.sdk.inapp.InAppActionType
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.utils.UriHelper

import java.lang.ref.WeakReference
import java.net.URLDecoder

internal abstract class CTInAppBaseFragment : Fragment() {

    companion object {
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
    private var listenerWeakReference: WeakReference<InAppListener>? = null
    private var didClickForHardPermissionListener: DidClickForHardPermissionListener? = null

    protected abstract fun cleanup()
    protected abstract fun generateListener()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        didShow(null)
    }

    fun setArguments(inAppNotification: CTInAppNotification, config: CleverTapInstanceConfig) {
        val bundle = Bundle()
        bundle.putParcelable(Constants.INAPP_KEY, inAppNotification)
        bundle.putParcelable(Constants.KEY_CONFIG, config)
        setArguments(bundle)
    }

    fun triggerAction(
        action: CTInAppAction, callToAction: String?, additionalData: Bundle?
    ) {
        var additionalData = additionalData
        var action = action
        var callToAction = callToAction
        if (action.type == InAppActionType.OPEN_URL) {
            //All URL parameters should be tracked as additional data
            val urlActionData = UriHelper.getAllKeyValuePairs(action.actionUrl, false)

            // callToAction is handled as a parameter
            var callToActionUrlParam = urlActionData.getString(Constants.KEY_C2A)
            // no need to keep it in the data bundle
            urlActionData.remove(Constants.KEY_C2A)

            // add all additional params, overriding the url params if there is a collision
            if (additionalData != null) {
                urlActionData.putAll(additionalData)
            }
            // Use the merged data for the action
            additionalData = urlActionData
            if (callToActionUrlParam != null) {
                // check if there is a deeplink within the callToAction param
                val parts = callToActionUrlParam.split(Constants.URL_PARAM_DL_SEPARATOR)
                if (parts.size == 2) {
                    // Decode it here as it is not decoded by UriHelper
                    try {
                        // Extract the actual callToAction value
                        callToActionUrlParam = URLDecoder.decode(parts[0], "UTF-8")
                    } catch (e: Exception) {
                        config.logger.debug("Error parsing c2a param", e)
                    }
                    // use the url from the callToAction param
                    action = CTInAppAction.CREATOR.createOpenUrlAction(parts[1])
                }
            }
            if (callToAction == null) {
                // Use the url param value only if no other value is passed
                callToAction = callToActionUrlParam
            }
        }
        val actionData = notifyActionTriggered(action, callToAction ?: "", additionalData)
        didDismiss(actionData)
    }

    fun openActionUrl(url: String) {
        triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, null)
    }

    fun didDismiss(data: Bundle?) {
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
            val clickData = didClick(button)

            if (index == 0 && inAppNotification.isLocalInApp && didClickForHardPermissionListener != null) {
                didClickForHardPermissionListener?.didClickForHardPermissionWithFallbackSettings(
                    inAppNotification.fallBackToNotificationSettings
                )
                return
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

    private fun didClick(button: CTInAppNotificationButton): Bundle? {
        var action = button.action
        if (action == null) {
            action = CTInAppAction.CREATOR.createCloseAction()
        }
        return notifyActionTriggered(action, button.text, null)
    }

    private fun notifyActionTriggered(
        action: CTInAppAction, callToAction: String, additionalData: Bundle?
    ): Bundle? {
        return getListener()?.inAppNotificationActionTriggered(
            inAppNotification, action, callToAction, additionalData, activity
        )
    }
}

package com.clevertap.android.sdk.inapp.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.DidClickForHardPermissionListener
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.customviews.CloseImageView
import com.clevertap.android.sdk.inapp.CTInAppAction
import com.clevertap.android.sdk.inapp.CTInAppHost
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.CTInAppNotificationButton
import com.clevertap.android.sdk.inapp.InAppActionType
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import java.lang.ref.WeakReference

internal abstract class CTInAppBaseFragment : Fragment(), CTInAppHost.Callbacks {

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
    private var didClickForHardPermissionListener: DidClickForHardPermissionListener? = null
    internal lateinit var inAppHost: CTInAppHost

    protected abstract fun cleanup()
    protected abstract fun generateListener()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = arguments
        if (bundle != null) {
            inAppNotification = bundle.getParcelable(Constants.INAPP_KEY)!!
            config = bundle.getParcelable(Constants.KEY_CONFIG)!!
            currentOrientation = resources.configuration.orientation
            inAppHost = CTInAppHost(null, config, inAppNotification, this, context)
            generateListener()
            /*Initialize the below listener only when in app has InAppNotification activity as their host activity
            when requesting permission for notification.*/
            if (context is DidClickForHardPermissionListener) {
                didClickForHardPermissionListener = context
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inAppHost.didShowInApp(null)
    }

    fun setArguments(inAppNotification: CTInAppNotification, config: CleverTapInstanceConfig) {
        val bundle = Bundle()
        bundle.putParcelable(Constants.INAPP_KEY, inAppNotification)
        bundle.putParcelable(Constants.KEY_CONFIG, config)
        setArguments(bundle)
    }

    fun didDismiss(data: Bundle?) {
        inAppHost.didDismissInApp(data)
    }

    override fun onDismissInApp() {
        cleanup()
    }

    fun setListener(listener: InAppListener) {
        inAppHost.setInAppListener(listener)
    }

    fun getScaledPixels(raw: Int): Int {
        return inAppHost.getScaledPixels(raw)
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
        return inAppHost.notifyActionTriggered(action, button.text, null)
    }
}

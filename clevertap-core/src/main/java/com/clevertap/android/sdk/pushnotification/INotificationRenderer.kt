package com.clevertap.android.sdk.pushnotification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.Utils
import java.util.Random
import org.json.JSONArray

/**
 * Renders a CleverTap push notification. Pass an implementation to
 * [com.clevertap.android.sdk.CleverTapAPI.renderPushNotification].
 */
interface INotificationRenderer {

    fun getCollapseKey(extras: Bundle): Any?

    fun getMessage(extras: Bundle): String?

    fun getTitle(extras: Bundle, context: Context): String?

    fun renderNotification(
        extras: Bundle,
        context: Context,
        nb: NotificationCompat.Builder,
        config: CleverTapInstanceConfig,
        notificationId: Int
    ): NotificationCompat.Builder?

    fun setSmallIcon(smallIcon: Int, context: Context)

    fun getActionButtonIconKey(): String

    fun setActionButtons(
        context: Context?,
        extras: Bundle?,
        notificationId: Int,
        nb: NotificationCompat.Builder,
        actions: JSONArray?
    ): NotificationCompat.Builder {
        val intentServiceName = ManifestInfo.getInstance(context).intentServiceName
        var clazz: Class<*>? = null
        if (intentServiceName != null) {
            try {
                clazz = Class.forName(intentServiceName)
            } catch (e: ClassNotFoundException) {
                try {
                    clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
                } catch (ex: ClassNotFoundException) {
                    Logger.d("No Intent Service found")
                }
            }
        } else {
            try {
                clazz = Class.forName("com.clevertap.android.sdk.pushnotification.CTNotificationIntentService")
            } catch (ex: ClassNotFoundException) {
                Logger.d("No Intent Service found")
            }
        }

        val isCTIntentServiceAvailable = Utils.isServiceAvailable(context!!, clazz)

        if (actions != null && actions.length() > 0) {
            for (i in 0 until actions.length()) {
                try {
                    val action = actions.getJSONObject(i)
                    val label = action.optString("l")
                    val dl = action.optString("dl")
                    val ico = action.optString(getActionButtonIconKey())
                    val id = action.optString("id")
                    val autoCancel = action.optBoolean("ac", true)
                    if (label.isEmpty() || id.isEmpty()) {
                        Logger.d("not adding push notification action: action label or id missing")
                        continue
                    }
                    var icon = 0
                    if (ico.isNotEmpty()) {
                        try {
                            icon = context.resources.getIdentifier(ico, "drawable", context.packageName)
                        } catch (t: Throwable) {
                            Logger.d("unable to add notification action icon: " + t.localizedMessage)
                        }
                    }

                    var sendToCTIntentService = (VERSION.SDK_INT < VERSION_CODES.S && autoCancel
                            && isCTIntentServiceAvailable)

                    val dismissOnClick = extras!!.getString("pt_dismiss_on_click")
                    // Send to CTIntentService in case (OS >= S) and notif is for Push templates with remind action
                    if (!sendToCTIntentService && PushNotificationHandler.isForPushTemplates(extras)
                        && id.contains("remind") && dismissOnClick != null
                        && dismissOnClick.equals("true", ignoreCase = true) && autoCancel
                        && isCTIntentServiceAvailable
                    ) {
                        sendToCTIntentService = true
                    }

                    // Send to CTIntentService in case (OS >= S) and notif is for Push templates with
                    // pt_dismiss_on_click true
                    if (!sendToCTIntentService && PushNotificationHandler.isForPushTemplates(extras)
                        && dismissOnClick != null && dismissOnClick.equals("true", ignoreCase = true)
                        && autoCancel && isCTIntentServiceAvailable
                    ) {
                        sendToCTIntentService = true
                    }

                    val actionLaunchIntent: Intent?
                    if (sendToCTIntentService) {
                        actionLaunchIntent = Intent(CTNotificationIntentService.MAIN_ACTION)
                        actionLaunchIntent.setPackage(context.packageName)
                        actionLaunchIntent.putExtra(Constants.KEY_CT_TYPE, CTNotificationIntentService.TYPE_BUTTON_CLICK)
                        if (dl.isNotEmpty()) {
                            actionLaunchIntent.putExtra("dl", dl)
                        }
                    } else {
                        if (dl.isNotEmpty()) {
                            actionLaunchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(dl))
                            Utils.setPackageNameFromResolveInfoList(context, actionLaunchIntent)
                        } else {
                            actionLaunchIntent = context.packageManager
                                .getLaunchIntentForPackage(context.packageName)
                        }
                    }

                    if (actionLaunchIntent != null) {
                        actionLaunchIntent.putExtras(extras)
                        actionLaunchIntent.removeExtra(Constants.WZRK_ACTIONS)
                        actionLaunchIntent.putExtra("actionId", id)
                        actionLaunchIntent.putExtra("autoCancel", autoCancel)
                        actionLaunchIntent.putExtra("wzrk_c2a", id)
                        actionLaunchIntent.putExtra("notificationId", notificationId)

                        actionLaunchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }

                    val flagsActionLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

                    val requestCode = Random().nextInt()
                    val actionIntent: PendingIntent? = if (sendToCTIntentService) {
                        PendingIntent.getService(
                            context, requestCode,
                            actionLaunchIntent!!, flagsActionLaunchPendingIntent
                        )
                    } else {
                        // Java passed a possibly-null launch intent here; getActivity throws on null,
                        // which the surrounding catch logs, exactly as before.
                        PendingIntent.getActivity(
                            context, requestCode,
                            actionLaunchIntent!!, flagsActionLaunchPendingIntent, null
                        )
                    }
                    nb.addAction(icon, label, actionIntent)
                } catch (t: Throwable) {
                    Logger.d("error adding notification action : " + t.localizedMessage)
                }
            }
        } // Uncommon - END

        return nb
    }
}

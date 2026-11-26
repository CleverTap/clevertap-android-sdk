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
        fun classOrNull(name: String): Class<*>? = try {
            Class.forName(name)
        } catch (e: ClassNotFoundException) {
            null
        }

        val ctIntentServiceName = "com.clevertap.android.sdk.pushnotification.CTNotificationIntentService"
        val intentServiceName = ManifestInfo.getInstance(context).intentServiceName
        val clazz = intentServiceName?.let(::classOrNull)
            ?: classOrNull(ctIntentServiceName)
            ?: run {
                Logger.d("No Intent Service found")
                null
            }

        // Java dereferenced a null context here as well (NPE).
        val isCTIntentServiceAvailable = Utils.isServiceAvailable(context!!, clazz)

        if (actions == null || actions.length() == 0) {
            return nb
        }
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
                        Logger.d("unable to add notification action icon: ${t.localizedMessage}")
                    }
                }

                // A null extras threw a NullPointerException here in Java, logged by the catch below.
                val dismissOnClick = extras!!.getString("pt_dismiss_on_click")
                val dismissOnClickForTemplates = PushNotificationHandler.isForPushTemplates(extras) &&
                        dismissOnClick.equals("true", ignoreCase = true) &&
                        autoCancel && isCTIntentServiceAvailable

                // Send to CTIntentService below S, or (OS >= S) when the notif is for Push templates
                // with pt_dismiss_on_click true (including the remind action)
                val sendToCTIntentService =
                    (VERSION.SDK_INT < VERSION_CODES.S && autoCancel && isCTIntentServiceAvailable) ||
                            dismissOnClickForTemplates

                val actionLaunchIntent: Intent? = when {
                    sendToCTIntentService -> Intent(CTNotificationIntentService.MAIN_ACTION).apply {
                        setPackage(context.packageName)
                        putExtra(Constants.KEY_CT_TYPE, CTNotificationIntentService.TYPE_BUTTON_CLICK)
                        if (dl.isNotEmpty()) {
                            putExtra("dl", dl)
                        }
                    }

                    dl.isNotEmpty() -> Intent(Intent.ACTION_VIEW, Uri.parse(dl)).also {
                        Utils.setPackageNameFromResolveInfoList(context, it)
                    }

                    else -> context.packageManager.getLaunchIntentForPackage(context.packageName)
                }

                actionLaunchIntent?.apply {
                    putExtras(extras)
                    removeExtra(Constants.WZRK_ACTIONS)
                    putExtra("actionId", id)
                    putExtra("autoCancel", autoCancel)
                    putExtra("wzrk_c2a", id)
                    putExtra("notificationId", notificationId)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

                val flagsActionLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

                val requestCode = Random().nextInt()
                // Java passed a possibly-null launch intent to getActivity, which threw on null; the
                // surrounding catch logs that, exactly as before.
                val launchIntent = actionLaunchIntent!!
                val actionIntent: PendingIntent? = if (sendToCTIntentService) {
                    PendingIntent.getService(context, requestCode, launchIntent, flagsActionLaunchPendingIntent)
                } else {
                    PendingIntent.getActivity(
                        context, requestCode,
                        launchIntent, flagsActionLaunchPendingIntent, null
                    )
                }
                nb.addAction(icon, label, actionIntent)
            } catch (t: Throwable) {
                Logger.d("error adding notification action : ${t.localizedMessage}")
            }
        }

        return nb
    }
}

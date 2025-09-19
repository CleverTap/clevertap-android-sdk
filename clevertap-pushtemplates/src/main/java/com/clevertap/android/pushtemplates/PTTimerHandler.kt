package com.clevertap.android.pushtemplates

import android.content.Context
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.clevertap.android.pushtemplates.PTConstants.ONE_SECOND
import com.clevertap.android.pushtemplates.PTConstants.PT_BIG_IMG
import com.clevertap.android.pushtemplates.PTConstants.PT_BIG_IMG_ALT_TEXT
import com.clevertap.android.pushtemplates.PTConstants.PT_COLLAPSE_KEY
import com.clevertap.android.pushtemplates.PTConstants.PT_GIF
import com.clevertap.android.pushtemplates.PTConstants.PT_ID
import com.clevertap.android.pushtemplates.PTConstants.PT_JSON
import com.clevertap.android.pushtemplates.PTConstants.PT_MSG
import com.clevertap.android.pushtemplates.PTConstants.PT_MSG_SUMMARY
import com.clevertap.android.pushtemplates.PTConstants.PT_TIMER_END
import com.clevertap.android.pushtemplates.PTConstants.PT_TIMER_MIN_THRESHOLD
import com.clevertap.android.pushtemplates.PTConstants.PT_TITLE
import com.clevertap.android.pushtemplates.TemplateDataFactory.toBasicTemplateData
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.pushnotification.INotificationRenderer
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import org.json.JSONObject

internal object PTTimerHandler {

    @RequiresApi(VERSION_CODES.M)
    internal fun scheduleTimer(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        delay: Int?,
        data: TimerTemplateData,
        config: CleverTapInstanceConfig,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        if (delay == null) {
            return
        }
        handler.postDelayed(
            {
                if (Utils.isNotificationInTray(
                        context,
                        notificationId
                    )
                    && ValidatorFactory.getValidator(data.toBasicTemplateData())
                        ?.validate() == true
                ) {
                    val applicationContext = context.applicationContext
                    val basicTemplateBundle = extras.clone() as Bundle
                    basicTemplateBundle.remove("wzrk_rnv")
                    basicTemplateBundle.putString(
                        Constants.WZRK_PUSH_ID,
                        null
                    ) // skip dupe check
                    basicTemplateBundle.putString(
                        PT_ID,
                        "pt_basic"
                    ) // set to basic


                    /**
                     *  Update existing payload bundle with new title,msg,img for Basic template
                     */
                    val ptJsonStr = basicTemplateBundle.getString(PT_JSON)
                    val ptJsonObj = try {
                        ptJsonStr?.let { JSONObject(it) }
                    } catch (_: Exception) {
                        Logger.v("Unable to convert JSON to String")
                        null
                    } ?: JSONObject()

                    with(ptJsonObj) {
                        put(PT_TITLE, data.terminalTextData.title)
                        put(PT_BIG_IMG, data.mediaData.bigImage.url)
                        put(
                            PT_BIG_IMG_ALT_TEXT,
                            data.mediaData.bigImage.altText
                        )
                        put(PT_MSG, data.terminalTextData.message)
                        put(
                            PT_MSG_SUMMARY,
                            data.terminalTextData.messageSummary
                        )
                        put(PT_GIF, data.mediaData.gif.url)
                    }

                    basicTemplateBundle.putString(PT_JSON, ptJsonObj.toString())
                    // force random id generation
                    basicTemplateBundle.putString(PT_COLLAPSE_KEY, null)
                    basicTemplateBundle.putString(Constants.WZRK_COLLAPSE, null)
                    basicTemplateBundle.remove(Constants.PT_NOTIF_ID)
                    val templateRenderer: INotificationRenderer =
                        TemplateRenderer(
                            applicationContext,
                            basicTemplateBundle,
                            config
                        )
                    val cleverTapAPI = CleverTapAPI
                        .getGlobalInstance(
                            applicationContext,
                            PushNotificationUtil.getAccountIdFromNotificationBundle(
                                basicTemplateBundle
                            )
                        )
                    cleverTapAPI?.renderPushNotification(
                        templateRenderer,
                        applicationContext,
                        basicTemplateBundle
                    )
                }
            }, (delay - 100).toLong()
        )
    }


    internal fun getTimerEnd(data: TimerTemplateData): Int? {
        var timer_end: Int? = null
        if (data.timerThreshold != -1 && data.timerThreshold >= PT_TIMER_MIN_THRESHOLD) {
            timer_end = data.timerThreshold * ONE_SECOND + ONE_SECOND
        } else if (data.timerEnd >= PT_TIMER_MIN_THRESHOLD) {
            timer_end = data.timerEnd * ONE_SECOND + ONE_SECOND
        } else {
            PTLog.debug("Not rendering notification Timer End value lesser than threshold (10 seconds) from current time: $PT_TIMER_END")
        }
        return timer_end
    }
}
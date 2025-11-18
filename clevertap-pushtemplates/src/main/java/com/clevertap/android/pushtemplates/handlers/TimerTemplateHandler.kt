package com.clevertap.android.pushtemplates.handlers

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.TemplateDataFactory.toBasicTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TimerTemplateData
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.INotificationRenderer
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil

internal object TimerTemplateHandler {

    @RequiresApi(Build.VERSION_CODES.M)
    internal fun scheduleTimer(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        delay: Long?,
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
                        PTConstants.PT_ID,
                        "pt_basic"
                    ) // set to basic


                    basicTemplateBundle.putString(PTConstants.PT_TITLE, data.terminalTextData.title)
                    basicTemplateBundle.putString(
                        PTConstants.PT_BIG_IMG,
                        data.terminalMediaData.bigImage.url
                    )
                    basicTemplateBundle.putString(
                        PTConstants.PT_BIG_IMG_ALT_TEXT,
                        data.terminalMediaData.bigImage.altText
                    )
                    basicTemplateBundle.putString(PTConstants.PT_MSG, data.terminalTextData.message)
                    basicTemplateBundle.putString(
                        PTConstants.PT_MSG_SUMMARY,
                        data.terminalTextData.messageSummary
                    )
                    basicTemplateBundle.putString(
                        PTConstants.PT_GIF,
                        data.terminalMediaData.gif.url
                    )

                    basicTemplateBundle.putString(
                        PTConstants.PT_GIF_FRAMES,
                        data.terminalMediaData.gif.numberOfFrames.toString()
                    )

                    basicTemplateBundle.putString(
                        PTConstants.PT_SCALE_TYPE,
                        data.terminalMediaData.scaleType.toString()
                    )


                    basicTemplateBundle.remove(PTConstants.PT_JSON)

                    // force random id generation
                    basicTemplateBundle.putString(PTConstants.PT_COLLAPSE_KEY, null)
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
            }, (delay - 100)
        )
    }


    internal fun getDismissAfterMs(timerEnd: Int, timerThreshold: Int): Long? {
        var timer_end: Long? = null
        if (timerThreshold != -1 && timerThreshold >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
            timer_end = timerThreshold * PTConstants.ONE_SECOND_LONG + PTConstants.ONE_SECOND_LONG
        } else if (timerEnd >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
            timer_end = timerEnd * PTConstants.ONE_SECOND_LONG + PTConstants.ONE_SECOND_LONG
        } else {
            PTLog.debug("Not rendering notification Timer End value lesser than threshold (10 seconds) from current time: ${PTConstants.PT_TIMER_END}")
        }
        return timer_end
    }
}
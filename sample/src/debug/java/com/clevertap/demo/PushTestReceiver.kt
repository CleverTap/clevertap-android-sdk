package com.clevertap.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Local test hook: renders a push-template payload straight from `adb shell am broadcast` extras
 * so templates can be checked on a device without a campaign.
 *
 * It has to be exported for the adb shell to reach it, which also means any app on the device can
 * drive it. That is why it lives in the debug source set, declared in `src/debug/AndroidManifest.xml`
 * and nowhere else: a release build never contains it.
 *
 *   adb shell "am broadcast -n com.clevertap.demo/.PushTestReceiver \
 *     --es pt_id pt_basic --es nt Title --es nm Message --es pt_big_img 'https://...'"
 *
 * For anything longer than a couple of keys, [LocalPushActivity] takes the same payload as JSON.
 */
class PushTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras ?: return
        LocalPushSender.render(context, extras)
    }
}

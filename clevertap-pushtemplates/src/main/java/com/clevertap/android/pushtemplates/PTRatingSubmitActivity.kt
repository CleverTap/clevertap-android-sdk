package com.clevertap.android.pushtemplates

import android.app.Activity
import android.os.Bundle

/**
 * Invisible activity behind the pt_custom_rating submit button.
 *
 * Since Android 12 a notification tap that lands in a service or a broadcast receiver may not start
 * an activity, which is exactly what submitting a rating has to do — record the rating, then open
 * the campaign's destination. Google's documented answer is a trampoline the app owns: the tap opens
 * this activity directly, and an activity is allowed to start another one.
 *
 * It draws nothing, keeps no state and finishes in [onCreate], so the user only ever sees the
 * destination the campaign asked for.
 */
class PTRatingSubmitActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val extras = intent?.extras
            if (extras == null) {
                PTLog.verbose("Custom rating submit activity started without extras, nothing to submit")
            } else {
                CustomRatingSubmitHandler.submit(this, extras)
            }
        } catch (t: Throwable) {
            PTLog.verbose("Error submitting the custom rating", t)
        } finally {
            finish()
        }
    }
}

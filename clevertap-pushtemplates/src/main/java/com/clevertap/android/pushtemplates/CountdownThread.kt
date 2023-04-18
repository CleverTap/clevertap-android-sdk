package com.clevertap.android.pushtemplates

/**
 * Creates a thread which simulates countdown with a second interval. Provides a callback along with
 * tick count and also provides a callback when the countdown is finished.
 *
 * @param seconds  - Takes the value of countdown in seconds
 * @param tickCallback - Gives a callback on every tick along with the tick count
 * @param finishCallback - Gives a callback when the countdown is finished
 */
class CountdownThread(private val seconds: Int, private val tickCallback: (Int) -> Unit,
                      private val finishCallback: () -> Unit) : Thread() {

        private var tickCount = 0

        override fun run() {
            for (i in seconds downTo 1) {
                tickCount++
                tickCallback(tickCount)
                try {
                    sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            finishCallback()
        }
}
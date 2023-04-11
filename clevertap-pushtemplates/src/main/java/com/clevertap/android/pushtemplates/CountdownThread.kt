package com.clevertap.android.pushtemplates

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
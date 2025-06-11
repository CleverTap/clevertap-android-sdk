package com.clevertap.android.sdk.utils

import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Interface to allow injecting of time functionality into a class for easier unit testing.
 *
 * Use SYSTEM instance to delegate functionality to System.currentTimeMillis() and default Date
 * constructor.
 */
interface Clock {

  /**
   * Gets current time in milliseconds.
   *
   * @return The current time in milliseconds.
   */
  fun currentTimeMillis(): Long

  /**
   * Gets current time in seconds.
   *
   * @return The current time in seconds.
   */
  fun currentTimeSeconds(): Long {
    return TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis())
  }

  fun currentTimeSecondsInt(): Int {
    return (currentTimeMillis() / 1000).toInt()
  }

  /**
   * Creates instance of type Date.
   *
   * @return New instance of Date.
   */
  fun newDate(): Date

  companion object {
    val SYSTEM = object : Clock {
      override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
      }
      override fun newDate(): Date {
        return Date()
      }
    }
  }
}

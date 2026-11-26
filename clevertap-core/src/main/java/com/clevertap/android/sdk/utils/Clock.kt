package com.clevertap.android.sdk.utils

import androidx.annotation.RestrictTo
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Interface to allow injecting of time functionality into a class for easier unit testing.
 *
 * Use SYSTEM instance to delegate functionality to System.currentTimeMillis() and default Date
 * constructor.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Clock {

  /**
   * Gets current time in milliseconds.
   *
   * @return The current time in milliseconds.
   */
  public fun currentTimeMillis(): Long

  /**
   * Gets current time in seconds.
   *
   * @return The current time in seconds.
   */
  public fun currentTimeSeconds(): Long {
    return TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis())
  }

  public fun currentTimeSecondsInt(): Int {
    return (currentTimeMillis() / 1000).toInt()
  }

  /**
   * Creates instance of type Date.
   *
   * @return New instance of Date.
   */
  public fun newDate(): Date

  public companion object {
    @JvmField
    public val SYSTEM: Clock = object : Clock {
      override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
      }
      override fun newDate(): Date {
        return Date()
      }
      override fun currentTimeSeconds(): Long {
        return TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis())
      }

      override fun currentTimeSecondsInt(): Int {
        return (currentTimeMillis() / 1000).toInt()
      }
    }
  }
}

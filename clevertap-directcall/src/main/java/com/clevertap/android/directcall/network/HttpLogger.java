package com.clevertap.android.directcall.network;

import android.util.Log;

import androidx.annotation.Nullable;

public final class HttpLogger {
   private static boolean isLogsRequired;
   public static final HttpLogger INSTANCE;

   private HttpLogger() {
   }

   static {
      INSTANCE = new HttpLogger();
      isLogsRequired = true;
   }

   public final boolean isLogsRequired() {
      return isLogsRequired;
   }

   public final void setLogsRequired(boolean enableLog) {
      isLogsRequired = enableLog;
   }

   public final void d(@Nullable String tag, @Nullable String message) {
      if (isLogsRequired) {
         Log.d(tag, message);
      }
   }

   public final void e(@Nullable String tag, @Nullable String message) {
      if (isLogsRequired) {
         Log.e(tag, message);
      }
   }
}

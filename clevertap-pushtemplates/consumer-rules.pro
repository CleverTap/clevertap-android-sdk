# The pt_progress Live Update renders the native Android 16 (androidx.core 1.17.0)
# NotificationCompat.ProgressStyle reflectively (see ProgressStyle.buildNative), so R8 in the host
# app must not strip or rename these classes/methods — otherwise the reflection fails and the SDK
# silently degrades to the RemoteViews fallback even on Android 16.
-keep class androidx.core.app.NotificationCompat$ProgressStyle { *; }
-keep class androidx.core.app.NotificationCompat$ProgressStyle$Segment { *; }
-keep class androidx.core.app.NotificationCompat$ProgressStyle$Point { *; }
-keepclassmembers class androidx.core.app.NotificationCompat$Builder {
    *** setRequestPromotedOngoing(...);
    *** setShortCriticalText(...);
}

# For CleverTap SDK's
-keep class com.clevertap.android.sdk.pushnotification.fcm.FcmPushProvider{*;}
-keep class com.google.firebase.messaging.FirebaseMessagingService{*;}
-keep class com.clevertap.android.sdk.pushnotification.CTNotificationIntentService{*;}
-keep class com.google.android.exoplayer2.ExoPlayer{*;}
-keep class com.google.android.exoplayer2.source.hls.HlsMediaSource{*;}
-keep class com.google.android.exoplayer2.ui.StyledPlayerView{*;}
-keep class androidx.media3.exoplayer.ExoPlayer{*;}
-keep class androidx.media3.exoplayer.hls.HlsMediaSource{*;}
-keep class androidx.media3.ui.PlayerView{*;}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient{*;}
-keep class com.google.android.gms.common.GooglePlayServicesUtil{*;}
-keep class com.google.android.play.core.review.ReviewManagerFactory{*;}
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-dontwarn com.clevertap.android.sdk.**
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
# WorkManager 2.10.x brings Room 2.6.1, whose rule keeps RoomDatabase subclasses but not their constructor. R8 in AGP 9 removes it and the app crashes at start (SDK-6284).
-keep class * extends androidx.room.RoomDatabase { void <init>(); }
# SDK-6379: Product Experiences variables. Parser reads @Variable fields reflectively and uses the
# FIELD NAME as the variable name, so R8 renaming or removing them in a release build silently
# breaks variables. Keep the annotation and the names of annotated fields (the host app no longer
# needs its own keep rule for this).
-keep @interface com.clevertap.android.sdk.variables.annotations.Variable
-keepclassmembers class * {
    @com.clevertap.android.sdk.variables.annotations.Variable <fields>;
}
# SDK-6379: JavaScript bridge (CTWebInterface and any @JavascriptInterface bridge the SDK installs).
# The WebView calls these methods by name from JavaScript; R8 must not rename or remove them.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

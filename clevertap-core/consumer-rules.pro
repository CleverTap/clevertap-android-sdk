# For CleverTap SDK's
-keep class com.clevertap.android.sdk.pushnotification.fcm.FcmPushProvider{*;}
-keep class com.clevertap.android.xps.XiaomiPushProvider{*;}
-keep class com.clevertap.android.hms.HmsPushProvider{*;}
-keep class com.google.firebase.messaging.FirebaseMessagingService{*;}
-keep class com.clevertap.android.sdk.pushnotification.CTNotificationIntentService{*;}
-keep class com.google.android.exoplayer2.ExoPlayer{*;}
-keep class com.google.android.exoplayer2.source.hls.HlsMediaSource{*;}
-keep class com.google.android.exoplayer2.ui.StyledPlayerView{*;}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient{*;}
-keep class com.google.android.gms.common.GooglePlayServicesUtil{*;}
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-dontwarn com.clevertap.android.sdk.**
-dontwarn com.baidu.**
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
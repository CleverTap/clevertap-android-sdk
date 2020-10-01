# For CleverTap SDK's
-keep class com.clevertap.android.**{*;}
-keep class com.google.android.gms.ads.**{*;}
-keep class **.R$* {
<fields>;
}
-keep class com.google.android.gms.common.**{*;}

-keep class com.google.android.exoplayer2.**{*;}

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
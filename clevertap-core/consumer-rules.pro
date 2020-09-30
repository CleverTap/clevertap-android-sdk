# For CleverTap SDK's
-keep class com.clevertap.android.**{*;}
-keep class com.google.android.gms.ads.**{*;}
-keep class **.R$* {
<fields>;
}

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
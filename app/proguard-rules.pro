############################## KJVBible 模块混淆规则 ##############################

# ================== Gson ==================
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
    @com.google.gson.annotations.Expose <methods>;
}
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson 类型适配器
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ================== Room Database ==================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ================== UtilCode ==================
-keep class com.blankj.utilcode.util.** { *; }
-dontwarn com.blankj.utilcode.**

# ================== Glide ==================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep class com.bumptech.glide.load.resource.** { *; }
-keep class com.bumptech.glide.integration.** { *; }
-dontwarn com.bumptech.glide.**

# ================== OkHttp ==================
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.internal.** { *; }
-keepclassmembers class okhttp3.internal.** { *; }
-keepclassmembers class okhttp3.** { *; }

# ================== Google服务 ==================
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.firebase.** { *; }
-keep class com.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.**

# ================== Google Ads Mobile SDK ==================
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-dontwarn com.google.android.libraries.ads.mobile.sdk.**
-keep class ads_mobile_sdk.** { *; }
-dontwarn ads_mobile_sdk.**

# ================== Facebook SDK ==================
-keep class com.facebook.** { *; }
-keep interface com.facebook.** { *; }
-keep class com.facebook.login.** { *; }
-keep class com.facebook.FacebookActivity { *; }
-keep class com.facebook.CustomTabActivity { *; }
-keep class com.facebook.share.** { *; }
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**
-keep class com.facebook.appevents.** { *; }
-keep class com.facebook.GraphRequest { *; }
-keep class com.facebook.GraphResponse { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.facebook.internal.method.annotation.* <methods>;
}
-keep class bolts.** { *; }
-keep class com.parse.bolts.** { *; }
-dontwarn bolts.**

# ================== UI库 ==================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ================== 其它（示例保留项） ==================
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

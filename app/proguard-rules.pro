# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve LineNumberTable and SourceFile for meaningful stack traces
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Room (Room AAR already bundles consumer-rules; preserve database implementations)
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Data Models & Entities (Room & Moshi & JSON & Firestore)
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Moshi
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Firebase & Google Play Services (Firebase SDK bundles consumer-rules)
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Glance / AppWidgets
-keep class com.example.widget.** { *; }


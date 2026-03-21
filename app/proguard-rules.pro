# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keep class io.ktor.client.engine.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }

-keepclasseswithmembers class com.google.android.piyush.youtube.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.google.android.piyush.youtube.model.**$$serializer { *; }
-keepclassmembers class com.google.android.piyush.youtube.model.** {
    *** Companion;
}

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# ExoPlayer
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp (used by Ktor CIO)
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep data classes used in serialization
-keep class com.google.android.piyush.youtube.model.Youtube { *; }
-keep class com.google.android.piyush.youtube.model.Item { *; }
-keep class com.google.android.piyush.youtube.model.Snippet { *; }
-keep class com.google.android.piyush.youtube.model.ContentDetails { *; }
-keep class com.google.android.piyush.youtube.model.Statistics { *; }
-keep class com.google.android.piyush.youtube.model.Thumbnails { *; }
-keep class com.google.android.piyush.youtube.model.comments.** { *; }
-keep class com.google.android.piyush.youtube.model.ratings.** { *; }
-keep class com.google.android.piyush.youtube.model.subscriptions.** { *; }
-keep class com.google.android.piyush.youtube.model.channelDetails.** { *; }
-keep class com.google.android.piyush.youtube.model.channelPlaylists.** { *; }
-keep class com.google.android.piyush.youtube.model.SearchTube { *; }
-keep class com.google.android.piyush.youtube.model.SearchTubeItems { *; }
-keep class com.google.android.piyush.youtube.model.Shorts { *; }

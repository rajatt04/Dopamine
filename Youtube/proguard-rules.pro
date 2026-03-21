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

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }

-keepclasseswithmembers class com.google.android.piyush.youtube.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.google.android.piyush.youtube.model.**$$serializer { *; }

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep all model classes
-keep class com.google.android.piyush.youtube.model.** { *; }

# OkHttp (used by Ktor CIO)
-dontwarn okhttp3.**
-dontwarn okio.**

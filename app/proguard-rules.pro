# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# R8 suggested rules to suppress warnings for missing classes
-dontwarn org.slf4j.impl.StaticLoggerBinder

# NewPipe Extractor
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn javax.annotation.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Jsoup optional dependencies
-dontwarn com.google.re2j.**

# ========== Hilt / Dagger ==========
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ========== Ktor CIO Client ==========
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# ========== kotlinx.serialization ==========
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class **$$serializer { *; }

# ========== YouTube API Models ==========
-keep class com.google.android.piyush.youtube.model.** { *; }
-keep class com.google.android.piyush.database.entities.** { *; }
-keep class com.google.android.piyush.database.model.** { *; }

# ========== Preserve line numbers for crash reports ==========
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

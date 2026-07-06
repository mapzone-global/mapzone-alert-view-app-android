# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- Alert View SDK (JNI) ---
# Native code (alert_view_jni.cpp) resolves these classes and invokes their
# callback methods by name via JNI. R8 must not rename/remove them, or the
# native layer crashes at runtime (bitmap/voice/route callbacks, native methods).
-keep class com.vietmap.alert_view_sdk.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- Optional transitive deps referenced by Vietmap/Mapbox navigation libs ---
# These are compile-time-optional integrations not bundled in the app (the app
# uses Vietmap's own LocationEngine, not Google Play Services). R8 only needs to
# stop warning about the absent references; nothing is kept.
-dontwarn com.google.android.gms.**
-dontwarn com.squareup.picasso.**
-dontwarn com.ryanharter.auto.value.gson.**
-dontwarn com.mapbox.api.directions.v5.AutoValue_MapboxDirections$1

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
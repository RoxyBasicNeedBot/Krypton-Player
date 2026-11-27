# ArchPlayer Production ProGuard / R8 Optimization Rules
# For more details, see: http://developer.android.com/guide/developing/tools/proguard.html

## Essential attributes for reflection, deserialization and coroutines
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepattributes SourceFile,LineNumberTable
-keepattributes DefaultMethod,InterfaceDefaultMethod
-renamesourcefileattribute SourceFile

## Preserve class names and method names in app packages for readable crash traces
-keepnames class dev.roxy.kryptonplayer.** { *; }

## TDLib JNI Bindings Protection (Strictly Preserve native MTProto methods and classes)
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**

## libmpv JNI Protection Rules
-keep class is.xyz.mpv.** { *; }
-keepclassmembers class is.xyz.mpv.** { *; }
-dontwarn is.xyz.mpv.**

## Guava Protection Rules
-keep class com.google.common.** { *; }
-keep class com.google.common.util.concurrent.** { *; }
-keep class com.google.common.collect.** { *; }
-dontwarn com.google.common.**

## Kotlin Serialization Protection
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

## Retrofit 2.x & Network Models Protection (Never Obfuscate Signatures or Methods)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes *Annotation*
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# Preserve all Retrofit API service interfaces, classes, models and DTOs
-keep interface dev.roxy.kryptonplayer.core.data.network.** { *; }
-keep class dev.roxy.kryptonplayer.core.data.network.** { *; }
-keepclassmembers class dev.roxy.kryptonplayer.core.data.network.** { *; }
-keep class dev.roxy.kryptonplayer.core.data.network.models.** { *; }
-keepclassmembers class dev.roxy.kryptonplayer.core.data.network.models.** { *; }

# Preserve all TDLib data layer classes
-keep class dev.roxy.kryptonplayer.core.data.tdlib.** { *; }
-keepclassmembers class dev.roxy.kryptonplayer.core.data.tdlib.** { *; }

# Preserve all API, Media, Lyrics, Shazam and Database Models used in serialization and reflection
-keep class dev.roxy.kryptonplayer.core.data.lyrics.** { *; }
-keepclassmembers class dev.roxy.kryptonplayer.core.data.lyrics.** { *; }
-keep class dev.roxy.kryptonplayer.core.data.shazam.** { *; }
-keepclassmembers class dev.roxy.kryptonplayer.core.data.shazam.** { *; }
-keep class dev.roxy.kryptonplayer.core.model.** { *; }
-keepclassmembers class dev.roxy.kryptonplayer.core.model.** { *; }

# Preserve Telegram repository, models, and download manager
-keep class dev.roxy.kryptonplayer.core.data.repository.** { *; }
-keepclassmembers class dev.roxy.kryptonplayer.core.data.repository.** { *; }
-keep class dev.roxy.kryptonplayer.core.data.download.** { *; }
-keepclassmembers class dev.roxy.kryptonplayer.core.data.download.** { *; }

# Telegram and VideoPicker ViewModels and Screens
-keep class dev.roxy.kryptonplayer.feature.videopicker.** { *; }
-keepclassmembers class dev.roxy.kryptonplayer.feature.videopicker.** { *; }

## Gson Official Reflection Rules (Strictly Preserve field names for JSON mapping)
-keep class com.google.gson.** { *; }
-dontwarn sun.misc.**
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
}

## ML Kit Language & Translation
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

## Coil / Coil3 Image Loading
-keep class coil.** { *; }
-keep class coil3.** { *; }
-dontwarn coil.**
-dontwarn coil3.**

## DataStore & Coroutines
-keep class androidx.datastore.** { *; }
-dontwarn kotlinx.coroutines.**

## Aggressive Bytecode Shrinking: Strip Verbose & Debug Logs in Release Builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

## Disable aggressive R8 optimization to prevent devirtualization of synthetic SAM bridge methods
-dontoptimize

## Kyant Backdrop & Liquid Glass Protection (Prevents AbstractMethodError in effects scope)
-keep class com.kyant.** { *; }
-keep interface com.kyant.** { *; }
-keepclassmembers class com.kyant.** { *; }
-keepclassmembernames class com.kyant.** { *; }
-dontwarn com.kyant.**

-keep class io.github.kyant0.** { *; }
-keep interface io.github.kyant0.** { *; }
-keepclassmembers class io.github.kyant0.** { *; }
-dontwarn io.github.kyant0.**

## Jetpack Compose Modifier.Node, Foundation Style & UI Node Protection (Fixes AbstractMethodError under R8)
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses,DefaultMethod,InterfaceDefaultMethod,Synthetic,Bridge

-keep class androidx.compose.ui.node.** { *; }
-keep interface androidx.compose.ui.node.** { *; }
-keepclassmembers class androidx.compose.ui.node.** { *; }

-keep class androidx.compose.foundation.style.** { *; }
-keep interface androidx.compose.foundation.style.** { *; }
-keepclassmembers class androidx.compose.foundation.style.** { *; }
-keepclassmembers interface androidx.compose.foundation.style.** { *; }

-keep class * implements androidx.compose.foundation.style.** { *; }
-keep class ** implements androidx.compose.foundation.style.** { *; }
-keepclassmembers class * implements androidx.compose.foundation.style.** { *; }
-keepclassmembers class ** implements androidx.compose.foundation.style.** { *; }
-keep class * extends androidx.compose.foundation.style.** { *; }
-keep class ** extends androidx.compose.foundation.style.** { *; }
-keepclassmembers class * extends androidx.compose.foundation.style.** { *; }
-keepclassmembers class ** extends androidx.compose.foundation.style.** { *; }
-keep interface * extends androidx.compose.foundation.style.** { *; }
-keep interface ** extends androidx.compose.foundation.style.** { *; }
-keepclassmembers interface * extends androidx.compose.foundation.style.** { *; }
-keepclassmembers interface ** extends androidx.compose.foundation.style.** { *; }

-keep class * implements androidx.compose.foundation.style.CustomStyle { *; }
-keep class ** implements androidx.compose.foundation.style.CustomStyle { *; }
-keepclassmembers class * implements androidx.compose.foundation.style.CustomStyle { *; }
-keepclassmembers class ** implements androidx.compose.foundation.style.CustomStyle { *; }
-keep class * implements androidx.compose.foundation.style.Style { *; }
-keep class ** implements androidx.compose.foundation.style.Style { *; }
-keepclassmembers class * implements androidx.compose.foundation.style.Style { *; }
-keepclassmembers class ** implements androidx.compose.foundation.style.Style { *; }

-keepclassmembers class * {
    public void applyStyle(...);
    public synthetic void applyStyle(...);
    void applyStyle(...);
    synthetic void applyStyle(...);
}

-keepclassmembers class ** {
    public void applyStyle(...);
    public synthetic void applyStyle(...);
    void applyStyle(...);
    synthetic void applyStyle(...);
}

-keepclasseswithmembers class * {
    public void applyStyle(...);
}
-keepclasseswithmembers class ** {
    public void applyStyle(...);
}
-keepclasseswithmembers class * {
    void applyStyle(...);
}
-keepclasseswithmembers class ** {
    void applyStyle(...);
}

-keep class androidx.compose.foundation.style.ResolvedStyle { *; }
-keep class androidx.compose.foundation.style.StyleOuterNode { *; }
-keep class androidx.compose.foundation.style.StyleInnerNode { *; }
-keep class androidx.compose.foundation.style.StyleOuterNode$** { *; }
-keep class androidx.compose.foundation.style.StyleInnerNode$** { *; }
-keep class androidx.compose.foundation.style.Style$** { *; }
-keep class androidx.compose.foundation.style.CustomStyle$** { *; }

-keep class * extends androidx.compose.ui.Modifier$Node { *; }
-keep class ** extends androidx.compose.ui.Modifier$Node { *; }
-keepclassmembers class * extends androidx.compose.ui.Modifier$Node { *; }
-keepclassmembers class ** extends androidx.compose.ui.Modifier$Node { *; }
-keep class * extends androidx.compose.ui.node.ModifierNodeElement { *; }
-keep class ** extends androidx.compose.ui.node.ModifierNodeElement { *; }
-keepclassmembers class * extends androidx.compose.ui.node.ModifierNodeElement { *; }
-keepclassmembers class ** extends androidx.compose.ui.node.ModifierNodeElement { *; }
-keep class * implements androidx.compose.ui.node.DelegatableNode { *; }
-keep class ** implements androidx.compose.ui.node.DelegatableNode { *; }
-keep class * implements androidx.compose.ui.node.DrawModifierNode { *; }
-keep class ** implements androidx.compose.ui.node.DrawModifierNode { *; }
-keep class * implements androidx.compose.ui.node.LayoutModifierNode { *; }
-keep class ** implements androidx.compose.ui.node.LayoutModifierNode { *; }
-keep class * implements androidx.compose.ui.node.ObserverModifierNode { *; }
-keep class ** implements androidx.compose.ui.node.ObserverModifierNode { *; }
-keep class * implements androidx.compose.ui.node.GlobalPositionAwareModifierNode { *; }
-keep class ** implements androidx.compose.ui.node.GlobalPositionAwareModifierNode { *; }
-keep class * implements androidx.compose.ui.node.SemanticsModifierNode { *; }
-keep class ** implements androidx.compose.ui.node.SemanticsModifierNode { *; }
-keep class * implements androidx.compose.ui.node.PointerInputModifierNode { *; }
-keep class ** implements androidx.compose.ui.node.PointerInputModifierNode { *; }
-dontwarn androidx.compose.**

## Strip Android Log & System.out/err in Release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

## Firebase & Credential Manager Protection Rules
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**

## GitHub Release Self-Update Models
-keep class dev.roxy.kryptonplayer.core.data.update.GitHubRelease { *; }
-keep class dev.roxy.kryptonplayer.core.data.update.GitHubAsset { *; }
-keep class dev.roxy.kryptonplayer.core.data.update.AppUpdateInfo { *; }
-keep class dev.roxy.kryptonplayer.core.data.update.ApkAsset { *; }
-keep class dev.roxy.kryptonplayer.core.data.update.UpdateState** { *; }


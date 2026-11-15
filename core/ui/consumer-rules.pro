## Consumer Proguard rules for core:ui (Kyant Backdrop & Liquid Glass Protection)
-keep class com.kyant.** { *; }
-keep interface com.kyant.** { *; }
-keepclassmembers class com.kyant.** { *; }
-keepclassmembernames class com.kyant.** { *; }
-dontwarn com.kyant.**

-keep class io.github.kyant0.** { *; }
-keep interface io.github.kyant0.** { *; }
-keepclassmembers class io.github.kyant0.** { *; }
-dontwarn io.github.kyant0.**

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,DefaultMethod,InterfaceDefaultMethod,Synthetic,Bridge

## Jetpack Compose Modifier.Node, Foundation Style & UI Node Protection (Fixes AbstractMethodError under R8)
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

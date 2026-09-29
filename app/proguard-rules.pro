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

# ✅ Reglas de androidx.startup (las de la librería se ignoran en build.gradle.kts), sin los dos
# inicializadores que se quitan del manifest, así R8 puede borrar su código (EmojiCompat y
# ProcessLifecycleOwner). Cualquier otro inicializador sigue conservándose como antes.
# Si alguno de los dos se vuelve a activar en el AndroidManifest.xml, hay que quitarlo de aquí.
-keepnames class !androidx.emoji2.text.EmojiCompatInitializer,!androidx.lifecycle.ProcessLifecycleInitializer,** extends androidx.startup.Initializer
-keep class !androidx.emoji2.text.EmojiCompatInitializer,!androidx.lifecycle.ProcessLifecycleInitializer,** extends androidx.startup.Initializer {
    <init>();
}
-assumenosideeffects class androidx.startup.StartupLogger { public static <methods>; }

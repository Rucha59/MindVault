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
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Phase A (Supabase / Ktor): warnings only. Release build with accounts is UNVERIFIED until you run it.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.**

# Search (V1 Free Dictionary and V2 Wiktionary/Wikipedia): the response classes are read by kotlinx.serialization
# and described to Retrofit through generic types (for example Map<String, List<...Dto>>). Release builds
# (R8) must keep their generated serializers and the generic signatures. UNVERIFIED until a release/R8 build is run.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keep,includedescriptorclasses class com.mindvault.data.remote.**$$serializer { *; }
-keepclassmembers class com.mindvault.data.remote.** { *** Companion; }
-keepclasseswithmembers class com.mindvault.data.remote.** { kotlinx.serialization.KSerializer serializer(...); }
-dontnote kotlinx.serialization.AnnotationsKt
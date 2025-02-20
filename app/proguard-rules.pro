# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
#
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable,SourceFile
-repackageclasses
-keep public class ru.morozovit.ultimatesecurity.ui.MainActivity
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int i(...);
    public static int v(...);
}
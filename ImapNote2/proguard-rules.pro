# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

-keepclassmembers class **.R$color {
    public static <fields>;
}

-dontobfuscate
# You can specify any path and filename.
-printconfiguration full-r8-config.txt
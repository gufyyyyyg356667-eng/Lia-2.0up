# Release shrinking. Compose ships its own consumer rules, so only app-specific keeps are listed.

# org.json is read by name from JSON payloads, so its classes must not be renamed or removed.
-keep class org.json.** { *; }
-dontwarn org.json.**

# OkHttp and Okio are used through their public APIs only.
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep the service and activity entry points referenced from the manifest.
-keep class com.Lia.assistant.voice.VoiceForegroundService { *; }
-keep class com.Lia.assistant.ui.MainActivity { *; }

# Strip Log.d/v/i in release; keep warnings and errors for field reports.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

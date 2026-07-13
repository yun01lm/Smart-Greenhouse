# ProGuard 规则
-keepattributes Signature
-keepattributes *Annotation*

# Gson
-keep class com.greenhouse.app.data.model.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

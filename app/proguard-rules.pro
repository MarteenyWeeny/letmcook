# Retrofit and Gson generic type preservation
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-keepattributes MethodParameters

# Keep our models and services
-keep class com.letmcook.letmcook.models.** { *; }
-keep class com.letmcook.letmcook.services.** { *; }

# Gson specific rules
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit specific rules
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# OkHttp/Okio
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# Keep keyboard app classes and models
-keep class com.example.mykeyboard.** { *; }
-keep public class * extends android.inputmethodservice.InputMethodService
-keep public class * extends android.app.Activity
-dontwarn com.example.mykeyboard.**

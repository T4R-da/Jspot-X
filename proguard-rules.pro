# Add project specific ProGuard rules here.
# For more details, see
# http://developer.android.com/guide/developing/tools/proguard.html

# Keep the Javascript Interface classes
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep the bridge inner class specifically
-keep class com.example.spoticaplite.MainActivity$SpotifyBridge { *; }

# PDFBox-Android rules
# Verified: These are optional dependencies for JPX (JPEG 2000) and encryption support.
# The library handles their absence at runtime.
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.harmony.**
-dontwarn javax.xml.stream.**
-dontwarn java.awt.**

# Keep PDFBox resources and necessary classes for resource loading
-keep class com.tom_roush.pdfbox.** { *; }
-keep interface com.tom_roush.pdfbox.** { *; }

# R8 optimization to prevent issues with certain PDFBox filters
-assumenosideeffects class com.tom_roush.pdfbox.filter.JPXFilter {
    *;
}

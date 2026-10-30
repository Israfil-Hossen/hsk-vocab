# Data classes are read from JSON with kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.israfilhossen.dawahchinese.**$$serializer { *; }
-keepclassmembers class com.israfilhossen.dawahchinese.** { *** Companion; }
-keepclasseswithmembers class com.israfilhossen.dawahchinese.** { kotlinx.serialization.KSerializer serializer(...); }

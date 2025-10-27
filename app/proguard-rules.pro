# Disable obfuscation to prevent crashes (like Seal app)
-dontobfuscate

# Keep YouTubeDL Android library
-keep class com.yausername.** { *; }
-keep class org.apache.commons.compress.archivers.zip.** { *; }

# Keep native libraries and JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlinx Serialization Rules
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Serializable classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep serializer methods
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep INSTANCE serializers for objects
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# ViewModel and LiveData
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Keep your data classes and models
-keep class com.devson.nosved.data.** { *; }
-keepclassmembers class com.devson.nosved.data.** { *; }

# Media3 ExoPlayer
-keep class com.google.android.exoplayer2.** { *; }
-keep class androidx.media3.** { *; }

# Coil image loading
-keep class coil.** { *; }

# Permissions library
-keep class com.google.accompanist.permissions.** { *; }

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Reduce APK size
-repackageclasses ''
-allowaccessmodification
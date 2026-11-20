# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.yogeshpaliyal.gapwise.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.yogeshpaliyal.gapwise.**$$serializer { *; }

# Glance creates widget action callbacks by class name, from the PendingIntent.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

# WorkManager creates workers by class name; keep them so scheduled work survives updates.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Readable stack traces: keep file names and line numbers (the class names are still
# obfuscated; mapping.txt is attached to each GitHub release and bundled in the AAB).
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

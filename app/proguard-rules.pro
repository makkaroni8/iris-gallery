# Project-specific shrinking rules belong here.

# ----------------------------------------------------------------------
# Lifecycle / ViewModel reflection constructors
# ----------------------------------------------------------------------
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>();
}

# ----------------------------------------------------------------------
# WorkManager: Keep all Workers and their reflection constructors
# ----------------------------------------------------------------------
-keep class com.iris.gallery.data.search.PhotoIndexingWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ----------------------------------------------------------------------
# Room & Database Entities / DAOs
# ----------------------------------------------------------------------
-keep class com.iris.gallery.data.search.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.Embedded *;
    @androidx.room.Relation *;
    @androidx.room.Ignore *;
    @androidx.room.Upsert *;
    @androidx.room.Query *;
}

# ----------------------------------------------------------------------
# Google ML Kit Core & Common Vision
# ----------------------------------------------------------------------
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Retain reflection-based dependency injection for ML Kit Component Registrars
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}

# Internal GMS ML Kit Vision classes and Dynamite loaders
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-dontwarn com.google.android.gms.internal.mlkit_vision_**
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# ML Kit Text Recognition
-keep class com.google.mlkit.vision.text.** { *; }
-keep class * implements com.google.mlkit.vision.text.TextRecognizerOptionsInterface { *; }
-dontwarn com.google.mlkit.vision.text.chinese.**
-dontwarn com.google.mlkit.vision.text.devanagari.**
-dontwarn com.google.mlkit.vision.text.japanese.**
-dontwarn com.google.mlkit.vision.text.korean.**

# ML Kit Image Labeling & TensorFlow Lite
-keep class com.google.mlkit.vision.label.** { *; }
-keep class com.google.mlkit.vision.label.custom.** { *; }
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

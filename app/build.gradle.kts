@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val isBuildingBundle = gradle.startParameter.taskNames.any { task: String -> task.contains("bundle", ignoreCase = true) }

android {
    namespace = "com.iris.gallery"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.iris.gallery"
        minSdk = 26
        targetSdk = 37
        versionCode = 19
        versionName = "0.9.0"
        resValue("string", "library_map_title", "Map")
        resValue("string", "library_map_subtitle", "Explore your photos on an offline map")
        resValue("string", "settings_section_map", "Offline Map")
        resValue("string", "settings_map_desc", "Offline vector map and GPS location index for photos")
        resValue("string", "settings_map_custom_active", "Custom detail map active (.pmtiles)")
        resValue("string", "settings_map_base_active", "Bundled global base map active")
        resValue("string", "settings_map_import_btn", "Import PMTiles map")
        resValue("string", "settings_map_reset_btn", "Reset to base map")
        resValue("string", "settings_map_note", "100% offline. You can download and import any .pmtiles vector map (e.g. from Protomaps).")
        resValue("string", "settings_map_importing", "Importing map&#8230;")
        resValue("string", "settings_map_reindex_title", "Rebuild Map Index")
        resValue("string", "settings_map_reindex_desc", "Re-analyze GPS locations and map preview thumbnails for all photos")
        resValue("string", "settings_map_reindex_dialog_title", "Rebuild Map Index?")
        resValue("string", "settings_map_reindex_dialog_msg", "The saved location cache and all generated preview markers will be deleted and rebuilt from scratch.")
        resValue("string", "settings_map_reindex_success", "Map index successfully rebuilt (%1$" + "d locations)")
        resValue("string", "settings_map_theme_title", "Map Appearance")
        resValue("string", "settings_map_theme_desc", "Choose whether the offline map follows the app theme or uses light/dark mode")
        resValue("string", "settings_map_theme_auto", "Auto (App)")
        resValue("string", "settings_map_theme_light", "Light")
        resValue("string", "settings_map_theme_dark", "Dark")
    }

    splits {
        abi {
            isEnable = !isBuildingBundle
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    androidResources {
        noCompress += listOf("tflite", "pmtiles", "pbf")
        localeFilters += listOf(
            "en", "ar", "de", "es", "fr", "it", "ja", "pl", "pt", "pt-rBR", "ru", "tr", "zh", "zh-rCN",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    signingConfigs {
        create("debugConfig") {
            val localKeystore = file("$rootDir/debug.keystore")
            storeFile = if (localKeystore.exists()) {
                localKeystore
            } else {
                file("${System.getProperty("user.home")}/.android/debug.keystore")
            }
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("optimized") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debugConfig")
            matchingFallbacks += listOf("release")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    lint {
        checkReleaseBuilds = false
        disable += listOf(
            "MissingTranslation",
            "ExtraTranslation",
            "TypographyDashes",
            "PluralsCandidate",
            "LogNotTimber",
            "NewerVersionAvailable",
            "GradleDynamicVersion",
            "AndroidGradlePluginVersion",
            "GradleDependency",
        )
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/*.kotlin_module",
                "kotlin/**",
                "kotlintooling/**",
            )
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.allWarningsAsErrors.set(true)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
    implementation("io.coil-kt.coil3:coil-gif:3.6.3")
    implementation("io.coil-kt.coil3:coil-svg:3.6.3")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("org.aomedia.avif.android:avif:1.1.1.14d8e3c4")

    // Room
    val roomVersion = "2.8.5"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ML Kit
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.12.0")

    // MapLibre Offline Map SDK & Coroutines
    //noinspection GradleDependency,NewerVersionAvailable
    implementation("org.maplibre.gl:android-sdk:11.13.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


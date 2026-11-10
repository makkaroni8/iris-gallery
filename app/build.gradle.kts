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
        noCompress += listOf("tflite")
        localeFilters += listOf(
            "en", "ar", "de", "es", "fr", "it", "ja", "pl", "pt", "pt-rBR", "ru", "tr", "zh", "zh-rCN",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
        disable += listOf("MissingTranslation", "ExtraTranslation", "TypographyDashes", "PluralsCandidate")
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

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


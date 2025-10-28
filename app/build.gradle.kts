import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

// Keystore configuration
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Check if splits should be enabled (disable for debug builds)
val splitApks = !project.hasProperty("noSplits") && !gradle.startParameter.taskNames.any {
    it.contains("debug", ignoreCase = true)
}

android {
    namespace = "com.devson.nosved"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.devson.nosved"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Conditional ABI configuration
        if (!splitApks) {
            // For debug builds - only include device ABI for faster builds
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String?
                keyPassword = keystoreProperties["keyPassword"] as String?
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    // Conditional splits configuration
    if (splitApks) {
        splits {
            abi {
                isEnable = true
                reset()
                include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                isUniversalApk = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "**/kotlin/**"
            excludes += "**/*.kotlin_metadata"
            excludes += "**/*.version"
            excludes += "**/kotlin-tooling-metadata.json"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    ndkVersion = "27.0.12077973"
}

// Use optimized youtubedl-android version (same as Seal)
val youtubedlAndroid = "0.17.3"

dependencies {
    // YouTubeDL Android - Same optimized version as Seal
    implementation("io.github.junkfood02.youtubedl-android:library:0.17.3")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.17.3")

    // Core Android Components - Latest stable versions
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose BOM - Performance optimized
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Essential animation dependencies only
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.foundation:foundation")

    // Navigation with performance optimization
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Lifecycle and ViewModel - Performance focused
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Room Database - Optimized
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Image Loading - Essential only (like Seal)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Performance monitoring
    implementation("androidx.compose.runtime:runtime-tracing:1.0.0-beta01")

    // Date/Time - Lightweight
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // JSON Parsing - Performance optimized
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines - Performance optimized
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Network optimization
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Data storage - Performance optimized
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.tencent:mmkv:1.3.9")

    // Essential dependencies only
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Debug tools - Only for debug builds
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Optimize builds like Seal
configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:24.1.0")
        force("androidx.annotation:annotation:1.8.2")
    }

    // Remove duplicate dependencies
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
}
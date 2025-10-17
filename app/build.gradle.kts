plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.devson.nosved"
    compileSdk = 36 // Assuming release 36 is correct; adjust if necessary

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
    }

    signingConfigs {
        create("release") {
            // This is a placeholder for your signing configuration.
            // You can either configure it here or let Android Studio handle it during the build process.
        }
    }

    buildTypes {
        release {
            // Enable code shrinking, obfuscation, and optimization.
            isMinifyEnabled = true

            // Enable resource shrinking.
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // This block enables the ABI splits, similar to the "Seal" project
    flavorDimensions += "abi"
    productFlavors {
        create("arm64-v8a") {
            dimension = "abi"
            ndk {
                abiFilters += "arm64-v8a"
            }
        }
        create("armeabi-v7a") {
            dimension = "abi"
            ndk {
                abiFilters += "armeabi-v7a"
            }
        }
        create("x86_64") {
            dimension = "abi"
            ndk {
                abiFilters += "x86_64"
            }
        }
        create("x86") {
            dimension = "abi"
            ndk {
                abiFilters += "x86"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Example version, adjust if needed
    }
    packaging {
        resources {
            // youtube-dl has a built-in downloader as a fallback.
            jniLibs {
                excludes += "**/libaria2c.so"
            }
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val youtubedlAndroid = "0.18.0"

dependencies {
    implementation("io.github.junkfood02.youtubedl-android:library:${youtubedlAndroid}")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:${youtubedlAndroid}")
//    implementation("io.github.junkfood02.youtubedl-android:aria2c:${youtubedlAndroid}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
// File: app/build.gradle.kts (Module :app)
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Apply Google Services for Firebase
    id("com.google.gms.google-services")

    // Apply the Compose Compiler plugin
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.sentimentanalysis"
    compileSdk = 36 // Recommended: Ensure this matches your targetSdk

    defaultConfig {
        applicationId = "com.example.sentimentanalysis"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Using Java 17 is recommended for modern Android projects
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
    // Note: No composeOptions block is needed for Kotlin 2.0+
}

// Keep your dependencies block as it was (ensure it includes Firebase and Navigation)

dependencies {
    // 1. Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // 2. Navigation (Fixes the red 'androidx.navigation' error)
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // 3. Firebase (Fixes the red 'FirebaseAuth' errors)
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-auth-ktx")

    // 4. Icons (Fixes the red 'Icons.Filled.Logout' error)
    implementation("androidx.compose.material:material-icons-extended:1.7.6")

    // 5. Google Sign In Credentials
    implementation("androidx.credentials:credentials:1.5.0-alpha06")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0-alpha06")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(libs.firebase.crashlytics.buildtools)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.google.firebase:firebase-firestore")
}
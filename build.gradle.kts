// File: build.gradle.kts (Project: SentimentAnalysis)
plugins {
    // Basic Android and Kotlin plugins
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // Google Services (Required for Firebase)
    id("com.google.gms.google-services") version "4.4.2" apply false

    // Compose Compiler (Required for Kotlin 2.0+)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}
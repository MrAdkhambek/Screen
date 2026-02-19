plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    alias(libs.plugins.screen)
    alias(libs.plugins.viewbinding)
}

android {
    namespace = "com.adkhambek.screen.sample"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.adkhambek.screen.sample"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures.viewBinding = true
}

// Substitute Maven artifacts with local projects
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.adkhambek:screen-compiler")).using(project(":screen-compiler"))
        substitute(module("com.adkhambek:viewbinding-compiler")).using(project(":viewbinding-compiler"))
    }
}

dependencies {
    implementation(project(":screen-annotations"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.cicerone)


    implementation(libs.vbpd)
}

kotlin {
    jvmToolchain(21)
}

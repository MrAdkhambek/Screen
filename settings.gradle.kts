pluginManagement {
    includeBuild("screen-gradle-plugin")
    includeBuild("viewbinding-gradle-plugin")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "screen"
include(":screen-annotations")
include(":screen-compiler")
include(":viewbinding-compiler")
include(":app")

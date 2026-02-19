plugins {
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "2.3.0"
}

group = "com.adkhambek"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.3.0")
}

gradlePlugin {
    plugins {
        create("screen") {
            id = "com.adkhambek.screen"
            implementationClass = "com.adkhambek.screen.gradle.ScreenSubplugin"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

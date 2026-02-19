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
        create("viewbinding") {
            id = "com.adkhambek.viewbinding"
            implementationClass = "com.adkhambek.viewbinding.gradle.ViewBindingSubplugin"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.3.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

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

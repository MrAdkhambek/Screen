plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.3.0"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.3.10")
}

gradlePlugin {
    plugins {
        create("screen") {
            id = "com.adkhambek.screen"
            implementationClass = "com.adkhambek.screen.gradle.ScreenSubplugin"
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

kotlin {
    jvmToolchain(21)
}

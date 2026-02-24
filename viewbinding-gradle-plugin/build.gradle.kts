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
        create("viewbinding") {
            id = "com.adkhambek.viewbinding"
            implementationClass = "com.adkhambek.viewbinding.gradle.ViewBindingSubplugin"
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

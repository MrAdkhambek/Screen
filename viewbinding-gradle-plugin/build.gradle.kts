plugins {
    `java-gradle-plugin`
    kotlin("jvm") version libs.versions.kotlin
    id("com.vanniktech.maven.publish") version libs.versions.maven.publish
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${libs.versions.kotlin.get()}")
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

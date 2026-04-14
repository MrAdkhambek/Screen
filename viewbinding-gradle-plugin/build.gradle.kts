plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.3.10"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.3.10")
}

val generateBuildConfig = tasks.register("generateBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/buildconfig")
    val versionName = project.property("VERSION_NAME") as String
    inputs.property("versionName", versionName)
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("com/adkhambek/viewbinding/gradle")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            |package com.adkhambek.viewbinding.gradle
            |
            |internal object BuildConfig {
            |    const val VERSION: String = "$versionName"
            |}
            """.trimMargin()
        )
    }
}

sourceSets.main {
    kotlin.srcDir(generateBuildConfig)
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

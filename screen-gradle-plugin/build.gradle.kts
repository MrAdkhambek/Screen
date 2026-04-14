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
        val dir = outputDir.get().asFile.resolve("com/adkhambek/screen/gradle")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            |package com.adkhambek.screen.gradle
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

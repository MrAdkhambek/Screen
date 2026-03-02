plugins {
    id("org.jetbrains.intellij.platform") version "2.5.0"
    kotlin("jvm") version "2.3.0"
}

group = "com.adkhambek"
version = "1.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val localPath = "/Applications/Android Studio.app"
        if (file(localPath).exists()) {
            local(localPath)
        } else {
            androidStudio("2024.3.2.14")
        }
        bundledPlugin("org.jetbrains.kotlin")
    }
    implementation(files("libs/screen-compiler.jar"))
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.adkhambek.screen.idea"
        name = "Screen"
        version = "1.1.0"
        ideaVersion {
            sinceBuild = "251"
        }
    }
    buildSearchableOptions = false
}

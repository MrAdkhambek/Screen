plugins {
    id("org.jetbrains.intellij.platform") version "2.5.0"
    kotlin("jvm") version "2.3.0"
}

group = "com.adkhambek"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        local("/Applications/Android Studio.app")
        bundledPlugin("org.jetbrains.kotlin")
    }
    implementation(files("libs/viewbinding-compiler.jar"))
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.adkhambek.viewbinding.idea"
        name = "ViewBinding"
        version = "1.0.0"
        ideaVersion {
            sinceBuild = "251"
        }
    }
    buildSearchableOptions = false
}

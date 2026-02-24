# Screen

A Kotlin K2 compiler plugin that generates companion object constants and utilities for Android Fragment classes annotated with `@Screen`.

[![Maven Central](https://img.shields.io/maven-central/v/com.adkhambek.screen/screen-annotations)](https://central.sonatype.com/namespace/com.adkhambek.screen)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

| Tool   | Version |
|--------|---------|
| Kotlin | 2.3.10  |
| Java   | 21      |
| AGP    | 8.13.0  |
| Gradle | 9.3.1   |

## Installation

### Screen Plugin

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    id("com.adkhambek.screen") version "<latest version>"
}

dependencies {
    implementation("com.adkhambek.screen:screen-annotations:<latest version>")
}
```

### ViewBinding Plugin

```kotlin
plugins {
    id("com.adkhambek.viewbinding") version "<latest version>"
}
```

### Maven Coordinates

| Artifact                  | Coordinates                                                              |
|---------------------------|--------------------------------------------------------------------------|
| Screen Annotations        | `com.adkhambek.screen:screen-annotations:<latest version>`               |
| Screen Compiler           | `com.adkhambek.screen:screen-compiler:<latest version>`                  |
| Screen Gradle Plugin      | `com.adkhambek.screen:screen-gradle-plugin:<latest version>`             |
| ViewBinding Compiler      | `com.adkhambek.viewbinding:viewbinding-compiler:<latest version>`        |
| ViewBinding Gradle Plugin | `com.adkhambek.viewbinding:viewbinding-gradle-plugin:<latest version>`   |

## Features

- Generates `const val KEY` in the companion object, set to the fully qualified class name
- Generates a `private val arg` property for reading Parcelable arguments from the Fragment's Bundle
- Generates a `createScreen()` factory function when Cicerone `FragmentScreen` is on the classpath
- Companion FIR checkers validate annotation usage at compile time
- IntelliJ IDEA / Android Studio plugin with line markers, documentation, and inspections
- ViewBinding compiler plugin companion for view binding code generation

## Modules

| Module                      | Description                                |
|-----------------------------|--------------------------------------------|
| `screen-annotations`        | `@Screen` annotation                       |
| `screen-compiler`           | K2 compiler plugin (FIR + IR)              |
| `screen-gradle-plugin`      | Gradle plugin to apply the compiler plugin |
| `screen-idea-plugin`        | IntelliJ IDEA plugin for IDE support       |
| `viewbinding-compiler`      | ViewBinding K2 compiler plugin             |
| `viewbinding-gradle-plugin` | ViewBinding Gradle plugin                  |
| `viewbinding-idea-plugin`   | ViewBinding IntelliJ IDEA plugin           |
| `app`                       | Sample Android application                 |

## Usage

### 1. Apply the Gradle plugin

```kotlin
plugins {
    id("com.adkhambek.screen")
}
```

### 2. Annotate your Fragment

```kotlin
@Screen
class HomeFragment : Fragment(R.layout.fragment_home)
```

The compiler generates a companion object with a `KEY` constant:

```kotlin
// Generated
companion object {
    const val KEY: String = "com.example.HomeFragment"
}
```

### 3. With Parcelable arguments

```kotlin
@Screen(arg = Arg::class, isNullable = false)
class SampleFragment : Fragment(R.layout.fragment_sample) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // `arg` is generated automatically
        Toast.makeText(requireContext(), arg.id.toString(), Toast.LENGTH_LONG).show()
    }
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

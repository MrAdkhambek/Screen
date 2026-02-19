# Screen Gradle Plugin

Gradle plugin that integrates the Screen compiler plugin into Kotlin compilations.

## Overview

This module provides a Gradle plugin that automatically applies the `screen-compiler` artifact to all Kotlin compilations in the project. It implements the `KotlinCompilerPluginSupportPlugin` interface to seamlessly integrate with the Kotlin Gradle plugin.

## Plugin ID

```
com.adkhambek.screen
```

## Usage

### Step 1: Include the plugin build

In your root `settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("screen-gradle-plugin")
}
```

Or, if published to a repository:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

### Step 2: Apply the plugin

In your module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.adkhambek.screen")
}
```

### Step 3: Add the annotations dependency

```kotlin
dependencies {
    implementation("com.adkhambek:screen-annotations:1.0-SNAPSHOT")
}
```

### Step 4: Annotate your Fragments

```kotlin
@Screen(arg = MyArg::class)
class MyFragment : Fragment(R.layout.fragment_my)
```

## How It Works

The plugin:
1. Applies to all Kotlin compilations (`isApplicable` always returns `true`)
2. Adds the `com.adkhambek:screen-compiler:1.0-SNAPSHOT` artifact to the compiler classpath
3. No additional configuration options are required

## Building

```bash
cd screen-gradle-plugin
./gradlew build
```

## Publishing to Maven Local

```bash
cd screen-gradle-plugin
./gradlew publishToMavenLocal
```

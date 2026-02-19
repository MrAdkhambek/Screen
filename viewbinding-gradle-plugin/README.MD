# ViewBinding Gradle Plugin

Gradle plugin that integrates the ViewBinding compiler plugin into Kotlin compilations.

## Overview

This module provides a Gradle plugin that applies the `viewbinding-compiler` artifact to all Kotlin compilations. It automatically extracts the Android `namespace` from the Android Gradle plugin and passes it to the compiler plugin for ViewBinding class resolution.

## Plugin ID

```
com.adkhambek.viewbinding
```

## Usage

### Step 1: Include the plugin build

In your root `settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("viewbinding-gradle-plugin")
}
```

### Step 2: Apply the plugin

In your module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.adkhambek.viewbinding")
}
```

### Step 3: Enable ViewBinding in Android

```kotlin
android {
    buildFeatures.viewBinding = true
}
```

### Step 4: Use @Screen on your Fragments

```kotlin
@Screen
class SampleFragment : Fragment(R.layout.fragment_sample) {
    // `binding` property is generated automatically
    // Type: FragmentSampleBinding
}
```

## How It Works

The plugin:
1. Applies to all Kotlin compilations
2. Extracts the `namespace` from the Android Gradle plugin extension (e.g., `com.example.app`)
3. Passes the namespace to the compiler via the `namespace` CLI option
4. The compiler uses this to resolve binding classes under `{namespace}.databinding`

## Namespace Resolution

The namespace is extracted via reflection from the Android plugin:

```
android.namespace → "com.example.app"
→ Binding class: com.example.app.databinding.FragmentSampleBinding
```

If the Android plugin is not applied or the namespace is not set, the ViewBinding compiler plugin will not generate binding properties.

## Building

```bash
cd viewbinding-gradle-plugin
./gradlew build
```

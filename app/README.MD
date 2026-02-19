# Sample App

Android sample application demonstrating the Screen and ViewBinding compiler plugins.

## Overview

This is a minimal Android application that showcases how to use the `@Screen` annotation with both the Screen compiler plugin (for navigation constants and `createScreen()`) and the ViewBinding compiler plugin (for automatic `binding` property generation).

## Setup

The app applies both plugins in `build.gradle.kts`:

```kotlin
plugins {
    id("com.adkhambek.screen")
    id("com.adkhambek.viewbinding")
}
```

Compiler artifacts are substituted with local projects for development:

```kotlin
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.adkhambek:screen-compiler")).using(project(":screen-compiler"))
        substitute(module("com.adkhambek:viewbinding-compiler")).using(project(":viewbinding-compiler"))
    }
}
```

## Examples

### Basic Fragment with Arguments

```kotlin
@Parcelize
data class Arg(val id: Long) : Parcelable

@Screen(arg = Arg::class)
class SampleFragment : Fragment(R.layout.fragment_sample) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Access the generated arg property
        val data: Arg = arg
    }
}

// Navigate using the generated createScreen() function
router.navigateTo(SampleFragment.createScreen(arg = Arg(id = 1)))
```

### Fragment with Nullable Arguments

```kotlin
@Screen(arg = Arg::class, isNullable = true)
class NullableArgFragment : Fragment(R.layout.fragment_sample) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val data: Arg? = arg  // nullable
    }
}
```

### Fragment with ViewBinding

```kotlin
@Screen
class ViewBindingSampleFragment : Fragment(R.layout.fragment_sample) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Access the generated binding property
        binding.textView.text = "Hello"
    }
}
```

## Dependencies

- `screen-annotations` — Provides the `@Screen` annotation
- `androidx.fragment` — Android Fragment library
- `cicerone` — Fragment navigation library (used by generated `createScreen()`)
- `vbpd` — ViewBindingPropertyDelegate (used by generated `binding` delegate)

## Running

```bash
./gradlew :app:installDebug
```

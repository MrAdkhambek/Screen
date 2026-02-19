# Screen Annotations

Core annotation module for the Screen compiler plugin system.

## Overview

This module provides the `@Screen` annotation used to mark Android `Fragment` and `DialogFragment` classes for automatic code generation by the Screen and ViewBinding compiler plugins.

## Installation

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.adkhambek:screen-annotations:1.0-SNAPSHOT")
}
```

## Usage

### Basic Usage (no arguments)

```kotlin
@Screen
class HomeFragment : Fragment(R.layout.fragment_home)
```

This generates:
- `HomeFragment.KEY` — a `const val` equal to the fully qualified class name
- `HomeFragment.createScreen()` — a factory function returning `FragmentScreen`

### With Parcelable Arguments

```kotlin
@Parcelize
data class UserArg(val id: Long) : Parcelable

@Screen(arg = UserArg::class)
class UserFragment : Fragment(R.layout.fragment_user) {
    fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user = arg // Generated: private val arg: UserArg
    }
}
```

This additionally generates:
- `private val arg: UserArg` — a property that reads the argument from the Fragment's Bundle
- `UserFragment.createScreen(arg: UserArg)` — a factory that bundles the arg into the Fragment

### With Nullable Arguments

```kotlin
@Screen(arg = UserArg::class, isNullable = true)
class OptionalUserFragment : Fragment(R.layout.fragment_user) {
    fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user: UserArg? = arg // nullable
    }
}
```

When `isNullable = true`:
- The generated `arg` property returns `ArgType?` (nullable)
- The `createScreen()` function accepts `arg` with a default value of `null`

## Annotation Parameters

| Parameter    | Type         | Default       | Description                                      |
|-------------|-------------|---------------|--------------------------------------------------|
| `arg`       | `KClass<*>` | `Unit::class` | The Parcelable data class to pass as arguments   |
| `isNullable`| `Boolean`   | `false`       | Whether the arg can be null                      |

## Validation Rules

The compiler plugin enforces:
1. `@Screen` can only be applied to classes that extend `Fragment` or `DialogFragment`
2. The `arg` class must implement `android.os.Parcelable`

## Generated Code Summary

For a class `com.example.MyFragment` with `@Screen(arg = MyArg::class)`:

```kotlin
// Generated companion object (if not already present)
companion object {
    const val KEY: String = "com.example.MyFragment"

    fun createScreen(
        arg: MyArg,
        key: String? = null,
        clearContainer: Boolean = true
    ): FragmentScreen
}

// Generated on the fragment class itself
private val arg: MyArg  // reads from Bundle via BundleCompat.getParcelable()
```
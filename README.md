# Screen

A Kotlin K2 compiler plugin that generates companion object constants and utilities for Android Fragment classes annotated with `@Screen`.

## Features

- Generates `const val KEY` in the companion object, set to the fully qualified class name
- Generates a `private val arg` property for reading Parcelable arguments from the Fragment's Bundle
- Generates a `createScreen()` factory function when Cicerone `FragmentScreen` is on the classpath
- Companion FIR checkers validate annotation usage at compile time
- IntelliJ IDEA / Android Studio plugin with line markers, documentation, and inspections
- ViewBinding compiler plugin companion for view binding code generation

## Modules

| Module | Description |
|---|---|
| `screen-annotations` | `@Screen` annotation |
| `screen-compiler` | K2 compiler plugin (FIR + IR) |
| `screen-gradle-plugin` | Gradle plugin to apply the compiler plugin |
| `screen-idea-plugin` | IntelliJ IDEA plugin for IDE support |
| `viewbinding-compiler` | ViewBinding K2 compiler plugin |
| `viewbinding-gradle-plugin` | ViewBinding Gradle plugin |
| `viewbinding-idea-plugin` | ViewBinding IntelliJ IDEA plugin |
| `app` | Sample Android application |

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

## Requirements

- Kotlin 2.3.0+
- JDK 21+
- Android Gradle Plugin (for the sample app)

## Building

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew :screen-compiler:test
./gradlew :viewbinding-compiler:test
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
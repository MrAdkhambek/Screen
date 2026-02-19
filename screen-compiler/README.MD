# Screen Compiler

Kotlin K2 compiler plugin that generates navigation boilerplate for `@Screen`-annotated Fragment classes.

## Overview

This module is the core compiler plugin implementation using the Kotlin 2.3.0 K2 compiler plugin API. It operates in two phases:

1. **FIR (Frontend IR) Phase** — Declares new symbols (companion object, `KEY` property, `arg` property, `createScreen()` function)
2. **IR (Intermediate Representation) Phase** — Generates actual method bodies and constant values

## Architecture

```
screen-compiler/
├── src/main/kotlin/com/adkhambek/screen/compiler/
│   ├── ScreenCommandLineProcessor.kt     # CLI option handling
│   ├── ScreenCompilerPluginRegistrar.kt   # Plugin entry point
│   ├── fir/                               # FIR phase (declaration generation)
│   │   ├── ScreenFirExtensionRegistrar.kt
│   │   ├── ScreenFirDeclarationGenerationExtension.kt
│   │   ├── ScreenDeclarationKey.kt
│   │   ├── ScreenPredicate.kt
│   │   ├── ScreenCheckersExtension.kt
│   │   ├── ScreenClassChecker.kt
│   │   └── ScreenErrors.kt
│   └── ir/                                # IR phase (code generation)
│       ├── ScreenIrGenerationExtension.kt
│       └── ScreenIrElementTransformer.kt
├── src/test/kotlin/                       # Tests
│   ├── TestHelper.kt
│   ├── ScreenDeclarationGenerationTest.kt
│   ├── ScreenDiagnosticsTest.kt
│   └── ScreenIrGenerationTest.kt
└── src/main/resources/META-INF/services/  # SPI registration
```

## How It Works

### FIR Phase

The `ScreenFirDeclarationGenerationExtension` handles declaration generation:

- **Companion Object**: If a `@Screen` class lacks a companion object, one is generated
- **`KEY` Property**: A `const val KEY: String` is added to the companion object with the class's fully qualified name as its value
- **`arg` Property**: If `@Screen(arg = ...)` specifies a class, a `private val arg: ArgType` property is added to the fragment class
- **`createScreen()` Function**: If the Cicerone `FragmentScreen` class is available on the classpath, a factory function is generated on the companion

The `ScreenClassChecker` validates:
- The annotated class extends `Fragment` or `DialogFragment`
- The `arg` class implements `Parcelable`

### IR Phase

The `ScreenIrElementTransformer` generates method bodies:

- **`KEY` initializer**: Sets the backing field to the FQN string constant
- **`arg` getter**: Reads from `Fragment.getArguments()` using `BundleCompat.getParcelable()`
- **`createScreen()` body**: Creates a `FragmentScreen` with a `Creator` lambda that instantiates the fragment via `FragmentFactory.instantiate()` and optionally bundles the arg

## Plugin ID

```
com.adkhambek.screen.compiler
```

## Building

```bash
./gradlew :screen-compiler:build
```

## Testing

```bash
./gradlew :screen-compiler:test
```

Tests use [kotlin-compile-testing](https://github.com/ZacSweers/kotlin-compile-testing) (kctfork) to verify:
- Declaration generation (FIR phase)
- Diagnostic errors (validation)
- IR code generation (runtime behavior)

## Publishing to Maven Local

```bash
./gradlew :screen-compiler:publishToMavenLocal
```

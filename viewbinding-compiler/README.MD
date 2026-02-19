# ViewBinding Compiler

Kotlin K2 compiler plugin that generates automatic `binding` properties for `@Screen`-annotated Fragment classes.

## Overview

This module is a companion compiler plugin to the Screen plugin. It automatically generates a `private val binding: XxxBinding` property on `@Screen`-annotated Fragment classes, derived from the layout resource passed to the Fragment's super constructor.

## Architecture

```
viewbinding-compiler/
├── src/main/kotlin/com/adkhambek/viewbinding/compiler/
│   ├── ViewBindingCommandLineProcessor.kt     # CLI option handling (namespace)
│   ├── ViewBindingCompilerPluginRegistrar.kt   # Plugin entry point
│   ├── fir/                                    # FIR phase (declaration generation)
│   │   ├── ViewBindingFirExtensionRegistrar.kt
│   │   ├── ViewBindingFirDeclarationGenerationExtension.kt
│   │   ├── ViewBindingDeclarationKey.kt
│   │   ├── ViewBindingPredicate.kt
│   │   ├── ViewBindingCheckersExtension.kt
│   │   ├── ViewBindingClassChecker.kt
│   │   └── ViewBindingErrors.kt
│   └── ir/                                     # IR phase (code generation)
│       ├── ViewBindingIrGenerationExtension.kt
│       └── ViewBindingIrElementTransformer.kt
├── src/test/kotlin/                            # Tests
│   ├── ViewBindingTestHelper.kt
│   ├── ViewBindingDeclarationGenerationTest.kt
│   ├── ViewBindingDiagnosticsTest.kt
│   └── ViewBindingIrGenerationTest.kt
└── src/main/resources/META-INF/services/       # SPI registration
```

## How It Works

### Layout-to-Binding Resolution

The plugin reads the layout resource from the Fragment's primary constructor delegation:

```kotlin
@Screen
class SampleFragment : Fragment(R.layout.fragment_sample)
//                                        ^^^^^^^^^^^^^^^ extracted
```

The layout name `fragment_sample` is converted to PascalCase and appended with `Binding`:
- `fragment_sample` → `FragmentSampleBinding`
- `fragment_user_profile` → `FragmentUserProfileBinding`

The binding class is resolved under `{namespace}.databinding.{BindingClassName}`.

### FIR Phase

The `ViewBindingFirDeclarationGenerationExtension`:

1. Extracts the layout resource name from the Fragment's super constructor call
2. Converts the snake_case layout name to a PascalCase binding class name
3. Verifies the binding class exists in the `{namespace}.databinding` package
4. Generates a `private val binding: XxxBinding` property on the Fragment class

### IR Phase

The `ViewBindingIrElementTransformer` generates the binding getter with two strategies:

1. **Delegate approach** (preferred): If the [VBPD](https://github.com/nickelaway/ViewBindingPropertyDelegate) library is available, uses `viewBinding { fragment -> XxxBinding.bind(fragment.requireView()) }` with proper delegate fields
2. **Direct approach** (fallback): Calls `XxxBinding.bind(requireView())` directly on every access (no caching)

### CLI Options

| Option      | Description                                  | Required |
|------------|----------------------------------------------|----------|
| `namespace`| Android namespace for binding class resolution | No       |

The namespace is automatically extracted from the Android Gradle plugin by the ViewBinding Gradle plugin.

## Plugin ID

```
com.adkhambek.viewbinding.compiler
```

## Building

```bash
./gradlew :viewbinding-compiler:build
```

## Testing

```bash
./gradlew :viewbinding-compiler:test
```

## Publishing to Maven Local

```bash
./gradlew :viewbinding-compiler:publishToMavenLocal
```

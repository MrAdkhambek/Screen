// Package declaration for the Screen annotation module.
package com.adkhambek.screen

// Import KClass to allow specifying argument types via class references (e.g., MyArg::class).
import kotlin.reflect.KClass

// Annotation that marks a Fragment or DialogFragment class for code generation by the Screen compiler plugin.
// When applied, the compiler generates:
//   1. A companion object (if not already present)
//   2. A `const val KEY: String` in the companion, set to the fully qualified class name
//   3. A `private val arg: ArgType` property on the fragment (if `arg` is specified and is not Unit)
//   4. A `fun createScreen(...)` factory function in the companion (if Cicerone FragmentScreen is on the classpath)
//
// The annotation target is restricted to classes only (not functions, properties, etc.).
// Retention is BINARY, meaning the annotation is stored in the compiled .class file
// but is not accessible at runtime via reflection — it is only needed at compile time.
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Screen(
    // The KClass of the Parcelable data class to pass as arguments to the Fragment.
    // Defaults to Unit::class, which means no arguments are expected.
    // When a non-Unit class is specified, the compiler generates:
    //   - A `private val arg: ArgType` property that reads the argument from the Fragment's Bundle
    //   - A `createScreen(arg: ArgType, ...)` function that bundles the argument into the Fragment
    // The specified class must implement android.os.Parcelable; the compiler will report an error otherwise.
    val arg: KClass<*> = Unit::class,

    // Whether the argument can be null.
    // When true, the generated `arg` property returns `ArgType?` (nullable),
    // and the `createScreen()` function accepts `arg` with a default value of null.
    // When false (default), the argument is required and the getter calls requireNotNull().
    val isNullable: Boolean = false
)

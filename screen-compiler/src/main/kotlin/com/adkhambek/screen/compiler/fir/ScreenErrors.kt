// Package declaration for the FIR phase of the Screen compiler plugin.
package com.adkhambek.screen.compiler.fir

// Import the factory-to-renderer map that associates diagnostic factories with human-readable messages.
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
// Import the base interface for diagnostic containers that group related error definitions.
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
// Import the error0 delegate which creates a diagnostic factory with no additional parameters.
import org.jetbrains.kotlin.diagnostics.error0
// Import the base class for diagnostic renderer factories that provide message formatting.
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
// Import KtElement which is the PSI base type for Kotlin source elements used in error reporting.
import org.jetbrains.kotlin.psi.KtElement

// Container object for all diagnostic errors reported by the Screen compiler plugin.
// Each error is defined as a delegated property using `error0<KtElement>()` which creates
// a diagnostic factory that takes no additional parameters and can be reported on any KtElement.
// These errors are used in ScreenClassChecker to report invalid @Screen usage.
object ScreenErrors : KtDiagnosticsContainer() {
    // Error reported when @Screen is applied to a class that does not extend Fragment or DialogFragment.
    // The checker recursively walks the supertype chain to determine Fragment inheritance.
    val SCREEN_NOT_ON_FRAGMENT by error0<KtElement>()

    // Error reported when the @Screen `arg` parameter specifies a class that does not implement
    // android.os.Parcelable. This is required because arguments are passed via Bundle, which
    // only supports Parcelable types for complex objects.
    val SCREEN_ARG_NOT_PARCELABLE by error0<KtElement>()

    // Returns the renderer factory that maps these diagnostic factories to human-readable messages.
    override fun getRendererFactory() = ScreenErrorMessages
}

// Renderer factory that provides user-facing error messages for Screen diagnostics.
// These messages are displayed in the IDE and compiler output when validation errors occur.
object ScreenErrorMessages : BaseDiagnosticRendererFactory() {
    // Lazy-initialized map that associates each diagnostic factory with its display message.
    // The "Screen" string is the diagnostic group name used for categorization.
    override val MAP by KtDiagnosticFactoryToRendererMap("Screen") { map ->
        // Message for SCREEN_NOT_ON_FRAGMENT: tells the developer that @Screen requires Fragment inheritance.
        map.put(
            ScreenErrors.SCREEN_NOT_ON_FRAGMENT,
            "@Screen can only be applied to classes that extend Fragment or DialogFragment."
        )
        // Message for SCREEN_ARG_NOT_PARCELABLE: tells the developer that the arg class must be Parcelable.
        map.put(
            ScreenErrors.SCREEN_ARG_NOT_PARCELABLE,
            "@Screen 'arg' class must implement android.os.Parcelable."
        )
    }
}

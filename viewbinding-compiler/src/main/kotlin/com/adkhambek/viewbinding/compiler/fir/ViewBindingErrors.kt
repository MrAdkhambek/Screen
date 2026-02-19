// Package declaration for the FIR phase of the ViewBinding compiler plugin.
package com.adkhambek.viewbinding.compiler.fir

// Import the factory-to-renderer map for associating diagnostic factories with messages.
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
// Import the base interface for diagnostic containers.
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
// Import error0 delegate for creating diagnostic factories with no parameters.
import org.jetbrains.kotlin.diagnostics.error0
// Import the base class for diagnostic renderer factories.
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
// Import KtElement, the PSI base type for error reporting.
import org.jetbrains.kotlin.psi.KtElement

// Container object for all diagnostic errors reported by the ViewBinding compiler plugin.
// Each error is defined as a delegated property using error0<KtElement>().
// These errors are reported by ViewBindingClassChecker during the FIR analysis phase.
object ViewBindingErrors : KtDiagnosticsContainer() {
    // Error reported when @Screen is applied to a class that does not extend Fragment or DialogFragment.
    // The ViewBinding plugin requires Fragment inheritance because the generated binding
    // property uses Fragment.requireView() to bind the layout.
    val VIEW_BINDING_NOT_ON_FRAGMENT by error0<KtElement>()

    // Error reported when the Fragment doesn't specify a layout resource in its constructor.
    // The ViewBinding plugin needs the layout name to determine which binding class to generate.
    // For example: Fragment(R.layout.fragment_sample) provides "fragment_sample".
    val VIEW_BINDING_MISSING_LAYOUT by error0<KtElement>()

    // Returns the renderer factory that maps these diagnostic factories to user-facing messages.
    override fun getRendererFactory() = ViewBindingErrorMessages
}

// Renderer factory that provides user-facing error messages for ViewBinding diagnostics.
object ViewBindingErrorMessages : BaseDiagnosticRendererFactory() {
    // Lazy-initialized map associating each diagnostic factory with its display message.
    override val MAP by KtDiagnosticFactoryToRendererMap("ViewBinding") { map ->
        // Message for VIEW_BINDING_NOT_ON_FRAGMENT: @Screen requires Fragment inheritance.
        map.put(
            ViewBindingErrors.VIEW_BINDING_NOT_ON_FRAGMENT,
            "@Screen can only be applied to classes that extend Fragment or DialogFragment."
        )
        // Message for VIEW_BINDING_MISSING_LAYOUT: @Screen requires a layout resource in the constructor.
        map.put(
            ViewBindingErrors.VIEW_BINDING_MISSING_LAYOUT,
            "@Screen requires a layout resource in the Fragment constructor (e.g., Fragment(R.layout.fragment_sample))."
        )
    }
}

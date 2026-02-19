// Package declaration for the FIR phase of the ViewBinding compiler plugin.
package com.adkhambek.viewbinding.compiler.fir

// Import FirSession which represents the current compilation session.
import org.jetbrains.kotlin.fir.FirSession
// Import the base class for FIR extension registrars.
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

// FIR extension registrar for the ViewBinding compiler plugin.
// This class is instantiated by ViewBindingCompilerPluginRegistrar and registers
// all FIR-phase extensions. It receives the Android namespace (e.g., "com.example.app")
// from the Gradle plugin via the compiler configuration, which is needed to resolve
// ViewBinding class names under {namespace}.databinding.
//
// Two extensions are registered:
//   1. ViewBindingFirDeclarationGenerationExtension — generates the `binding` property
//   2. ViewBindingCheckersExtension — validates @Screen usage on Fragment classes
class ViewBindingFirExtensionRegistrar(private val namespace: String? = null) : FirExtensionRegistrar() {
    // Called by the FIR infrastructure to configure the plugin's extensions.
    override fun ExtensionRegistrarContext.configurePlugin() {
        // Register the declaration generation extension with the namespace.
        // The namespace is passed to the extension for resolving binding class names.
        +{ session: FirSession -> ViewBindingFirDeclarationGenerationExtension(session, namespace) }

        // Register the checkers extension for validating @Screen annotation usage.
        +::ViewBindingCheckersExtension
    }
}

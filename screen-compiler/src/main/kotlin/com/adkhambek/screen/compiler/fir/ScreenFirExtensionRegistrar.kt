// Package declaration for the FIR (Frontend IR) phase of the Screen compiler plugin.
package com.adkhambek.screen.compiler.fir

// Import FirSession which represents the current compilation session and provides access to all FIR services.
import org.jetbrains.kotlin.fir.FirSession
// Import the base class for FIR extension registrars that configure plugin extensions.
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

// FIR extension registrar for the Screen compiler plugin.
// This class is the entry point for registering all FIR-phase extensions.
// It is instantiated by ScreenCompilerPluginRegistrar and registered via FirExtensionRegistrarAdapter.
// Two extensions are registered:
//   1. ScreenFirDeclarationGenerationExtension — generates companion objects, KEY, arg, createScreen()
//   2. ScreenCheckersExtension — validates @Screen usage (Fragment inheritance, Parcelable arg)
class ScreenFirExtensionRegistrar : FirExtensionRegistrar() {
    // Called by the FIR infrastructure to configure the plugin's extensions.
    // The `+` operator is a DSL shorthand for registering extension factories.
    override fun ExtensionRegistrarContext.configurePlugin() {
        // Register the declaration generation extension.
        // The lambda takes a FirSession parameter and returns the extension instance.
        // This extension handles generating companion objects, KEY constants, arg properties,
        // and createScreen() functions for @Screen-annotated classes.
        +{ session: FirSession -> ScreenFirDeclarationGenerationExtension(session) }

        // Register the additional checkers extension using a constructor reference.
        // This extension provides diagnostic checkers that validate @Screen annotation usage,
        // ensuring the class extends Fragment and the arg type implements Parcelable.
        +::ScreenCheckersExtension
    }
}

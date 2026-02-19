// Package declaration for the ViewBinding compiler plugin's registration entry point.
package com.adkhambek.viewbinding.compiler

// Import the FIR extension registrar that handles declaration generation and checkers.
import com.adkhambek.viewbinding.compiler.fir.ViewBindingFirExtensionRegistrar
// Import the IR generation extension that handles code generation for the binding getter.
import com.adkhambek.viewbinding.compiler.ir.ViewBindingIrGenerationExtension
// Import the IrGenerationExtension interface used to register IR phase extensions.
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
// Import the base class for compiler plugin registrars in K2.
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
// Import the experimental API opt-in annotation required for compiler plugin APIs.
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
// Import CompilerConfiguration which provides access to CLI options and settings.
import org.jetbrains.kotlin.config.CompilerConfiguration
// Import the FIR extension registrar adapter used to register FIR-phase extensions.
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

// Opt into the experimental compiler plugin API.
@OptIn(ExperimentalCompilerApi::class)
// Main entry point for the ViewBinding compiler plugin.
// This class is discovered via META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.
// It registers both FIR and IR extensions, passing the Android namespace from the CLI options
// to the FIR extension for binding class resolution.
class ViewBindingCompilerPluginRegistrar : CompilerPluginRegistrar() {
    // Unique identifier matching the CommandLineProcessor's pluginId and the Gradle plugin's compilerPluginId.
    override val pluginId: String = "com.adkhambek.viewbinding.compiler"

    // Declares that this plugin supports the K2 compiler (FIR-based frontend).
    override val supportsK2: Boolean = true

    // Called by the Kotlin compiler to register all extensions provided by this plugin.
    // Reads the namespace from the compiler configuration (set by ViewBindingCommandLineProcessor)
    // and passes it to the FIR extension for resolving ViewBinding class names.
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Read the Android namespace from the compiler configuration.
        // This was set by ViewBindingCommandLineProcessor.processOption().
        // May be null if the namespace option was not provided (e.g., non-Android projects).
        val namespace = configuration.get(NAMESPACE_KEY)
        // Register the FIR extension with the namespace for binding class resolution.
        FirExtensionRegistrarAdapter.registerExtension(ViewBindingFirExtensionRegistrar(namespace))
        // Register the IR extension for generating binding getter bodies.
        IrGenerationExtension.registerExtension(ViewBindingIrGenerationExtension())
    }
}

// Package declaration for the Screen compiler plugin's registration entry point.
package com.adkhambek.screen.compiler

// Import the FIR extension registrar that handles declaration generation and checkers.
import com.adkhambek.screen.compiler.fir.ScreenFirExtensionRegistrar
// Import the IR generation extension that handles code generation for method bodies.
import com.adkhambek.screen.compiler.ir.ScreenIrGenerationExtension
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
// Main entry point for the Screen compiler plugin.
// This class is discovered via META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.
// It registers both FIR (Frontend IR) and IR (Intermediate Representation) extensions.
class ScreenCompilerPluginRegistrar : CompilerPluginRegistrar() {
    // Unique identifier matching the CommandLineProcessor's pluginId and the Gradle plugin's compilerPluginId.
    override val pluginId: String = "com.adkhambek.screen.compiler"

    // Declares that this plugin supports the K2 compiler (FIR-based frontend).
    // Must be true for plugins that register FIR extensions.
    override val supportsK2: Boolean = true

    // Called by the Kotlin compiler to register all extensions provided by this plugin.
    // This registers two extensions:
    //   1. FIR extension — handles declaration generation (companion object, KEY, arg, createScreen)
    //      and diagnostic checks (validates Fragment inheritance and Parcelable arg).
    //   2. IR extension — generates actual method/property bodies in the IR lowering phase.
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Register the FIR extension registrar which sets up declaration generation and checkers.
        FirExtensionRegistrarAdapter.registerExtension(ScreenFirExtensionRegistrar())
        // Register the IR generation extension which fills in method bodies and constant values.
        IrGenerationExtension.registerExtension(ScreenIrGenerationExtension())
    }
}

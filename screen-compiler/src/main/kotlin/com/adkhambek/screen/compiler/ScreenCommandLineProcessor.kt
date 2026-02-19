// Package declaration for the Screen compiler plugin's command-line processing.
package com.adkhambek.screen.compiler

// Import the base class for defining CLI options accepted by the compiler plugin.
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
// Import the CommandLineProcessor interface that the Kotlin compiler calls to process CLI arguments.
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
// Import the experimental API opt-in annotation required for compiler plugin APIs.
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
// Import CompilerConfiguration which stores key-value configuration options for the compilation.
import org.jetbrains.kotlin.config.CompilerConfiguration

// Opt into the experimental compiler plugin API, which is required for all K2 plugin classes.
@OptIn(ExperimentalCompilerApi::class)
// CommandLineProcessor implementation for the Screen compiler plugin.
// This class is discovered via META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor.
// It defines the plugin's unique ID and any CLI options it accepts.
// The Screen plugin does not require any CLI options, so pluginOptions is empty.
class ScreenCommandLineProcessor : CommandLineProcessor {
    // Unique identifier for this compiler plugin, matching the ID used by the Gradle plugin.
    // This must match the value returned by ScreenSubplugin.getCompilerPluginId().
    override val pluginId: String = "com.adkhambek.screen.compiler"

    // Collection of CLI options this plugin accepts. Empty because the Screen plugin
    // does not require any external configuration — all information comes from the source code.
    override val pluginOptions: Collection<AbstractCliOption> = emptyList()

    // Callback invoked for each CLI option passed to this plugin.
    // Since we define no options, this method is a no-op.
    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
    }
}

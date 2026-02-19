// Package declaration for the ViewBinding compiler plugin's command-line processing.
package com.adkhambek.viewbinding.compiler

// Import the base class for defining CLI options accepted by the compiler plugin.
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
// Import CliOption for creating individual CLI option definitions.
import org.jetbrains.kotlin.compiler.plugin.CliOption
// Import the CommandLineProcessor interface that the Kotlin compiler calls to process CLI arguments.
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
// Import the experimental API opt-in annotation required for compiler plugin APIs.
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
// Import CompilerConfiguration which stores key-value configuration options for the compilation.
import org.jetbrains.kotlin.config.CompilerConfiguration
// Import CompilerConfigurationKey for defining typed keys to store/retrieve values in CompilerConfiguration.
import org.jetbrains.kotlin.config.CompilerConfigurationKey

// Configuration key for storing the Android namespace passed from the Gradle plugin.
// The namespace is used to resolve ViewBinding classes under {namespace}.databinding.
// For example, if namespace is "com.example.app", binding classes are resolved at
// "com.example.app.databinding.FragmentSampleBinding".
val NAMESPACE_KEY = CompilerConfigurationKey<String>("android namespace")

// Opt into the experimental compiler plugin API.
@OptIn(ExperimentalCompilerApi::class)
// CommandLineProcessor implementation for the ViewBinding compiler plugin.
// This class is discovered via META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor.
// It defines the plugin's unique ID and accepts a "namespace" CLI option from the Gradle plugin.
class ViewBindingCommandLineProcessor : CommandLineProcessor {
    // Unique identifier for this compiler plugin, matching the ViewBindingSubplugin's compilerPluginId.
    override val pluginId: String = "com.adkhambek.viewbinding.compiler"

    // Collection of CLI options accepted by this plugin.
    // The "namespace" option receives the Android namespace from the Gradle plugin,
    // which is needed to resolve ViewBinding class names at compile time.
    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption("namespace", "<namespace>", "Android namespace for ViewBinding resolution", required = false),
    )

    // Callback invoked for each CLI option passed to this plugin.
    // Stores the namespace value in the CompilerConfiguration for later use by the registrar.
    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            // Store the namespace in the compiler configuration under NAMESPACE_KEY.
            "namespace" -> configuration.put(NAMESPACE_KEY, value)
        }
    }
}

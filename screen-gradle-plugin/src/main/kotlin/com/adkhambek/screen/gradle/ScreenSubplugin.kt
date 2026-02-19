// Package declaration for the Screen Gradle plugin module.
package com.adkhambek.screen.gradle

// Import Project which represents a Gradle project in the build system.
import org.gradle.api.Project
// Import Provider for lazy evaluation of configuration values.
import org.gradle.api.provider.Provider
// Import KotlinCompilation which represents a single Kotlin compilation unit (main, test, etc.).
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
// Import KotlinCompilerPluginSupportPlugin, the interface for Gradle plugins that add Kotlin compiler plugins.
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
// Import SubpluginArtifact which specifies the Maven coordinates of the compiler plugin JAR.
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
// Import SubpluginOption which represents a key-value CLI option passed to the compiler plugin.
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

// Gradle plugin that integrates the Screen compiler plugin into Kotlin compilations.
// This class implements KotlinCompilerPluginSupportPlugin, which is the standard way to
// add a Kotlin compiler plugin via Gradle. It is registered in build.gradle.kts under
// the gradlePlugin block with id "com.adkhambek.screen".
// When applied to a project, it automatically adds the screen-compiler artifact to the
// Kotlin compiler classpath for all compilations.
class ScreenSubplugin : KotlinCompilerPluginSupportPlugin {

    // Called when the plugin is applied to a Gradle project.
    // No additional configuration is needed for the Screen plugin,
    // so this method is intentionally empty.
    override fun apply(target: Project) {}

    // Determines whether this compiler plugin should be applied to a given Kotlin compilation.
    // Returns true for all compilations (main, test, etc.) so the Screen plugin is always active.
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    // Provides CLI options to pass to the compiler plugin for each compilation.
    // The Screen plugin does not require any CLI options, so this returns an empty list.
    // The Provider wrapper enables lazy evaluation (the list is computed only when needed).
    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList() }
    }

    // Returns the unique plugin ID that matches the CommandLineProcessor's pluginId
    // and the CompilerPluginRegistrar's pluginId in the screen-compiler module.
    override fun getCompilerPluginId(): String = "com.adkhambek.screen.compiler"

    // Returns the Maven coordinates of the screen-compiler artifact.
    // Gradle uses these coordinates to resolve the compiler plugin JAR and add it
    // to the Kotlin compiler's classpath during compilation.
    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.adkhambek",
        artifactId = "screen-compiler",
        version = "1.0-SNAPSHOT",
    )
}

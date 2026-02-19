// Package declaration for the ViewBinding Gradle plugin module.
package com.adkhambek.viewbinding.gradle

// Import Project which represents a Gradle project.
import org.gradle.api.Project
// Import Provider for lazy evaluation of configuration values.
import org.gradle.api.provider.Provider
// Import KotlinCompilation which represents a single Kotlin compilation unit.
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
// Import KotlinCompilerPluginSupportPlugin for Gradle-Kotlin compiler plugin integration.
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
// Import SubpluginArtifact for specifying Maven coordinates of the compiler plugin JAR.
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
// Import SubpluginOption for passing CLI options to the compiler plugin.
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

// Gradle plugin that integrates the ViewBinding compiler plugin into Kotlin compilations.
// This class implements KotlinCompilerPluginSupportPlugin and is registered in build.gradle.kts
// with id "com.adkhambek.viewbinding".
//
// Unlike the Screen Gradle plugin, this plugin actively extracts the Android namespace
// from the Android Gradle plugin extension and passes it to the compiler plugin.
// The namespace is essential for resolving ViewBinding class names under {namespace}.databinding.
class ViewBindingSubplugin : KotlinCompilerPluginSupportPlugin {

    // Called when the plugin is applied to a Gradle project.
    // No additional configuration is needed at apply time.
    override fun apply(target: Project) {}

    // Determines whether this compiler plugin should be applied to a given compilation.
    // Returns true for all compilations so ViewBinding generation is always active.
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    // Provides CLI options to pass to the compiler plugin for each compilation.
    // Extracts the Android namespace from the Android Gradle plugin and passes it as
    // the "namespace" option. This is used by the compiler to resolve binding class names.
    //
    // The extraction uses reflection because the Gradle plugin module doesn't have a
    // compile-time dependency on the Android Gradle plugin. This allows the plugin to
    // gracefully handle non-Android projects.
    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        return project.provider {
            val options = mutableListOf<SubpluginOption>()
            try {
                // Look up the "android" extension from the Android Gradle plugin.
                val androidExtension = project.extensions.findByName("android")
                if (androidExtension != null) {
                    // Extract the namespace via reflection (getNamespace() method).
                    // The namespace is the Android application/library ID (e.g., "com.example.app").
                    val namespace = androidExtension.javaClass
                        .getMethod("getNamespace")
                        .invoke(androidExtension) as? String
                    if (!namespace.isNullOrBlank()) {
                        // Pass the namespace to the compiler plugin as a CLI option.
                        options += SubpluginOption("namespace", namespace)
                    }
                }
            } catch (_: Exception) {
                // Silently ignore if the Android plugin is not applied or namespace is unavailable.
                // In this case, the compiler plugin will not generate binding properties.
            }
            options
        }
    }

    // Returns the unique plugin ID matching the ViewBindingCommandLineProcessor's pluginId.
    override fun getCompilerPluginId(): String = "com.adkhambek.viewbinding.compiler"

    // Returns the Maven coordinates of the viewbinding-compiler artifact.
    // Gradle resolves this artifact and adds it to the Kotlin compiler's classpath.
    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.adkhambek",
        artifactId = "viewbinding-compiler",
        version = "1.0-SNAPSHOT",
    )
}

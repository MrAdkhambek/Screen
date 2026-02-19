// Package declaration for the Screen IDEA plugin module.
package com.adkhambek.screen.idea

// Import PluginManagerCore for accessing installed IDEA plugin metadata and paths.
import com.intellij.ide.plugins.PluginManagerCore
// Import PluginId for looking up IDEA plugins by their unique identifier.
import com.intellij.openapi.extensions.PluginId
// Import Project which represents an open IDEA project.
import com.intellij.openapi.project.Project
// Import KotlinBundledFirCompilerPluginProvider, the extension point interface for
// providing bundled compiler plugin JARs to the K2 FIR analysis engine in the IDE.
import org.jetbrains.kotlin.idea.fir.extensions.KotlinBundledFirCompilerPluginProvider
// Import Files for filesystem operations (exists, list).
import java.nio.file.Files
// Import Path for representing filesystem paths.
import java.nio.file.Path

// Provides the bundled screen-compiler JAR to the Kotlin K2 FIR analysis engine in the IDE.
// When the IDE's Kotlin plugin needs to analyze code that uses a compiler plugin,
// it calls this provider to locate the compiler plugin JAR. This enables real-time
// resolution of generated declarations (KEY, arg, createScreen) in the editor without
// needing to build the project first.
//
// This class is registered as an extension in plugin.xml:
//   <bundledFirCompilerPluginProvider implementation="...ScreenBundledCompilerPluginProvider"/>
class ScreenBundledCompilerPluginProvider : KotlinBundledFirCompilerPluginProvider {

    // Called by the Kotlin IDE plugin to provide the path to the compiler plugin JAR.
    // The userSuppliedPluginJar parameter is the path that Gradle configured for the
    // compiler plugin artifact; we check if it matches our plugin and provide the
    // bundled JAR from this IDEA plugin's lib directory instead.
    //
    // Returns the path to the bundled screen-compiler JAR, or null if:
    //   - The requested JAR doesn't match "screen-compiler" (not our plugin)
    //   - This IDEA plugin is not installed
    //   - The lib directory doesn't contain the compiler JAR
    override fun provideBundledPluginJar(project: Project, userSuppliedPluginJar: Path): Path? {
        // Extract the filename from the user-supplied JAR path.
        val fileName = userSuppliedPluginJar.fileName?.toString() ?: return null
        // Only handle requests for the screen-compiler JAR.
        if (!fileName.startsWith("screen-compiler")) return null

        // Look up this IDEA plugin's descriptor by its unique ID.
        val descriptor = PluginManagerCore.getPlugin(
            PluginId.getId("com.adkhambek.screen.idea")
        ) ?: return null

        // Resolve the lib directory within the IDEA plugin's installation path.
        val libDir = descriptor.pluginPath.resolve("lib")
        // Verify the lib directory exists.
        if (!Files.exists(libDir)) return null

        // Search the lib directory for a JAR file matching "screen-compiler*.jar".
        // This finds the bundled compiler JAR regardless of its exact version suffix.
        return Files.list(libDir).use { stream ->
            stream.filter { path ->
                val name = path.fileName.toString()
                name.startsWith("screen-compiler") && name.endsWith(".jar")
            }.findFirst().orElse(null)
        }
    }
}

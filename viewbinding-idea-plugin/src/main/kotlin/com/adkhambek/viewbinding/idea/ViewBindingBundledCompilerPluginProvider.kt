// Package declaration for the ViewBinding IDEA plugin module.
package com.adkhambek.viewbinding.idea

// Import PluginManagerCore for accessing installed IDEA plugin metadata and paths.
import com.intellij.ide.plugins.PluginManagerCore
// Import PluginId for looking up IDEA plugins by their unique identifier.
import com.intellij.openapi.extensions.PluginId
// Import Project which represents an open IDEA project.
import com.intellij.openapi.project.Project
// Import KotlinBundledFirCompilerPluginProvider for providing bundled compiler plugin JARs to K2 FIR.
import org.jetbrains.kotlin.idea.fir.extensions.KotlinBundledFirCompilerPluginProvider
// Import Files for filesystem operations.
import java.nio.file.Files
// Import Path for filesystem path representation.
import java.nio.file.Path

// Provides the bundled viewbinding-compiler JAR to the Kotlin K2 FIR analysis engine in the IDE.
// This enables real-time resolution of the generated `binding` property in the editor
// without needing to build the project first.
//
// Registered in plugin.xml:
//   <bundledFirCompilerPluginProvider implementation="...ViewBindingBundledCompilerPluginProvider"/>
class ViewBindingBundledCompilerPluginProvider : KotlinBundledFirCompilerPluginProvider {

    // Called by the Kotlin IDE plugin to provide the path to the compiler plugin JAR.
    // Returns the bundled viewbinding-compiler JAR from this IDEA plugin's lib directory,
    // or null if the requested JAR doesn't match or isn't available.
    override fun provideBundledPluginJar(project: Project, userSuppliedPluginJar: Path): Path? {
        // Extract the filename from the user-supplied JAR path.
        val fileName = userSuppliedPluginJar.fileName?.toString() ?: return null
        // Only handle requests for the viewbinding-compiler JAR.
        if (!fileName.startsWith("viewbinding-compiler")) return null

        // Look up this IDEA plugin by its unique ID.
        val descriptor = PluginManagerCore.getPlugin(
            PluginId.getId("com.adkhambek.viewbinding.idea")
        ) ?: return null

        // Resolve the lib directory within the plugin's installation path.
        val libDir = descriptor.pluginPath.resolve("lib")
        if (!Files.exists(libDir)) return null

        // Search for a matching JAR file in the lib directory.
        return Files.list(libDir).use { stream ->
            stream.filter { path ->
                val name = path.fileName.toString()
                name.startsWith("viewbinding-compiler") && name.endsWith(".jar")
            }.findFirst().orElse(null)
        }
    }
}

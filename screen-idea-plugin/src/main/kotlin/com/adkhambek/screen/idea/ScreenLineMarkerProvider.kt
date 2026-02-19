// Package declaration for the Screen IDEA plugin module.
package com.adkhambek.screen.idea

// Import LineMarkerInfo which represents a gutter icon/marker on a specific line in the editor.
import com.intellij.codeInsight.daemon.LineMarkerInfo
// Import LineMarkerProvider, the extension point interface for providing gutter line markers.
import com.intellij.codeInsight.daemon.LineMarkerProvider
// Import AllIcons for accessing built-in IntelliJ icon resources.
import com.intellij.icons.AllIcons
// Import GutterIconRenderer for controlling gutter icon alignment and behavior.
import com.intellij.openapi.editor.markup.GutterIconRenderer
// Import PsiElement, the base class for all PSI tree nodes.
import com.intellij.psi.PsiElement
// Import LeafPsiElement which represents a leaf (terminal) node in the PSI tree (a single token).
import com.intellij.psi.impl.source.tree.LeafPsiElement
// Import analyze for Kotlin Analysis API operations.
import org.jetbrains.kotlin.analysis.api.analyze
// Import KtTokens which contains token type constants for Kotlin lexer tokens.
import org.jetbrains.kotlin.lexer.KtTokens
// Import ClassId for identifying the @Screen annotation class.
import org.jetbrains.kotlin.name.ClassId
// Import FqName for fully qualified name representation.
import org.jetbrains.kotlin.name.FqName
// Import Name for simple identifier representation.
import org.jetbrains.kotlin.name.Name
// Import KtClass which represents a class declaration in the PSI tree.
import org.jetbrains.kotlin.psi.KtClass
// Import KtClassOrObject, the base class for class and object declarations.
import org.jetbrains.kotlin.psi.KtClassOrObject

// Line marker provider that displays a plugin icon in the editor gutter next to
// @Screen-annotated classes. The gutter icon provides a visual indicator that
// code generation is active for this class, and shows a tooltip with the
// generated constants (e.g., KEY = "com.example.MyFragment").
//
// Registered in plugin.xml as a codeInsight.lineMarkerProvider for the Kotlin language.
// Line markers are only applied to leaf PSI elements (tokens) for performance reasons,
// so we check for the CLASS_KEYWORD token and navigate up to the class declaration.
class ScreenLineMarkerProvider : LineMarkerProvider {

    // ClassId for the @Screen annotation, used to verify the class is annotated.
    private val screenClassId = ClassId(
        FqName("com.adkhambek.screen"),
        Name.identifier("Screen"),
    )

    // Called for each PSI element in the editor to determine if a line marker should be shown.
    // For performance, line markers should only be placed on leaf elements (tokens).
    // Returns a LineMarkerInfo if the element is the CLASS_KEYWORD of a @Screen class, null otherwise.
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Only process leaf PSI elements (individual tokens).
        val leaf = element as? LeafPsiElement ?: return null
        // Only process the "class" keyword token.
        if (leaf.elementType != KtTokens.CLASS_KEYWORD) return null
        // Navigate up to the class declaration that owns this keyword.
        val ktClass = leaf.parent as? KtClass ?: return null
        // Check if the class has the @Screen annotation.
        if (!hasScreenAnnotation(ktClass)) return null

        // Build the tooltip text showing the generated constants.
        val fqName = ktClass.fqName?.asString() ?: return null
        val tooltip = buildString {
            append("Screen generated constants:")
            append("\nKEY = \"$fqName\"")
        }

        // Create and return the line marker info with the plugin icon and tooltip.
        return LineMarkerInfo(
            // The anchor element for the marker (the leaf token).
            leaf as PsiElement,
            // The text range where the marker is placed (covers the "class" keyword).
            leaf.textRange,
            // The icon to display in the gutter (uses the standard Plugin icon).
            AllIcons.Nodes.Plugin,
            // Tooltip provider: returns the tooltip text on hover.
            { tooltip },
            // Navigation handler: null (no click action).
            null,
            // Alignment of the gutter icon (displayed on the right side).
            GutterIconRenderer.Alignment.RIGHT,
            // Accessible name for screen readers.
            { "@Screen" },
        )
    }

    // Checks whether a class or object has the @Screen annotation.
    // Uses the Kotlin Analysis API (analyze block) to inspect the symbol's annotations.
    private fun hasScreenAnnotation(classOrObject: KtClassOrObject): Boolean {
        analyze(classOrObject) {
            val symbol = classOrObject.symbol
            return symbol.annotations.any { it.classId == screenClassId }
        }
    }
}

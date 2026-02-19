// Package declaration for the ViewBinding IDEA plugin module.
package com.adkhambek.viewbinding.idea

// Import LineMarkerInfo for gutter icon/marker representation.
import com.intellij.codeInsight.daemon.LineMarkerInfo
// Import LineMarkerProvider, the extension point interface for gutter line markers.
import com.intellij.codeInsight.daemon.LineMarkerProvider
// Import AllIcons for built-in IntelliJ icon resources.
import com.intellij.icons.AllIcons
// Import GutterIconRenderer for gutter icon alignment.
import com.intellij.openapi.editor.markup.GutterIconRenderer
// Import PsiElement, the base class for PSI tree nodes.
import com.intellij.psi.PsiElement
// Import LeafPsiElement for leaf (terminal) PSI nodes.
import com.intellij.psi.impl.source.tree.LeafPsiElement
// Import analyze for Kotlin Analysis API operations.
import org.jetbrains.kotlin.analysis.api.analyze
// Import KtTokens for Kotlin lexer token type constants.
import org.jetbrains.kotlin.lexer.KtTokens
// Import ClassId for annotation class identification.
import org.jetbrains.kotlin.name.ClassId
// Import FqName for fully qualified names.
import org.jetbrains.kotlin.name.FqName
// Import Name for simple identifiers.
import org.jetbrains.kotlin.name.Name
// Import KtClass for class PSI declarations.
import org.jetbrains.kotlin.psi.KtClass
// Import KtClassOrObject, the base PSI class for class/object declarations.
import org.jetbrains.kotlin.psi.KtClassOrObject

// Line marker provider that displays a gutter icon next to @Screen-annotated classes
// to indicate that a ViewBinding property is generated for the class.
// The tooltip shows "ViewBinding generated property: binding".
//
// Registered in plugin.xml as a codeInsight.lineMarkerProvider for the Kotlin language.
class ViewBindingLineMarkerProvider : LineMarkerProvider {

    // ClassId for the @Screen annotation.
    private val screenClassId = ClassId(
        FqName("com.adkhambek.screen"),
        Name.identifier("Screen"),
    )

    // Called for each PSI element to determine if a gutter marker should be shown.
    // Returns a LineMarkerInfo for the CLASS_KEYWORD of @Screen-annotated classes.
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Only process leaf PSI elements (individual tokens).
        val leaf = element as? LeafPsiElement ?: return null
        // Only process the "class" keyword.
        if (leaf.elementType != KtTokens.CLASS_KEYWORD) return null
        // Navigate to the class declaration.
        val ktClass = leaf.parent as? KtClass ?: return null
        // Check for @Screen annotation.
        if (!hasScreenAnnotation(ktClass)) return null

        // Tooltip text indicating the generated property.
        val tooltip = "ViewBinding generated property: binding"

        // Create the gutter line marker.
        return LineMarkerInfo(
            leaf as PsiElement,
            leaf.textRange,
            // Plugin icon in the gutter.
            AllIcons.Nodes.Plugin,
            // Tooltip shown on hover.
            { tooltip },
            // No click navigation handler.
            null,
            // Align the icon to the right side of the gutter.
            GutterIconRenderer.Alignment.RIGHT,
            // Accessible name for screen readers.
            { "ViewBinding @Screen" },
        )
    }

    // Checks whether a class or object has the @Screen annotation using the Analysis API.
    private fun hasScreenAnnotation(classOrObject: KtClassOrObject): Boolean {
        analyze(classOrObject) {
            val symbol = classOrObject.symbol
            return symbol.annotations.any { it.classId == screenClassId }
        }
    }
}

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

    // Fast path: returns null so that no expensive work runs on every editor repaint.
    // All analysis is deferred to collectSlowLineMarkers() which runs in the background.
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    // Slow path: runs in a background thread. Filters elements with cheap PSI checks,
    // applies a text-based annotation pre-check, then uses analyze{} only for candidates.
    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        for (element in elements) {
            val leaf = element as? LeafPsiElement ?: continue
            if (leaf.elementType != KtTokens.CLASS_KEYWORD) continue
            val ktClass = leaf.parent as? KtClass ?: continue
            // Cheap text-based pre-check: skip classes that don't even mention @Screen.
            if (ktClass.annotationEntries.none { it.shortName?.asString() == "Screen" }) continue
            // Full K2 resolution to confirm the annotation identity.
            if (!hasScreenAnnotation(ktClass)) continue

            val tooltip = "ViewBinding generated property: binding"

            result.add(
                LineMarkerInfo(
                    leaf as PsiElement,
                    leaf.textRange,
                    AllIcons.Nodes.Plugin,
                    { tooltip },
                    null,
                    GutterIconRenderer.Alignment.RIGHT,
                    { "ViewBinding @Screen" },
                )
            )
        }
    }

    // Checks whether a class or object has the @Screen annotation using the Analysis API.
    private fun hasScreenAnnotation(classOrObject: KtClassOrObject): Boolean {
        analyze(classOrObject) {
            val symbol = classOrObject.symbol
            return symbol.annotations.any { it.classId == screenClassId }
        }
    }
}

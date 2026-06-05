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

            val fqName = ktClass.fqName?.asString() ?: continue
            val tooltip = buildString {
                append("Screen generated constants:")
                append("\nKEY = \"$fqName\"")
            }

            result.add(
                LineMarkerInfo(
                    leaf as PsiElement,
                    leaf.textRange,
                    AllIcons.Nodes.Plugin,
                    { tooltip },
                    null,
                    GutterIconRenderer.Alignment.RIGHT,
                    { "@Screen" },
                )
            )
        }
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

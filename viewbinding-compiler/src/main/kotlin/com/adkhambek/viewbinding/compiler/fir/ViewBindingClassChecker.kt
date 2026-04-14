// Package declaration for the FIR phase of the ViewBinding compiler plugin.
package com.adkhambek.viewbinding.compiler.fir

// Import DiagnosticReporter for reporting compilation diagnostics.
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
// Import reportOn for conveniently reporting diagnostics on source elements.
import org.jetbrains.kotlin.diagnostics.reportOn
// Import MppCheckerKind to specify checker scope (Common or Platform).
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
// Import CheckerContext providing session and context during checking.
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
// Import the base class for FIR class-level diagnostic checkers.
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
// Import FirClass representing a class declaration in the FIR tree.
import org.jetbrains.kotlin.fir.declarations.FirClass
// Import FirConstructor for accessing constructor declarations.
import org.jetbrains.kotlin.fir.declarations.FirConstructor
// Import DirectDeclarationsAccess opt-in for accessing direct declarations.
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
// Import FirResolvePhase for specifying minimum resolution phases.
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
// Import getAnnotationByClassId to look up annotations on symbols.
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
// Import FirExpression as the base type for FIR expressions.
import org.jetbrains.kotlin.fir.expressions.FirExpression
// Import FirPropertyAccessExpression for property access expressions (e.g., R.layout.name).
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
// Import FirResolvedQualifier for fully resolved qualified names.
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
// Import symbolProvider for class symbol lookups.
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
// Import FirRegularClassSymbol for regular class symbols.
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
// Import lazyResolveToPhase for triggering lazy resolution.
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
// Import classId extension for extracting ClassId from ConeKotlinType.
import org.jetbrains.kotlin.fir.types.classId
// Import ClassId for unique class identification.
import org.jetbrains.kotlin.name.ClassId
// Import FqName for fully qualified names.
import org.jetbrains.kotlin.name.FqName
// Import Name for simple identifiers.
import org.jetbrains.kotlin.name.Name

// ClassId for the @Screen annotation.
private val SCREEN_CLASS_ID = ClassId(
    FqName("com.adkhambek.screen"),
    Name.identifier("Screen"),
)

// ClassId for androidx.fragment.app.Fragment, used to verify Fragment inheritance.
private val FRAGMENT_CLASS_ID = ClassId(
    FqName("androidx.fragment.app"),
    Name.identifier("Fragment"),
)

// FIR class checker that validates @Screen annotation usage for the ViewBinding plugin.
// This checker ensures that @Screen-annotated classes extend Fragment or DialogFragment,
// which is required because the generated binding property uses Fragment.requireView().
// MppCheckerKind.Common means this checker runs for both common and platform-specific code.
class ViewBindingClassChecker : FirClassChecker(MppCheckerKind.Common) {

    // Main check function called for every class declaration in the compilation.
    // Uses Kotlin 2.x context parameters for CheckerContext and DiagnosticReporter.
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val session = context.session
        val symbol = declaration.symbol
        // Skip classes without the @Screen annotation.
        symbol.getAnnotationByClassId(SCREEN_CLASS_ID, session) ?: return

        // Check that the class extends Fragment by walking the supertype chain.
        val extendsFragment = symbol.resolvedSuperTypes.any { superType ->
            val superClassId = superType.classId ?: return@any false
            isFragmentSubclass(superClassId, context)
        }

        // Report an error if the class doesn't extend Fragment.
        if (!extendsFragment) {
            reporter.reportOn(declaration.source, ViewBindingErrors.VIEW_BINDING_NOT_ON_FRAGMENT)
        } else if (!hasLayoutInConstructor(declaration)) {
            // The class extends Fragment but doesn't pass a layout resource to the super
            // constructor. Without a layout, the plugin cannot determine which binding class
            // to generate, so the binding property would silently not be created.
            reporter.reportOn(declaration.source, ViewBindingErrors.VIEW_BINDING_MISSING_LAYOUT)
        }
    }

    // Checks whether the Fragment class passes a layout resource (R.layout.xxx) to its
    // super constructor. Returns false when the primary constructor has no delegated call,
    // no arguments, or the first argument is not an R.layout reference.
    @OptIn(DirectDeclarationsAccess::class)
    private fun hasLayoutInConstructor(declaration: FirClass): Boolean {
        val primaryConstructor = declaration.declarations
            .filterIsInstance<FirConstructor>()
            .firstOrNull { it.isPrimary } ?: return false
        val delegatedCall = primaryConstructor.delegatedConstructor ?: return false
        val firstArg = delegatedCall.argumentList.arguments.firstOrNull() ?: return false
        return isLayoutResourceExpression(firstArg)
    }

    // Checks whether an expression matches the R.layout.xxx pattern.
    // Handles two FIR resolution stages:
    //   - After resolution: receiver is a FirResolvedQualifier with classId ending in "layout"
    //   - Before resolution: receiver is a FirPropertyAccessExpression with callee name "layout"
    private fun isLayoutResourceExpression(expr: FirExpression): Boolean {
        if (expr !is FirPropertyAccessExpression) return false
        val receiver = expr.explicitReceiver ?: return false
        if (receiver is FirResolvedQualifier) {
            val classId = receiver.classId ?: return false
            return classId.shortClassName.asString() == "layout"
        }
        if (receiver is FirPropertyAccessExpression) {
            return receiver.calleeReference.name.asString() == "layout"
        }
        return false
    }

    // Recursively checks whether a class identified by classId is Fragment or a subclass of Fragment.
    // Handles deep inheritance chains (e.g., MyFragment -> BaseFragment -> Fragment).
    private fun isFragmentSubclass(classId: ClassId, context: CheckerContext): Boolean {
        // Base case: the classId is exactly Fragment.
        if (classId == FRAGMENT_CLASS_ID) return true
        // Look up the class symbol; return false if not found.
        val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
            as? FirRegularClassSymbol ?: return false
        // Ensure supertypes are resolved before checking.
        symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
        // Recursively check each supertype.
        return symbol.resolvedSuperTypes.any { superType ->
            val superClassId = superType.classId ?: return@any false
            isFragmentSubclass(superClassId, context)
        }
    }
}

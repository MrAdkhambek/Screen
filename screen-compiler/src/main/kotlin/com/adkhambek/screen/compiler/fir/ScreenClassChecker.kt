// Package declaration for the FIR phase of the Screen compiler plugin.
package com.adkhambek.screen.compiler.fir

import com.adkhambek.compiler.common.FRAGMENT_CLASS_ID
import com.adkhambek.compiler.common.SCREEN_CLASS_ID
import com.adkhambek.compiler.common.isFragmentSubclass
import com.adkhambek.compiler.common.isSubclassOf
import com.adkhambek.compiler.common.resolveArgClassIdFromAnnotation
// Import DiagnosticReporter used to report compilation errors and warnings.
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
// Import reportOn extension function for conveniently reporting diagnostics on source elements.
import org.jetbrains.kotlin.diagnostics.reportOn
// Import MppCheckerKind which specifies whether the checker runs for common or platform-specific code.
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
// Import CheckerContext which provides access to the session and other contextual information during checking.
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
// Import the base class for FIR class-level diagnostic checkers.
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
// Import FirClass which represents a class declaration in the FIR tree.
import org.jetbrains.kotlin.fir.declarations.FirClass
// Import getAnnotationByClassId to look up annotations on a symbol by their class ID.
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
// Import symbolProvider to look up class symbols by ClassId from the session.
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
// Import SymbolInternals opt-in required for accessing internal symbol properties.
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
// Import FirRegularClassSymbol which represents a regular (non-anonymous, non-local) class symbol.
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
// Import classId extension to extract the ClassId from a ConeKotlinType.
import org.jetbrains.kotlin.fir.types.classId
// Import ClassId which uniquely identifies a class by its package and relative name.
import org.jetbrains.kotlin.name.ClassId
// Import FqName which represents a fully qualified dotted name.
import org.jetbrains.kotlin.name.FqName
// Import Name which represents a simple (non-qualified) identifier name.
import org.jetbrains.kotlin.name.Name

// ClassId for android.os.Parcelable, used to verify the arg class implements Parcelable.
private val PARCELABLE_CLASS_ID = ClassId(
    FqName("android.os"),
    Name.identifier("Parcelable"),
)

// FIR class checker that validates @Screen annotation usage at compile time.
// This checker runs during the FIR analysis phase and reports errors for:
//   1. Classes annotated with @Screen that don't extend Fragment or DialogFragment
//   2. @Screen arg classes that don't implement android.os.Parcelable
// MppCheckerKind.Common means this checker runs for both common and platform-specific code.
class ScreenClassChecker : FirClassChecker(MppCheckerKind.Common) {

    // Opt into SymbolInternals to access FIR symbol internals like resolvedSuperTypes.
    @OptIn(SymbolInternals::class)
    // Main check function called for every class declaration in the compilation.
    // Uses Kotlin 2.x context parameters to receive CheckerContext and DiagnosticReporter.
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        // Get the current compilation session from the checker context.
        val session = context.session
        // Get the FIR symbol for this class declaration.
        val symbol = declaration.symbol
        // Look up the @Screen annotation on this class. If absent, skip this class entirely.
        val annotation = symbol.getAnnotationByClassId(SCREEN_CLASS_ID, session) ?: return

        // Check that the class extends Fragment by walking its resolved supertype chain.
        // resolvedSuperTypes includes all direct supertypes (superclass + interfaces).
        val extendsFragment = symbol.resolvedSuperTypes.any { superType ->
            val superClassId = superType.classId ?: return@any false
            // Recursively check if this supertype is Fragment or inherits from Fragment.
            isFragmentSubclass(superClassId, context)
        }

        // Report an error if the class doesn't extend Fragment.
        if (!extendsFragment) {
            reporter.reportOn(declaration.source, ScreenErrors.SCREEN_NOT_ON_FRAGMENT)
        }

        // Check that the arg class implements Parcelable (if specified).
        // resolveArgClassIdFromAnnotation extracts the ClassId from the @Screen(arg = ...) parameter.
        val argClassId = resolveArgClassIdFromAnnotation(annotation, symbol.classId, context)
        if (argClassId != null) {
            // Look up the arg class symbol in the compilation session.
            val argSymbol = session.symbolProvider.getClassLikeSymbolByClassId(argClassId)
                as? FirRegularClassSymbol
            if (argSymbol != null) {
                // Recursively check if the arg class implements Parcelable anywhere in its hierarchy.
                if (!isSubclassOf(argClassId, PARCELABLE_CLASS_ID, context)) {
                    reporter.reportOn(declaration.source, ScreenErrors.SCREEN_ARG_NOT_PARCELABLE)
                }
            }
        }
    }

    // Resolves the ClassId of the arg parameter from the @Screen annotation expression.
    // Delegates to the shared resolveArgClassIdFromAnnotation with the CheckerContext's session.
    @OptIn(SymbolInternals::class)
    private fun resolveArgClassIdFromAnnotation(
        annotation: org.jetbrains.kotlin.fir.expressions.FirAnnotation,
        ownerClassId: ClassId,
        context: CheckerContext,
    ): ClassId? {
        return resolveArgClassIdFromAnnotation(annotation, ownerClassId.packageFqName, context.session)
    }
}

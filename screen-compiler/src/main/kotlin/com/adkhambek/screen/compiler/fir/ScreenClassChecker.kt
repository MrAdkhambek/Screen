// Package declaration for the FIR phase of the Screen compiler plugin.
package com.adkhambek.screen.compiler.fir

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
// Import FirResolvePhase used to specify the minimum resolution phase required before accessing data.
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
// Import findArgumentByName to extract named arguments from annotation expressions.
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
// Import getAnnotationByClassId to look up annotations on a symbol by their class ID.
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
// Import FirAnnotation which represents an annotation in the FIR tree.
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
// Import FirClassReferenceExpression for handling resolved class references in annotations.
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
// Import FirGetClassCall which represents a ::class expression in the FIR tree.
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
// Import FirPropertyAccessExpression for handling unresolved property accesses in annotations.
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
// Import FirResolvedQualifier for handling fully resolved qualified names in annotations.
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
// Import symbolProvider to look up class symbols by ClassId from the session.
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
// Import SymbolInternals opt-in required for accessing internal symbol properties.
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
// Import FirRegularClassSymbol which represents a regular (non-anonymous, non-local) class symbol.
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
// Import lazyResolveToPhase to trigger lazy resolution of a symbol to a specific FIR phase.
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
// Import classId extension to extract the ClassId from a ConeKotlinType.
import org.jetbrains.kotlin.fir.types.classId
// Import coneTypeOrNull to safely extract the ConeKotlinType from a type reference.
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
// Import ClassId which uniquely identifies a class by its package and relative name.
import org.jetbrains.kotlin.name.ClassId
// Import FqName which represents a fully qualified dotted name.
import org.jetbrains.kotlin.name.FqName
// Import Name which represents a simple (non-qualified) identifier name.
import org.jetbrains.kotlin.name.Name
// Import StandardClassIds which provides well-known ClassIds for standard library types.
import org.jetbrains.kotlin.name.StandardClassIds

// ClassId for the @Screen annotation, used to look up the annotation on class symbols.
private val SCREEN_CLASS_ID = ClassId(
    FqName("com.adkhambek.screen"),
    Name.identifier("Screen"),
)

// ClassId for androidx.fragment.app.Fragment, used to verify Fragment inheritance.
private val FRAGMENT_CLASS_ID = ClassId(
    FqName("androidx.fragment.app"),
    Name.identifier("Fragment"),
)

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
        // resolveArgClassId extracts the ClassId from the @Screen(arg = ...) parameter.
        val argClassId = resolveArgClassId(annotation, symbol.classId, context)
        if (argClassId != null) {
            // Look up the arg class symbol in the compilation session.
            val argSymbol = session.symbolProvider.getClassLikeSymbolByClassId(argClassId)
                as? FirRegularClassSymbol
            if (argSymbol != null) {
                // Ensure the arg class's supertypes are resolved before checking them.
                argSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
                // Check if any of the arg class's supertypes is Parcelable.
                val implementsParcelable = argSymbol.resolvedSuperTypes.any { superType ->
                    superType.classId == PARCELABLE_CLASS_ID
                }
                // Report an error if the arg class doesn't implement Parcelable.
                if (!implementsParcelable) {
                    reporter.reportOn(declaration.source, ScreenErrors.SCREEN_ARG_NOT_PARCELABLE)
                }
            }
        }
    }

    // Recursively checks whether a class identified by classId is Fragment or a subclass of Fragment.
    // This handles deep inheritance chains (e.g., MyFragment -> BaseFragment -> Fragment).
    private fun isFragmentSubclass(classId: ClassId, context: CheckerContext): Boolean {
        // Base case: the classId is exactly Fragment.
        if (classId == FRAGMENT_CLASS_ID) return true
        // Look up the class symbol; return false if not found (e.g., external unresolved class).
        val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
            as? FirRegularClassSymbol ?: return false
        // Ensure supertypes are resolved before accessing them.
        symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
        // Recursively check each supertype.
        return symbol.resolvedSuperTypes.any { superType ->
            val superClassId = superType.classId ?: return@any false
            isFragmentSubclass(superClassId, context)
        }
    }

    // Resolves the ClassId of the arg parameter from the @Screen annotation expression.
    // The arg value can appear in different FIR expression forms depending on the resolution phase:
    //   - FirResolvedQualifier: fully resolved reference (e.g., after type resolution)
    //   - FirClassReferenceExpression: resolved class reference
    //   - FirPropertyAccessExpression: unresolved simple name (before resolution)
    // Returns null if arg is not specified, is Unit::class, or cannot be resolved.
    @OptIn(SymbolInternals::class)
    private fun resolveArgClassId(
        annotation: FirAnnotation,
        ownerClassId: ClassId,
        context: CheckerContext,
    ): ClassId? {
        val session = context.session
        // Find the "arg" named argument in the annotation. returnFirstWhenNotFound=false
        // means return null if the argument is not explicitly specified.
        val argExpr = annotation.findArgumentByName(Name.identifier("arg"), returnFirstWhenNotFound = false)
            ?: return null
        // The arg expression should be a ::class call (e.g., MyArg::class).
        val getClassCall = argExpr as? FirGetClassCall ?: return null
        // Get the inner argument of the ::class call.
        val argument = getClassCall.argument

        // Extract the ClassId based on the expression type.
        val classId = when (argument) {
            // After full resolution: the argument is a resolved qualifier with a known classId.
            is FirResolvedQualifier -> argument.classId
            // Resolved class reference: extract classId from the type reference.
            is FirClassReferenceExpression -> argument.classTypeRef.coneTypeOrNull?.classId
            // Before resolution: the argument is a simple property access expression.
            // We attempt to resolve it by constructing a candidate ClassId in the same package.
            is FirPropertyAccessExpression -> {
                val simpleName = argument.calleeReference.name
                val candidateClassId = ClassId(ownerClassId.packageFqName, simpleName)
                // Verify the candidate class actually exists.
                if (session.symbolProvider.getClassLikeSymbolByClassId(candidateClassId) != null) {
                    candidateClassId
                } else null
            }
            else -> null
        }

        // Return null if the resolved class is Unit (meaning no arg was specified).
        return if (classId == StandardClassIds.Unit) null else classId
    }
}

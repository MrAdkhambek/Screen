// Package declaration for the FIR phase of the ViewBinding compiler plugin.
package com.adkhambek.viewbinding.compiler.fir

import com.adkhambek.compiler.common.SCREEN_CLASS_ID
import com.adkhambek.compiler.common.isFragmentSubclass
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
// Import getAnnotationByClassId to look up annotations on symbols.
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
// Import classId extension for extracting ClassId from ConeKotlinType.
import org.jetbrains.kotlin.fir.types.classId

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
        }
    }
}

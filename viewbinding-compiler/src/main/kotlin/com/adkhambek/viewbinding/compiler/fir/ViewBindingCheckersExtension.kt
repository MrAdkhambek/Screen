// Package declaration for the FIR phase of the ViewBinding compiler plugin.
package com.adkhambek.viewbinding.compiler.fir

// Import FirSession which provides access to the current compilation session's services.
import org.jetbrains.kotlin.fir.FirSession
// Import DeclarationCheckers which is the container for all checker types.
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
// Import the base class for registering additional diagnostic checkers.
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

// Extension that registers diagnostic checkers for the ViewBinding compiler plugin.
// Currently provides a class checker that validates @Screen is applied to Fragment subclasses.
// This is registered by ViewBindingFirExtensionRegistrar.
class ViewBindingCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    // Override declaration checkers to provide the ViewBinding class checker.
    override val declarationCheckers = object : DeclarationCheckers() {
        // Register the ViewBindingClassChecker which validates that @Screen-annotated
        // classes extend Fragment or DialogFragment.
        override val classCheckers = setOf(ViewBindingClassChecker())
    }
}

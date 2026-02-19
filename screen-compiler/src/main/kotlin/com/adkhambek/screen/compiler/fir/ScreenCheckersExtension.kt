// Package declaration for the FIR phase of the Screen compiler plugin.
package com.adkhambek.screen.compiler.fir

// Import FirSession which provides access to the current compilation session's services.
import org.jetbrains.kotlin.fir.FirSession
// Import DeclarationCheckers which is the container for all checker types (class, function, property, etc.).
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
// Import the base class for registering additional diagnostic checkers from a compiler plugin.
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

// Extension that registers diagnostic checkers for the Screen compiler plugin.
// Checkers validate that @Screen annotations are used correctly at compile time.
// This extension is registered by ScreenFirExtensionRegistrar and runs during
// the FIR analysis phase, before IR generation begins.
class ScreenCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    // Override the declaration checkers to provide our custom class checker.
    // DeclarationCheckers supports multiple checker types (class, function, property, etc.);
    // we only need a class checker since @Screen is applied to classes.
    override val declarationCheckers = object : DeclarationCheckers() {
        // Register ScreenClassChecker which validates:
        //   1. The @Screen-annotated class extends Fragment or DialogFragment
        //   2. The @Screen arg class implements android.os.Parcelable
        override val classCheckers = setOf(ScreenClassChecker())
    }
}

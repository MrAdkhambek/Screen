// Package declaration for the FIR phase of the ViewBinding compiler plugin.
package com.adkhambek.viewbinding.compiler.fir

import com.adkhambek.compiler.common.SCREEN_ANNOTATION_FQ_NAME
// Import DeclarationPredicate which defines a condition for matching FIR declarations.
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate

// Predicate that matches classes annotated with @com.adkhambek.screen.Screen.
// The ViewBinding plugin reuses the same @Screen annotation as the Screen plugin.
// This predicate is registered in the FirDeclarationPredicateRegistrar and used
// by ViewBindingFirDeclarationGenerationExtension to determine which classes should
// receive the generated `binding` property.
val viewBindingPredicate = DeclarationPredicate.create {
    // Match any declaration annotated with @Screen.
    // Note: Both the Screen and ViewBinding plugins use the same annotation.
    annotated(SCREEN_ANNOTATION_FQ_NAME)
}

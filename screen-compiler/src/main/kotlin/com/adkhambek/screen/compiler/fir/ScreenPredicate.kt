// Package declaration for the FIR phase of the Screen compiler plugin.
package com.adkhambek.screen.compiler.fir

// Import DeclarationPredicate which defines a condition for matching FIR declarations.
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
// Import FqName which represents a fully qualified name (e.g., "com.adkhambek.screen.Screen").
import org.jetbrains.kotlin.name.FqName

// Predicate that matches classes annotated with @com.adkhambek.screen.Screen.
// This predicate is registered in the FirDeclarationPredicateRegistrar and used
// by the FIR declaration generation extension to determine which classes should
// trigger code generation. The predicate-based provider caches matches for efficiency,
// so hasScreenAnnotation() checks are fast O(1) lookups after initial resolution.
val screenPredicate = DeclarationPredicate.create {
    // Match any declaration annotated with the fully qualified @Screen annotation.
    annotated(FqName("com.adkhambek.screen.Screen"))
}

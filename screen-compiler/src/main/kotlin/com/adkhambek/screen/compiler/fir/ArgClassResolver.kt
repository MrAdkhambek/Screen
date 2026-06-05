package com.adkhambek.screen.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Resolves a simple (unqualified) class name to a [ClassId] by searching:
 * 1. The same package as the owner class
 * 2. Nested classes of the owner class
 * 3. Explicit and star imports in the owner's source file
 *
 * Returns null if the class cannot be found.
 */
internal fun resolveClassBySimpleName(
    simpleName: Name,
    ownerClassId: ClassId,
    session: FirSession,
): ClassId? {
    val symbolProvider = session.symbolProvider

    // 1. Same package as the owner class.
    val samePackageId = ClassId(ownerClassId.packageFqName, simpleName)
    if (symbolProvider.getClassLikeSymbolByClassId(samePackageId) != null) {
        return samePackageId
    }

    // 2. Nested class of the owner.
    val nestedId = ownerClassId.createNestedClassId(simpleName)
    if (symbolProvider.getClassLikeSymbolByClassId(nestedId) != null) {
        return nestedId
    }

    // 3. Walk the file's import directives.
    val file = session.firProvider.getFirClassifierContainerFileIfAny(ownerClassId) ?: return null
    for (import in file.imports) {
        val importedFqName = import.importedFqName ?: continue
        if (import.isAllUnder) {
            // Star import (e.g., import com.other.pkg.*): try as top-level class in that package.
            val candidateId = ClassId(importedFqName, simpleName)
            if (symbolProvider.getClassLikeSymbolByClassId(candidateId) != null) {
                return candidateId
            }
        } else if (importedFqName.shortName() == simpleName || import.aliasName == simpleName) {
            // Explicit import matching the simple name: resolve via progressive FQN splitting.
            val resolved = resolveClassFromFqName(importedFqName, session)
            if (resolved != null) return resolved
        }
    }

    return null
}

/**
 * Tries every possible (package, relativeClassName) split of [fqName] to find a valid [ClassId].
 * This handles both top-level and nested classes.
 *
 * For example, given `com.foo.Outer.Inner`, tries:
 *   - ClassId("com.foo.Outer", "Inner")
 *   - ClassId("com.foo", "Outer.Inner")
 *   - ClassId("com", "foo.Outer.Inner")
 */
private fun resolveClassFromFqName(fqName: FqName, session: FirSession): ClassId? {
    val symbolProvider = session.symbolProvider
    var packagePart = fqName.parent()
    var classPart = FqName(fqName.shortName().asString())

    while (!packagePart.isRoot) {
        val candidateId = ClassId(packagePart, classPart, false)
        if (symbolProvider.getClassLikeSymbolByClassId(candidateId) != null) {
            return candidateId
        }
        classPart = FqName("${packagePart.shortName().asString()}.${classPart.asString()}")
        packagePart = packagePart.parent()
    }

    // Try with root package.
    val rootCandidate = ClassId(FqName.ROOT, classPart, false)
    if (symbolProvider.getClassLikeSymbolByClassId(rootCandidate) != null) {
        return rootCandidate
    }

    return null
}

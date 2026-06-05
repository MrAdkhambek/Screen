package com.adkhambek.compiler.common

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId

@OptIn(SymbolInternals::class)
fun isSubclassOf(classId: ClassId, targetId: ClassId, context: CheckerContext): Boolean {
    if (classId == targetId) return true
    val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
        as? FirRegularClassSymbol ?: return false
    symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
    return symbol.resolvedSuperTypes.any { superType ->
        val superClassId = superType.classId ?: return@any false
        isSubclassOf(superClassId, targetId, context)
    }
}

fun isFragmentSubclass(classId: ClassId, context: CheckerContext): Boolean =
    isSubclassOf(classId, FRAGMENT_CLASS_ID, context)

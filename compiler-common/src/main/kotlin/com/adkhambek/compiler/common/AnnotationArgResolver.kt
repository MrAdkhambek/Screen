package com.adkhambek.compiler.common

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

data class ScreenArgs(
    val argClassId: ClassId?,
    val isNullable: Boolean,
)

fun resolveArgClassIdFromAnnotation(
    annotation: FirAnnotation,
    ownerPackageFqName: FqName,
    session: FirSession,
): ClassId? {
    val argExpr = annotation.findArgumentByName(Name.identifier("arg"), returnFirstWhenNotFound = false)
        ?: return null
    val getClassCall = argExpr as? FirGetClassCall ?: return null
    val argument = getClassCall.argument

    val classId = when (argument) {
        is FirResolvedQualifier -> argument.classId
        is FirClassReferenceExpression -> argument.classTypeRef.coneTypeOrNull?.classId
        is FirPropertyAccessExpression -> {
            val simpleName = argument.calleeReference.name
            val candidateClassId = ClassId(ownerPackageFqName, simpleName)
            if (session.symbolProvider.getClassLikeSymbolByClassId(candidateClassId) != null) {
                candidateClassId
            } else null
        }
        else -> null
    }

    return if (classId == StandardClassIds.Unit) null else classId
}

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.readScreenArgs(session: FirSession): ScreenArgs {
    lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
    val annotation = this.getAnnotationByClassId(SCREEN_CLASS_ID, session)
        ?: return ScreenArgs(null, false)

    val argClassId = resolveArgClassIdFromAnnotation(annotation, this.classId.packageFqName, session)

    var isNullableArg = false
    val isNullableExpr = annotation.findArgumentByName(Name.identifier("isNullable"), returnFirstWhenNotFound = false)
    if (isNullableExpr is FirLiteralExpression) {
        isNullableArg = isNullableExpr.value as? Boolean ?: false
    }

    return ScreenArgs(argClassId, isNullableArg)
}

package com.adkhambek.compiler.common

import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter

fun regularParameterCount(params: List<IrValueParameter>): Int =
    params.count { it.kind == IrParameterKind.Regular }

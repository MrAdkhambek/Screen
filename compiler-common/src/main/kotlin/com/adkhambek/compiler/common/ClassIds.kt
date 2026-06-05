package com.adkhambek.compiler.common

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val SCREEN_CLASS_ID = ClassId(
    FqName("com.adkhambek.screen"),
    Name.identifier("Screen"),
)

val FRAGMENT_CLASS_ID = ClassId(
    FqName("androidx.fragment.app"),
    Name.identifier("Fragment"),
)

val JAVA_CLASS_ID = ClassId(
    FqName("java.lang"),
    Name.identifier("Class"),
)

val SCREEN_ANNOTATION_FQ_NAME = FqName("com.adkhambek.screen.Screen")

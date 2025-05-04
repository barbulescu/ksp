package com.example.processor

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

fun KSPropertyDeclaration.isMasked(): Boolean = annotations.any { it.isMask() }

private fun KSAnnotation.isMask(): Boolean =
    annotationType.resolve().declaration.qualifiedName?.asString() == "com.example.app.Mask"

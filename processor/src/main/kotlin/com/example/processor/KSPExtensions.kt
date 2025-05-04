package com.example.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference

fun KSPropertyDeclaration.isMasked(): Boolean = annotations.any { it.isMask() }

fun KSAnnotation.isMask(): Boolean = annotationType.fullName() == "com.example.app.Mask"

fun KSClassDeclaration.isModel(): Boolean = superTypes.any { it.fullName() == "com.example.app.Model" }

private fun KSTypeReference.fullName(): String? = resolve().declaration.qualifiedName?.asString()

fun Resolver.models() = this.getAllFiles()
    .flatMap { it.declarations }
    .filterIsInstance<KSClassDeclaration>()
    .filter { it.isModel() }

package com.example.processor

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

data class MaskedProperty(
    val propertyPath: String,
    val property: KSPropertyDeclaration,
)

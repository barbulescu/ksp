package com.example.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class FunctionGenerator(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.models().forEach { processClass(it) }

        return emptyList()
    }

    private fun processClass(declaration: KSClassDeclaration): Boolean {
        val properties = declaration.getAllProperties()
            .filter { processProperty(it) }
            .toList()

        return if (properties.isNotEmpty()) {
            declaration.toExtensionFile(properties)
            true
        } else {
            false
        }
    }

    private fun processProperty(property: KSPropertyDeclaration): Boolean = if (property.isMasked()) {
        true
    } else {
        property.type.resolve().declaration.let { processClass(it as KSClassDeclaration) }
    }

    private fun KSClassDeclaration.toExtensionFile(maskedProperties: List<KSPropertyDeclaration>) {
        val className = simpleName.asString()
        val packageName = packageName.asString()
        val fileName = "${className}Mask"

        try {
            val file = codeGenerator.createNewFile(
                dependencies = Dependencies(false, containingFile!!),
                packageName = packageName,
                fileName = fileName
            )

            val propertiesBlock = maskedProperties
                .map { it.simpleName.asString() }
                .joinToString(separator = ",\n") { "   $it = $it.mask()" }

            val code = """
                |package $packageName
                |
                |fun ${className}.mask(): $className = this.copy(
                |$propertiesBlock
                |)
            """.trimMargin()

            file.use { outputStream ->
                outputStream.writer().use { writer ->
                    writer.write(code)
                }
            }
        } catch (e: FileAlreadyExistsException) {
            logger.info("${e.message} already exists, skipping generation.")
        }
    }

}

class MaskingGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        FunctionGenerator(environment.codeGenerator, environment.logger)
}

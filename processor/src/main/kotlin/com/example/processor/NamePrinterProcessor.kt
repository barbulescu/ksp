package com.example.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import java.io.OutputStream

class NamePrinterProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Find SampleClass
        val sampleClass = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString("com.example.app.SampleClass")
        )

        if (sampleClass != null) {
            processSampleClass(sampleClass)
        } else {
            logger.error("SampleClass not found")
        }

        return emptyList()
    }

    private fun processSampleClass(classDeclaration: KSClassDeclaration) {
        logger.info("Processing SampleClass for name printing method")

        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()

        // Find all properties annotated with @Include
        val includedProperties = classDeclaration.getAllProperties()
            .filter { property ->
                property.annotations.any { annotation ->
                    annotation.shortName.asString() == "Include" &&
                    annotation.annotationType.resolve().declaration.qualifiedName?.asString() == "com.example.annotations.Include"
                }
            }
            .toList()

        logger.info("Found ${includedProperties.size} properties with @Include annotation")

        val fileName = "${className}NamePrinter"
        val fileSpec = Dependencies(aggregating = false, classDeclaration.containingFile!!)

        try {
            codeGenerator.createNewFile(fileSpec, packageName, fileName)
                .use { outputStream -> outputStream.generateFile(packageName, className, includedProperties) }
            logger.info("Generated printName method for SampleClass")
        } catch (_: FileAlreadyExistsException) {
            logger.info("File already exists, skipping generation")
        }
    }

    private fun OutputStream.generateFile(
        packageName: String,
        className: String,
        includedProperties: List<KSPropertyDeclaration>
    ) {
        writer().use { writer ->
            val printStatements = StringBuilder()

            // Print all properties annotated with @Include
            includedProperties.forEach { property ->
                val propertyName = property.simpleName.asString()
                printStatements.append("    println(\"${propertyName}: \" + this.${propertyName})\n")
            }

            writer.write(
                """
                package $packageName

                /**
                 * Generated printName method for $className
                 */
                fun ${className}.printName() {
${printStatements}}
                """.trimIndent()
            )
        }
    }
}

class NamePrinterProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return NamePrinterProcessor(environment.codeGenerator, environment.logger)
    }
}

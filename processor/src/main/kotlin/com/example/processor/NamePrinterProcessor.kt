package com.example.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
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

        val fileName = "${className}NamePrinter"
        val fileSpec = Dependencies(aggregating = false, classDeclaration.containingFile!!)

        try {
            codeGenerator.createNewFile(fileSpec, packageName, fileName)
                .use { outputStream -> outputStream.generateFile(packageName, className) }
            logger.info("Generated printName method for SampleClass")
        } catch (_: FileAlreadyExistsException) {
            logger.info("File already exists, skipping generation")
        }
    }

    private fun OutputStream.generateFile(
        packageName: String,
        className: String
    ) {
        writer().use { writer ->
            writer.write(
                """
                package $packageName

                /**
                 * Generated printName method for $className
                 */
                fun ${className}.printName() {
                    // Access the name property directly
                    println("Name property: " + this.name)
                }
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

package com.example.processor

import com.example.annotations.GenerateFunction
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

class FunctionGenerator(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Find all classes annotated with @GenerateFunction
        val symbols = resolver.getSymbolsWithAnnotation(GenerateFunction::class.qualifiedName!!)
        val unprocessed = symbols.filter { !it.validate() }.toList()

        symbols
            .filter { it is KSClassDeclaration }
            .filter { it.validate() }
            .filterIsInstance(KSClassDeclaration::class.java)
            .forEach { processAnnotatedClass(it) }

        return unprocessed
    }

    private fun processAnnotatedClass(classDeclaration: KSClassDeclaration) {
        // Get the annotation and its parameters
        val annotation = classDeclaration.annotations
            .find { it.shortName.asString() == "GenerateFunction" }
            ?: return

        val functionName = annotation.arguments
            .find { it.name?.asString() == "name" }?.value as? String
            ?: run {
                logger.error("Function name must be provided", classDeclaration)
                return
            }

        val returnType = annotation.arguments.find { it.name?.asString() == "returnType" }?.value as? String ?: "String"

        // Get package name for the class
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()

        // Create a new file for the generated function
        val fileName = "${className}Generated"
        val fileSpec = Dependencies(aggregating = false, classDeclaration.containingFile!!)

        codeGenerator.createNewFile(
            fileSpec,
            packageName,
            fileName
        ).use { outputStream ->
            outputStream.writer().use { writer ->
                writer.write(
                    """
                    package $packageName
                    
                    /**
                     * Generated function for ${className}
                     */
                    fun ${className}.$functionName(): $returnType {
                        return "Hello from generated function!"
                    }
                    """.trimIndent()
                )
            }
        }

        logger.info("Generated function '$functionName' for class '$className'", classDeclaration)
    }
}

class FunctionGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FunctionGenerator(environment.codeGenerator, environment.logger)
    }
}

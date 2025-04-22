package com.example.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import java.io.OutputStream

class FunctionGenerator(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("com.example.annotations.GenerateFunction")

        symbols
            .filter { it is KSClassDeclaration }
            .filter { it.validate() }
            .filterIsInstance(KSClassDeclaration::class.java)
            .forEach { processAnnotatedClass(it) }

        return symbols
            .filterNot { it.validate() }
            .toList()
    }

    private fun processAnnotatedClass(classDeclaration: KSClassDeclaration) {
        logger.info("symbol $classDeclaration")

        val annotation = classDeclaration.annotations
            .find { it.shortName.asString() == "GenerateFunction" }
            ?: return

        logger.info("annotation: $annotation", classDeclaration)

        val functionName = annotation.arguments
            .find { it.name?.asString() == "name" }?.value as? String
            ?: run {
                logger.error("Function name must be provided", classDeclaration)
                return
            }

        logger.info("function: $functionName")

        val returnType = annotation.arguments
            .find { it.name?.asString() == "returnType" }
            ?.value as? String
            ?: "String"
        logger.info("returnType: $returnType")

        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        classDeclaration

        val fileName = "${className}Generated"
        val fileSpec = Dependencies(aggregating = false, classDeclaration.containingFile!!)

        codeGenerator.createNewFile(fileSpec, packageName, fileName)
            .use { outputStream -> outputStream.generateFile(packageName, className, functionName, returnType) }

        logger.info("Generated function '$functionName' for class '$className'", classDeclaration)
    }

    private fun OutputStream.generateFile(
        packageName: String,
        className: String,
        functionName: String,
        returnType: String,
    ) {
        writer().use { writer ->
            writer.write(
                """
                        package $packageName
                        
                        /**
                         * Generated function for $className
                         */
                        fun ${className}.$functionName(): $returnType {
                            return "Hello from generated function!"
                        }
                        """.trimIndent()
            )
        }
    }
}

class FunctionGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FunctionGenerator(environment.codeGenerator, environment.logger)
    }
}

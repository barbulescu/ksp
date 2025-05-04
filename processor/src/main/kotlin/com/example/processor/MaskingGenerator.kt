package com.example.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import java.io.OutputStream

class FunctionGenerator(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val modelInterface = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString("com.example.app.Model")
        ) ?: return emptyList()

        val modelClasses = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { classDeclaration -> classDeclaration.implements(modelInterface) }

        modelClasses.forEach { modelClass ->
            logger.info("Found class implementing Model: ${modelClass.simpleName.asString()}")
        }

        return emptyList()
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

private fun KSClassDeclaration.implements(targetInterface: KSClassDeclaration): Boolean = superTypes.any {
    it.resolve().declaration == targetInterface
}


class MaskingGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        FunctionGenerator(environment.codeGenerator, environment.logger)
}

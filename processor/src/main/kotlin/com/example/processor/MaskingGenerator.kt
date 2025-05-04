package com.example.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class FunctionGenerator(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        cleanGeneratedFiles()

        val modelInterface = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString("com.example.app.Model")
        ) ?: return emptyList()

        val modelClasses = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { classDeclaration -> classDeclaration.implements(modelInterface) }
            .distinctBy { it.simpleName.asString() }

        modelClasses.forEach { modelClass ->
            logger.info("Found class implementing Model 11: ${modelClass.simpleName.asString()}")

            // Collect all properties with @Mask annotation
            val maskedProperties = mutableListOf<MaskedProperty>()
            collectMaskedProperties(modelClass, resolver, "", maskedProperties)

            // Generate mask function for this model class
            if (maskedProperties.isNotEmpty()) {
                generateMaskFunction(modelClass, maskedProperties)
            }
        }

        return emptyList()
    }

    private data class MaskedProperty(
        val propertyPath: String,
        val property: KSPropertyDeclaration,
    )

    private fun collectMaskedProperties(
        classDeclaration: KSClassDeclaration,
        resolver: Resolver,
        propertyPath: String,
        maskedProperties: MutableList<MaskedProperty>,
    ) {
        classDeclaration.getAllProperties().forEach { property ->
            // Check if property has Mask annotation
            val hasMaskAnnotation = property.annotations.any {
                it.shortName.asString() == "Mask"
            }

            val currentPath = if (propertyPath.isEmpty()) property.simpleName.asString()
            else "$propertyPath.${property.simpleName.asString()}"

            if (hasMaskAnnotation) {
                logger.info("Found property with @Mask annotation: $currentPath")
                maskedProperties.add(MaskedProperty(currentPath, property))
            }

            // Get property type
            val propertyType = property.type.resolve()
            val propertyTypeDeclaration = propertyType.declaration

            // If property type is a class (not a primitive), process it recursively
            if (propertyTypeDeclaration is KSClassDeclaration &&
                propertyTypeDeclaration.qualifiedName?.asString()?.startsWith("kotlin.") != true
            ) {
                collectMaskedProperties(
                    propertyTypeDeclaration,
                    resolver,
                    currentPath,
                    maskedProperties
                )
            }
        }
    }

    private fun generateMaskFunction(
        classDeclaration: KSClassDeclaration,
        maskedProperties: List<MaskedProperty>,
    ) {
        val className = classDeclaration.simpleName.asString()
        logger.info("Generating mask function for $className")
        val packageName = classDeclaration.packageName.asString()
        val fileName = "${className}Mask"

        try {
            val file = codeGenerator.createNewFile(
                dependencies = Dependencies(false, classDeclaration.containingFile!!),
                packageName = packageName,
                fileName = fileName
            )

            file.use { outputStream ->
                outputStream.writer().use { writer ->
                    writer.write("package $packageName\n\n")

                    // Write the mask function
                    writer.write("fun ${className}.mask(): ${className} {\n")
                    writer.write("    return this.copy(\n")

                    // Get direct properties of the class
                    val directProperties = classDeclaration.getAllProperties().toList()

                    // For each direct property, check if it or any of its nested properties need masking
                    directProperties.forEachIndexed { index, property ->
                        val propertyName = property.simpleName.asString()
                        val propertyType = property.type.resolve()
                        val propertyTypeDeclaration = propertyType.declaration

                        // Check if this property itself has @Mask annotation
                        val hasMaskAnnotation = property.annotations.any { it.shortName.asString() == "Mask" }

                        if (hasMaskAnnotation) {
                            // If property has @Mask annotation, set it to empty string
                            writer.write("        $propertyName = \"\",\n")
                        } else if (propertyTypeDeclaration is KSClassDeclaration &&
                            propertyTypeDeclaration.qualifiedName?.asString()?.startsWith("kotlin.") != true
                        ) {
                            // Check if any nested properties of this property need masking
                            val nestedMaskedProperties = maskedProperties.filter {
                                it.propertyPath.startsWith("$propertyName.")
                            }

                            if (nestedMaskedProperties.isNotEmpty()) {
                                // Generate nested masking code
                                generateNestedMaskingCode(
                                    writer,
                                    propertyName,
                                    propertyTypeDeclaration,
                                    nestedMaskedProperties
                                )
                            } else {
                                // If no nested properties need masking, keep the original value
                                writer.write("        $propertyName = $propertyName")
                                if (index < directProperties.size - 1) {
                                    writer.write(",")
                                }
                                writer.write("\n")
                            }
                        } else {
                            // For primitive properties without @Mask, keep the original value
                            writer.write("        $propertyName = $propertyName")
                            if (index < directProperties.size - 1) {
                                writer.write(",")
                            }
                            writer.write("\n")
                        }
                    }

                    writer.write("    )\n")
                    writer.write("}\n")
                }
            }
        } catch (e: FileAlreadyExistsException) {
            logger.info("${e.message} already exists, skipping generation.")
        }
    }

    private fun generateNestedMaskingCode(
        writer: java.io.Writer,
        propertyName: String,
        propertyTypeDeclaration: KSClassDeclaration,
        nestedMaskedProperties: List<MaskedProperty>,
    ) {
        // Create a deep copy of the nested object with masked properties
        writer.write("        $propertyName = $propertyName.copy(\n")

        // Get all direct properties of the nested class
        val nestedProperties = propertyTypeDeclaration.getAllProperties().toList()

        nestedProperties.forEachIndexed { index, nestedProperty ->
            val nestedPropertyName = nestedProperty.simpleName.asString()
            val nestedPropertyPath = "$propertyName.$nestedPropertyName"

            // Check if this nested property has @Mask annotation
            val isMasked = nestedMaskedProperties.any {
                it.propertyPath == nestedPropertyPath
            }

            if (isMasked) {
                // If nested property has @Mask annotation, set it to empty string
                writer.write("            $nestedPropertyName = \"\"")
            } else {
                // Check if this nested property has further nested properties that need masking
                val furtherNestedMaskedProperties = nestedMaskedProperties.filter {
                    it.propertyPath.startsWith("$nestedPropertyPath.")
                }

                if (furtherNestedMaskedProperties.isNotEmpty()) {
                    // If there are further nested properties to mask, we need to handle them recursively
                    // For simplicity, we'll just keep the original value here
                    // In a real implementation, you might want to generate recursive masking code
                    writer.write("            $nestedPropertyName = $propertyName.$nestedPropertyName")
                } else {
                    // If no further nested properties need masking, keep the original value
                    writer.write("            $nestedPropertyName = $propertyName.$nestedPropertyName")
                }
            }

            if (index < nestedProperties.size - 1) {
                writer.write(",")
            }
            writer.write("\n")
        }

        writer.write("        ),\n")
    }

    private fun cleanGeneratedFiles() {
        codeGenerator.generatedFile.forEach { file -> file.deleteRecursively() }
    }
}

private fun KSClassDeclaration.implements(targetInterface: KSClassDeclaration): Boolean = superTypes.any {
    it.resolve().declaration == targetInterface
}


class MaskingGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        FunctionGenerator(environment.codeGenerator, environment.logger)
}

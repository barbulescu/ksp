package com.example.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class FunctionGenerator(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classesToProcess = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.isModel() || it.hasMaskedProperties() }

        classesToProcess
            .filter { it.isModel() }
            .onEach { logger.info("Found class implementing Model: ${it.simpleName.asString()}") }
            .forEach { classDeclaration ->
                // Collect all properties with @Mask annotation
                val maskedProperties = mutableListOf<MaskedProperty>()
                collectMaskedProperties(classDeclaration, resolver, "", maskedProperties)

                // Generate mask function for this class only if it has masked properties
                if (maskedProperties.isNotEmpty()) {
                    generateMaskFunction(classDeclaration, maskedProperties)
                }
            }

        // Process classes with masked properties that don't implement Model
        classesToProcess
            .filter { it.hasMaskedProperties() && !it.isModel() }
            .onEach { logger.info("Found class with @Mask annotation: ${it.simpleName.asString()}") }
            .forEach { classDeclaration ->
                // Collect all properties with @Mask annotation
                val maskedProperties = mutableListOf<MaskedProperty>()
                collectMaskedProperties(classDeclaration, resolver, "", maskedProperties)

                // Generate mask function for this class
                if (maskedProperties.isNotEmpty()) {
                    generateMaskFunction(classDeclaration, maskedProperties)
                }
            }

        return emptyList()
    }

    private fun collectMaskedProperties(
        classDeclaration: KSClassDeclaration,
        resolver: Resolver,
        propertyPath: String,
        maskedProperties: MutableList<MaskedProperty>,
    ) {
        classDeclaration.getAllProperties().forEach { property ->

            val currentPath = if (propertyPath.isEmpty()) property.simpleName.asString()
            else "$propertyPath.${property.simpleName.asString()}"

            if (property.isMasked()) {
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

                        // Check if this property itself has @Mask annotation or if the class implements Model
                        val hasMaskAnnotation = property.annotations.any { it.shortName.asString() == "Mask" }
                        val isModelImplementation = classDeclaration.isModel()
                        val isStringProperty = propertyType.toString().contains("kotlin.String")

                        logger.info("Property: $propertyName, hasMaskAnnotation: $hasMaskAnnotation, isModelImplementation: $isModelImplementation, isStringProperty: $isStringProperty, propertyType: ${propertyType.toString()}")

                        if (hasMaskAnnotation || (isModelImplementation && isStringProperty)) {
                            // If property has @Mask annotation or the class implements Model and property is a String, set it to empty string
                            logger.info("Masking property: $propertyName")
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
        // Check if this class implements Model
        val implementsModel = propertyTypeDeclaration.isModel()

        if (implementsModel) {
            // If the class has its own mask function, use it
            writer.write("        $propertyName = $propertyName.mask(),\n")
            return
        }

        // Otherwise, create a deep copy of the nested object with masked properties
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
                // Get property type
                val propertyType = nestedProperty.type.resolve()
                val propertyTypeDecl = propertyType.declaration

                // Check if this nested property has further nested properties that need masking
                val furtherNestedMaskedProperties = nestedMaskedProperties.filter {
                    it.propertyPath.startsWith("$nestedPropertyPath.")
                }

                if (furtherNestedMaskedProperties.isNotEmpty() && propertyTypeDecl is KSClassDeclaration) {
                    // Check if this nested property's class implements Model or has masked properties
                    val nestedImplementsModel = propertyTypeDecl.isModel()
                    val nestedHasMaskedProperties = propertyTypeDecl.hasMaskedProperties()

                    if (nestedImplementsModel || nestedHasMaskedProperties) {
                        // If the nested class has its own mask function, use it
                        writer.write("            $nestedPropertyName = $propertyName.$nestedPropertyName.mask()")
                    } else {
                        // Otherwise, keep the original value
                        writer.write("            $nestedPropertyName = $propertyName.$nestedPropertyName")
                    }
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

}

private fun KSClassDeclaration.hasMaskedProperties(): Boolean = getAllProperties().any { it.isMasked() }

class MaskingGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        FunctionGenerator(environment.codeGenerator, environment.logger)
}

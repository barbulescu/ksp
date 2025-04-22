package com.example.annotations

/**
 * Annotation to generate a simple function in the annotated class.
 *
 * @param name The name of the function to generate
 * @param returnType The return type of the function (default is "String")
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateFunction(
    val name: String,
    val returnType: String = "String"
)

package com.example.app

import com.example.annotations.GenerateFunction
import com.example.annotations.Include

/**
 * A sample class that uses the GenerateFunction annotation.
 */
@GenerateFunction(name = "sayHello", returnType = "String")
class SampleClass(
    val name: String,
    @Include val age: Int,
    @Include val email: String = "example@example.com"
) {

    fun regularFunction(): String {
        return "This is a regular function"
    }
}

/**
 * Main function to demonstrate the generated code.
 */
fun main() {
    val sample = SampleClass(name = "KSP", age = 12, email = "ksp@example.com")

    // Call the regular function
    println("Regular function: ${sample.regularFunction()}")

    // Call the generated function
    println("Generated function: ${sample.sayHello()}")

    sample.printName()
}

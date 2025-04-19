package com.example.app

import com.example.annotations.GenerateFunction

/**
 * A sample class that uses the GenerateFunction annotation.
 */
@GenerateFunction(name = "sayHello", returnType = "String")
class SampleClass {
    fun regularFunction(): String {
        return "This is a regular function"
    }
}

/**
 * Main function to demonstrate the generated code.
 */
fun main() {
    val sample = SampleClass()
    
    // Call the regular function
    println("Regular function: ${sample.regularFunction()}")
    
    // Call the generated function
    println("Generated function: ${sample.sayHello()}")
}

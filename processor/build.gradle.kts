plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.10-1.0.13")
    
    // For testing if needed
    testImplementation(kotlin("test"))
}

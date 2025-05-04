plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.20-2.0.0")

    // For testing if needed
    testImplementation(kotlin("test"))
}

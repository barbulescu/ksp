package com.example.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

data class NoMaskedProperty(val value: String) : Model

class NoMaskedPropertyTest {
    @Test
    fun `no masked property`() {
        try {
            val maskFunctions = Class.forName("${this.javaClass.packageName}.NoMaskedPropertyMaskKt")
                .methods
                .filter { it.name == "mask" }

            assertThat(maskFunctions).isEmpty()
        } catch (e: ClassNotFoundException) {
            assertThat(e.message).contains("NoMaskedPropertyMaskKt")
        }
    }
}

package com.example.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

data class A2(
    val value: String,
    val b: B2,
) : Model

data class B2(
    @Mask
    val value: String,
)

class NoMaskingInModelTest {
    @Test
    fun `check masking works when no masking in model`() {
        val a = A2(
            value = "a2",
            b = B2(value = "b2")
        )

        val masked = a.mask()
        assertThat(masked.b.value).isEmpty()
    }
}

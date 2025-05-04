package com.example.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

data class A1(
    @Mask
    val value: String,
    val b: B1,
    val d: D1,
) : Model

data class B1(
    val value: String,
    val c: C1,
)

data class C1(
    @Mask
    val value: String,
)

data class D1(val d1: String)

class MultiLevelMaskingTest {
    @Test
    fun `check masking on multiple levels`() {
        val a = A1(
            value = "a1",
            b = B1(
                value = "b1",
                c = C1(value = "c1")
            ),
            d = D1(d1 = "d1")
        )

        val masked = a.mask()
        assertThat(masked.value).isEmpty()
        assertThat(masked.b.c.value).isEmpty()
    }
}

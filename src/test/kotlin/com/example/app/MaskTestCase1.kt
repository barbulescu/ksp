package com.example.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

data class Inner(@Mask val value: String) : Model

data class A1(
    @Mask
    val a1: String,
    val b1: B1,
    val d1: D1,
) : Model

data class B1(
    val b1: String,
    val c1: C1,
)

data class C1(
    @Mask
    val c1: String,
)

data class D1(val d1: String)

class MaskTest {
    @Test
    fun `handle inner classes`() {
        val inner = Inner("value")
        val modified = inner.mask()
        assertThat(modified.value).isEmpty()
    }

    @Test
    fun `check masking`() {
        val c1 = C1(c1 = "c1")
        val b1 = B1(b1 = "b1", c1 = c1)
        val a1 = A1(a1 = "a1", b1 = b1, d1 = D1(d1 = "d1"))

        val a2 = a1.mask()
        assertThat(a2.a1).isEmpty()
        assertThat(a2.b1.c1.c1).isEmpty()
    }
}

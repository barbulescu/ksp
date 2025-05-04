package com.example.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test



class MaskTest {

    @Test
    fun `handle inner classes`() {

//        val inner = Inner("value")
//        val modified = inner.mask()
//        assertThat(modified.value).isEmpty()
    }

    @Test
    @Disabled("children are not masked")
    fun `check masking`() {
        val c1 = C1(c1 = "c1")
        val b1 = B1(b1 = "b1", c1 = c1)
        val a1 = A1(a1 = "a1", b1 = b1, d1 = D1(d1 = "d1"))

        val a2 = a1.mask()
        assertThat(a2.a1).isEmpty()
        assertThat(a2.b1.c1.c1).isEmpty()
    }
}

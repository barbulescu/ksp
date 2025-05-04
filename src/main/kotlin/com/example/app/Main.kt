package com.example.app

fun main() {
    val a1 = A1(
        a1 = "a1",
        b1 = B1(
            b1 = "b1",
            c1 = C1(
                c1 = "c1",
            ),
        ),
        d1 = D1(d1 = "d1"),
    )

    println(a1)
}

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

//data class Inner(@Mask val value: String)

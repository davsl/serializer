package test

import sliep.jes.serializer.implement

fun main() {
    val daa = implement<DAA> { name, _ -> name }
    System.err.println(daa.isis)
    daa.isis = "refv"
    daa.sdrogo()
}

interface DAA {
    var isis: String

    fun sdrogo()
}
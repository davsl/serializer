package test

import sliep.jes.serializer.Loggable
import sliep.jes.serializer.implement

fun main() {
    val daa = implement<DAA> { name, _ -> name }
    System.err.println(daa.isis)
    daa.isis = "refv"
    daa.sdrogo()
    MyClass().doTask()
    MyClass.LOG = false
}

private interface DAA {
    var isis: String

    fun sdrogo()
}

class MyClass : Loggable {
    fun doTask() {
        val hello = "world"
        log { "Hello is $hello" } //will be executed ONLY if variable LOG is true
        depth++
        for (i in 0..10) {
            System.out.println(i * 1234)
            log { "Computing $i" }
        }
        depth--
        log { "Fineeshhh" }
    }

    var depth = 0

    companion object {
        var LOG = true
    }
}
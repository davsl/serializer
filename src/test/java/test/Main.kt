package test

import org.json.JSONObject
import sliep.jes.serializer.Loggable
import sliep.jes.serializer.fromJson

fun main() {
    val myClass = MyClass()
    myClass.depth = 5
    myClass.depth3 = 5
    val fromJson = JSONObject("{\"depth\":3}").fromJson<MyClass>()
    System.out.println(fromJson.depth)
    System.out.println(fromJson.depth3)
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
    var depth3 = 0

    companion object {
        var LOG = true
    }
}
import sliep.jes.serializer.field
import sliep.jes.serializer.invokeMethod

fun main() {
    Dooo().invokeMethod(String::class, "rrrr")
    Dooo.invokeMethod("st")
    System.err.println(Dooo().field("hero"))
}

class Dooo {
    val hero = "WSDFG"
    fun rrr() {
        System.out.println("dfgdb")
    }

    fun rrrr(): String? {
        System.out.println("gtrdhba")
        return "ddd"
    }

    companion object {
        fun st() {
            System.out.println("STATIC srfghse")
        }
    }
}
import org.json.JSONObject
import sliep.jes.serializer.*
import java.util.*

private val myVal: String
    get() = lateInit {
        gg()
        "d"
    }

private val myVal2: String
    get() = lateInit {
        gg()
        "d"
    }
var i = 0
fun gg() {
    System.err.println("DDDDDDDDDDDD" + i++)
}
fun main() {
//    Loggable.setLog(JesSerializer as Loggable)
    val trueField = java.lang.Boolean::class.fieldR("TRUE")
    trueField.isFinal = false
    trueField[null] = false
    System.err.println("value: ${java.lang.Boolean.TRUE}")
    System.err.println("value: $myVal")
    System.err.println("value: $myVal")
    System.err.println("value: $myVal2")
    System.err.println("value: $myVal2")
    System.err.println("value: $myVal")



    val dooo = Dooo()
    dooo.zozo["adcfsafc"] = "edfg"
    dooo.zozo["aa"] = "ss"
    dooo.zuzu.add("asdfg")
    val toJson = JSONObject(dooo.toJson().toString())
//    val toJson = dooo.toJson()
//    System.err.println(toJson.toString(2))

//    val arrayToJson = JesSerializer.arrayToJson(arrayOf(Dooo(), Dooo()))
    val fromJson = toJson.fromJson<Dooo>()
    System.err.println(fromJson.zozo["adcfsafc"])
    System.err.println(fromJson.zozo["aa"])
    System.err.println(fromJson.zuzu[0])
//    JesSerializer.fromJsonArray(arrayToJson, Dooo::class)
}
class Dooo : JesObject, Loggable {
    @Transient
    private val dddd = "WSDFG"
    val zozo = HashMap<String, String>()
    val zuzu = ArrayList<String>()
    val hero = "WSDFG"
    var soooooca = Dooo3()
    var aaaa = arrayOf(Aro(), Aro())
    var fghj = arrayOf(1, 2, 3)
    var hddg = 344

    val hh: String? get() = lateInit { null }

    fun rrr() {
        System.out.println(!LOG)
        log { "it  was true" }
    }

    fun rrrr(): String? {
        System.out.println("gtrdhba")
        return "ddd"
    }

    companion object {
        var LOG = false
        fun st() {
            System.out.println("STATIC srfghse")
        }
    }
}

class Aro : JesObject {
    val ddd = "QWesdf"
}

class Dooo3 : JesObject {
    @Transient
    val dddd = "WSDFG"
    val hero = "WSDFG"
    var hddg = "WSDFG"
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
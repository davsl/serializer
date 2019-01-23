import sliep.jes.serializer.JesObject
import sliep.jes.serializer.JesSerializer
import sliep.jes.serializer.Loggable

fun main() {
    Loggable.setLog(JesSerializer)
    val toJson = JesSerializer.toJson(Dooo())
    val arrayToJson = JesSerializer.arrayToJson(arrayOf(Dooo(), Dooo()))
    JesSerializer.fromJson(toJson, Dooo::class)
    JesSerializer.fromJsonArray(arrayToJson, Dooo::class)
}

class Dooo : JesObject {
    @Transient
    val dddd = "WSDFG"
    val hero = "WSDFG"
    var soooooca = Dooo3()
    var aaaa = arrayOf(Aro(), Aro())
    var fghj = arrayOf(1, 2, 3)
    var hddg = 344

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
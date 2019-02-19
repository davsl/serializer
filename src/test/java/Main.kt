import sliep.jes.serializer.JesObject
import sliep.jes.serializer.JesSerializer
import sliep.jes.serializer.fromJson
import sliep.jes.serializer.toJson

fun main() {
    JesSerializer.LOG = true
    val toJson = Muuuu().toJson()
    System.err.println(toJson)
    val fromJson = toJson.fromJson<Muuuu>()
    System.err.println(fromJson)
}

class Muuuu : JesObject {
    val dd = "dqwafes"
    val ddd = 44
//    override fun toString() = toJson().toString()
}
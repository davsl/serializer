import org.json.JSONObject
import sliep.jes.serializer.*
import java.util.*

fun main() {
//    Loggable.setLog(JesSerializer as Loggable)

    /*val dooo = Dooo()
    dooo.zozo["adcfsafc"] = "edfg"
    dooo.zozo["aa"] = "ss"
    dooo.zuzu.add("asdfg")
    val toJson = JSONObject(JesSerializer.toJson(dooo).toString())
    System.err.println(toJson.toString(2))

//    val arrayToJson = JesSerializer.arrayToJson(arrayOf(Dooo(), Dooo()))
    val fromJson = JesSerializer.fromJson(toJson, Dooo::class)
    System.err.println(fromJson.zozo["adcfsafc"])
    System.err.println(fromJson.zozo["aa"])
    System.err.println(fromJson.zuzu[0])*/
//    JesSerializer.fromJsonArray(arrayToJson, Dooo::class)
    val compress = JSONObject(dd).compress()

    //TODO use json signature and hashcode of field instead of name
    System.err.println(dd.toByteArray().size)
    System.err.println(compress.size)
//    System.err.println(JSONObject(compress.decompress()).toString()==JSONObject(dd).toString())
    System.err.println(compress.decompress())
    System.err.println(JSONObject(dd).toString())
    System.err.println(JSONObject(dd).toString() == compress.decompress().toString())
}

val dd = "{\n" +
        "  \"trgwvw\": {\n" +
        "    \"tgvtre\": \"appligregvercation/json\",\n" +
        "    \"gregerg-Type\": \"appgergverlication/json\"\n" +
        "  },\n" +
        "  \"rega\": \"/api\",\n" +
        "  \"hoaergveragst\": \"http://gefvr.rgfveafera.refva\",\n" +
        "  \"nume\": 3,\n" +
        "  \"nuFme\": true,\n" +
        "  \"dewsfdgb\": 3.4567777777777654,\n" +
        "  \"f\": {\n" +
        "    \"waefa<f\": {\n" +
        "      \"aFCAFC\": \"afcweAF\",\n" +
        "      \"parFWAFCams\": [\n" +
        "        \"toFWAFCA<FCken\"\n" +
        "      ]\n" +
        "    },\n" +
        "    \"Few\": {\n" +
        "      \"methWEFAod\": \"awfca<f\",\n" +
        "      \"parFAWERFAams\": [\n" +
        "        \"weFCwfc\",\n" +
        "        \"wefc\"\n" +
        "      ]\n" +
        "    },\n" +
        "    \"Rwfcw\": {\n" +
        "      \"FWEf\": \"fwCEFC\",\n" +
        "      \"f\": [\n" +
        "        \"FWaf\",\n" +
        "        \"fwV\"\n" +
        "      ]\n" +
        "    },\n" +
        "    \"FWEAf\": {\n" +
        "      \"fwAF\": \"Fwfcwea\",\n" +
        "      \"fWC\": [\n" +
        "        \"FWWEfc\"\n" +
        "      ]\n" +
        "    }\n" +
        "  }\n" +
        "}"
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

    @Transient
    val hh = object : LateInitVal<String?>() {
        override fun initialize(): String? {
            return null
        }
    }

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
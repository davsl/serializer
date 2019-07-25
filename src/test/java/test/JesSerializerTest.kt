@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")

package test

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import sliep.jes.serializer.*
import java.util.*

class JesSerializerTest {

    @Test
    fun serializePrimitiveValues() {
        val modelTest = JSONObject(ModelTest.TEST_JSON).fromJson<ModelTest>()
        assertEquals("A B C D", modelTest.s)
        assertEquals(123, modelTest.i)
        assertEquals(45.6f, modelTest.f)
        assertEquals(78.9, modelTest.d, 0.0)
        assertEquals(true, modelTest.b)
        assertEquals('c', modelTest.c)
        assertEquals(34567654345678, modelTest.l)
        assertEquals("E F G", modelTest.o.s)
    }

    @Test
    fun serializeDeserialize() {
        val originalJson = JSONObject(ModelTest.TEST_JSON)
        val modelTesto = originalJson.fromJson<ModelTest>()
        val modelTest = modelTesto.toJson()
        assertEquals(modelTest.toString(), originalJson.toString())
        assertEquals(modelTest.toString(), modelTest.fromJson<ModelTest>().toJson().toString())
        val fromJson = modelTest.fromJson<ModelTest>()
        fromJson.s = "ARRRRRRG"
        assertEquals(fromJson.ciccio, AnEnumClass.SSUUS)
        modelTest.fromJson(fromJson)
        assertEquals("A B C D", fromJson.s)
    }

    @Test
    fun jesObjectImpl() {
        val modelTest = JSONObject(ModelTest.TEST_JSON).fromJson<ModelTest>()
        modelTest.impl = ModelImplTest("fversv", 24)
        val copy = modelTest.toJson().fromJson<ModelTest>().toJson().toString()
        assertEquals(modelTest.toJson().toString(), copy)
    }

    @Test
    fun jsonArrayToArray() {
        val ar1 = JSONArray("[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"]")
        val ar2 = JSONArray("[1,2,3,4,5]")
        assertEquals("c", ar1.toTypedArray<String>()[2])
        assertEquals(4, ar2.toTypedArray<Int>()[3])
    }

    @Test
    fun testEnum() {
        val modelTest = JSONObject(ModelTest.TEST_JSON).fromJson<ModelTest>()
        assertEquals(modelTest.ciccio, AnEnumClass.SSUUS)
    }

    enum class AnEnumClass(override val value: Int) : ValueEnum {
        CANE(1),
        SSUUS(2),
        SOSOSOO(3)
    }

    class ModelImplTest(val a: String, val b: Int) : JesObjectImpl<String> {
        constructor(data: String) : this(
            data.substring(0, data.indexOf("----")),
            data.substring(data.indexOf("----") + 4).toInt()
        )

        override fun toJson(): String = "$a----$b"
    }

    data class Skkkk(val ulul: String, val ddfdf: Int) : JesObject
    data class ModelTest(
        @JesName("pippo")
        var s: String,
        val i: Int,
        val f: Float,
        val ciccio: AnEnumClass,
        val d: Double,
        val b: Boolean,
        val c: Char,
        val l: Long,
        @JesDate("HH:mm:ss")
        val puzzy: Date,
        val deee: Array<Int>,
        val doo: List<Skkkk>,
        val mappy: Map<Int, Skkkk>,
        val o: ModelTest2,
        var impl: ModelImplTest
    ) : JesObject {
        companion object {
            const val TEST_JSON = "{\n" +
                    "  \"pippo\": \"A B C D\",\n" +
                    "  \"i\": 123,\n" +
                    "  \"ciccio\": 2,\n" +
                    "  \"deee\": [1,2,3,5,78,8],\n" +
                    "  \"doo\": [{" +
                    "    \"ulul\": \"E F G\",\n" +
                    "    \"ddfdf\": 24\n" +
                    "  }],\n" +
                    "  \"mappy\": {\"1\":{" +
                    "    \"ulul\": \"E F G\",\n" +
                    "    \"ddfdf\": 24\n" +
                    "  }},\n" +
                    "  \"f\": 45.6,\n" +
                    "  \"d\": 78.9,\n" +
                    "  \"b\": true,\n" +
                    "  \"c\": \"c\",\n" +
                    "  \"l\": 34567654345678,\n" +
                    "  \"puzzy\": \"13:14:00\",\n" +
                    "  \"o\": {\n" +
                    "    \"s\": \"E F G\"\n" +
                    "  }\n" +
                    "}"
        }
    }

    class ModelTest2(
        val s: String
    ) : JesObject
}
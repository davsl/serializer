@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")
package test

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import sliep.jes.serializer.*

class JesSerializerTest {
    @Test
    fun serializePrimitiveValues() {
//        JesSerializer.LOG = true
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
        val modelTest = originalJson.fromJson<ModelTest>().toJson()
        assertEquals(modelTest.toString(), originalJson.toString())
        assertEquals(modelTest.toString(), modelTest.fromJson<ModelTest>().toJson().toString())
        val fromJson = modelTest.fromJson<ModelTest>()
        fromJson.s = "ARRRRRRG"
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
        assertEquals("c", ar1.toArrayOf<String>()[2])
        assertEquals(4, ar2.toArrayOf<Int>()[3])
    }

    class ModelImplTest(val a: String, val b: Int) : JesObjectImpl<String> {
        constructor(jes: JesConstructor<String>) : this(
            jes.data.substring(0, jes.data.indexOf("----")),
            jes.data.substring(jes.data.indexOf("----") + 4).toInt()
        )

        override fun toJson(): String = "$a----$b"
    }

    class ModelTest(
        var s: String,
        val i: Int,
        val f: Float,
        val d: Double,
        val b: Boolean,
        val c: Char,
        val l: Long,
        val o: ModelTest2,
        var impl: ModelImplTest
    ) : JesObject {
        companion object {
            const val TEST_JSON = "{\n" +
                    "  \"s\": \"A B C D\",\n" +
                    "  \"i\": 123,\n" +
                    "  \"f\": 45.6,\n" +
                    "  \"d\": 78.9,\n" +
                    "  \"b\": true,\n" +
                    "  \"c\": \"c\",\n" +
                    "  \"l\": 34567654345678,\n" +
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
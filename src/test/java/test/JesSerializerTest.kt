package test

import org.json.JSONObject
import org.junit.Test
import sliep.jes.serializer.*

class JesSerializerTest {
    @Test
    fun serializePrimitiveValues() {
        val modelTest = JSONObject(ModelTest.TEST_JSON).fromJson<ModelTest>()
        assert(modelTest.s == "A B C D")
        assert(modelTest.i == 123)
        assert(modelTest.f == 45.6f)
        assert(modelTest.d == 78.9)
        assert(modelTest.b)
        assert(modelTest.c == 'c')
        assert(modelTest.l == 34567654345678)
        assert(modelTest.o.s == "E F G")
    }

    @Test
    fun serializeDeserialize() {
        val originalJson = JSONObject(ModelTest.TEST_JSON)
        val modelTest = originalJson.fromJson<ModelTest>().toJson()
        assert(originalJson.toString() == modelTest.toString())
        assert(modelTest.fromJson<ModelTest>().toJson().toString() == modelTest.toString())
    }

    @Test
    fun jesObjectImpl() {
        val modelTest = JSONObject(ModelTest.TEST_JSON).fromJson<ModelTest>()
        modelTest.impl = ModelImplTest("fversv", 24)
        val copy = modelTest.toJson().fromJson<ModelTest>().toJson().toString()
        assert(modelTest.toJson().toString() == copy)
    }

    class ModelImplTest(val a: String, val b: Int) : JesObjectImpl<String> {
        constructor(jes: JesConstructor<String>) : this(
            jes.data.substring(0, jes.data.indexOf("----")),
            jes.data.substring(jes.data.indexOf("----") + 4).toInt()
        )

        override fun toJson(): String = "$a----$b"
    }

    class ModelTest(
        val s: String,
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
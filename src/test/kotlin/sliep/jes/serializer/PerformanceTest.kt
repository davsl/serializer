@file:Suppress("SpellCheckingInspection")

package sliep.jes.serializer

import com.google.gson.Gson
import org.json.JSONArray
import org.junit.Assert
import org.junit.Test
import java.io.InputStreamReader

class PerformanceTest {

    //4.49
    //0.26
    @Test
    fun jesVsGson() {
        val input =
            JSONArray(InputStreamReader(this::class.java.getResourceAsStream("/test.json")).use { it.readText() })

        System.err.println("Jes vs Gson performance test (${input.length()} bytes)")
        System.err.println()

        lateinit var gsonInstance: Gson
        lateinit var gson: Array<TestJson>
        lateinit var jes: Array<TestJson>
        lateinit var gsonOrig: String
        lateinit var jesOrig: JSONArray
        var gt: Float
        var jt: Float

        bm("Gson instantiation took ") { gsonInstance = Gson() }
        gt = bm("Deserializing Gson:") { gson = gsonInstance.fromJson(input.toString(), Array<TestJson>::class.java) }
        jt = bm("Deserializing Jes:") { jes = input.fromJson() }
        System.err.println("Jes is ${gt / jt} times faster than gson")
        Assert.assertArrayEquals(jes, gson)
        System.err.println("CONTENT IS IDENTICAL")
        System.err.println()

        gt = bm("Deserializing Gson 100 times avg:", 100) {
            gsonInstance.fromJson(
                input.toString(),
                Array<TestJson>::class.java
            )
        }
        jt = bm("Deserializing Jes 100 times avg:", 100) { input.fromJson<Array<TestJson>>() }
        System.err.println("Jes is ${gt / jt} times faster than gson")
        System.err.println()

        gt = bm("Serializing Gson:") { gsonOrig = gsonInstance.toJson(gson) }
        jt = bm("Serializing Jes:") { jesOrig = jes.toJson() }
        System.err.println("Jes is ${gt / jt} times faster than gson")
        Assert.assertEquals(JSONArray(gsonOrig).toString(), jesOrig.toString())
        Assert.assertEquals(input.toString(), jesOrig.toString())
        System.err.println("CONTENT IS IDENTICAL")
        System.err.println()

        gt = bm("Serializing Gson 100 times avg:", 100) { gsonInstance.toJson(gson) }
        jt = bm("Serializing Jes 100 times avg:", 100) { gson.toJson() }
        System.err.println("Jes is ${gt / jt} times faster than gson")
        System.err.println()
    }

    private inline fun bm(message: String, repeatCount: Int = 1, block: () -> Unit): Float {
        val start = System.nanoTime()
        for (i in 0 until repeatCount) block()
        val result = ((System.nanoTime() - start) / repeatCount) / 1000_000f
        System.err.println("$message $result ms")
        return result
    }

    data class TestJson(
        @JvmField val _id: String,
        @JvmField val index: Int,
        @JvmField val guid: String,
        @JvmField val isActive: Boolean,
        @JvmField val balance: String,
        @JvmField val picture: String,
        @JvmField val age: Int,
        @JvmField val eyeColor: String,
        @JvmField val name: TestJsonName,
        @JvmField val company: String,
        @JvmField val email: String,
        @JvmField val phone: String,
        @JvmField val address: String,
        @JvmField val about: String,
        @JvmField val registered: String,
        @JvmField val latitude: String,
        @JvmField val longitude: String,
        @JvmField val tags: Array<String>,
        @JvmField val range: IntArray,
        @JvmField val friends: Array<TestJsonFriend>,
        @JvmField val greeting: String,
        @JvmField val favoriteFruit: String
    ) : JesObject {
        data class TestJsonName(
            @JvmField val first: String,
            @JvmField val last: String
        ) : JesObject

        data class TestJsonFriend(
            @JvmField val id: Int,
            @JvmField val name: String
        ) : JesObject

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TestJson

            if (_id != other._id) return false
            if (index != other.index) return false
            if (guid != other.guid) return false
            if (isActive != other.isActive) return false
            if (balance != other.balance) return false
            if (picture != other.picture) return false
            if (age != other.age) return false
            if (eyeColor != other.eyeColor) return false
            if (name != other.name) return false
            if (company != other.company) return false
            if (email != other.email) return false
            if (phone != other.phone) return false
            if (address != other.address) return false
            if (about != other.about) return false
            if (registered != other.registered) return false
            if (latitude != other.latitude) return false
            if (longitude != other.longitude) return false
            if (!tags.contentEquals(other.tags)) return false
            if (!range.contentEquals(other.range)) return false
            if (!friends.contentEquals(other.friends)) return false
            if (greeting != other.greeting) return false
            if (favoriteFruit != other.favoriteFruit) return false

            return true
        }

        override fun hashCode(): Int {
            var result = _id.hashCode()
            result = 31 * result + index
            result = 31 * result + guid.hashCode()
            result = 31 * result + isActive.hashCode()
            result = 31 * result + balance.hashCode()
            result = 31 * result + picture.hashCode()
            result = 31 * result + age
            result = 31 * result + eyeColor.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + company.hashCode()
            result = 31 * result + email.hashCode()
            result = 31 * result + phone.hashCode()
            result = 31 * result + address.hashCode()
            result = 31 * result + about.hashCode()
            result = 31 * result + registered.hashCode()
            result = 31 * result + latitude.hashCode()
            result = 31 * result + longitude.hashCode()
            result = 31 * result + tags.contentHashCode()
            result = 31 * result + range.contentHashCode()
            result = 31 * result + friends.contentHashCode()
            result = 31 * result + greeting.hashCode()
            result = 31 * result + favoriteFruit.hashCode()
            return result
        }
    }
}
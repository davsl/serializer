package test

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import sliep.jes.serializer.*
import sliep.jes.serializer.impl.JesDate
import sliep.jes.serializer.impl.JesName
import java.util.*

class UtilityTest {
    @Test
    fun testToDynamic() {
        val g = 3
        val s = g.toDynamic(Float::class.java)
        System.err.println(g)
        System.err.println(s)
    }

    companion object {
        const val HELLO = 0x234
        const val WORLD = 0x567
    }

    @Test
    fun testToArray() {
        val list = listOf<CharSequence>("A", "B", "C")
        try {
            @Suppress("UNCHECKED_CAST")
            list.toTypedArray() as Array<String>
            assert(false)
        } catch (e: Throwable) {
            println(e)
        }
        list.toTypedArray(String::class.java)
    }

    @Test
    fun testFlags() {
        val f1 = HELLO or WORLD
        assertTrue(f1 includes HELLO)
        assertTrue(f1 includes WORLD)
        val f2 = f1 and WORLD.inv()
        assertTrue(f2 excludes WORLD)
        val f3 = f2 or HELLO
        assertTrue(f3 includes HELLO)
    }

    @Test
    fun dtoEditorTest() {
        val fff = Bb(1, "we", 1.3f)
        val editor = edit<TestEditor>(object : DTOEditorCallback {
            override fun onCommit(result: JSONObject) = System.err.println("COMMIT $result")

            override fun onPropertyChanged(prop: String, newValue: Any?, state: PropertyState) =
                when (state) {
                    PropertyState.ADDED -> println("Added $prop -> $newValue")
                    PropertyState.CHANGED -> println("Editing $prop -> $newValue")
                    PropertyState.REMOVED -> println("Removed $prop")
                }
        })
        editor.prop1 = 23
        assertEquals(23, editor.prop1)
        editor.prop1 = 24
        editor.prop1 = 25
        editor.prop1 = 26
        editor.prop2 = "qwsdf"
        assertEquals("qwsdf", editor.prop2)
        editor.prop2 = "fgh"
        editor.prop2 = null
        assertTrue(null == editor.prop2)
        editor.prop3 = 2.34f
        editor.remove(Bb::prop3)
        try {
            editor.prop3
            assertTrue(false)
        } catch (e: NullPointerException) {
        }
        editor["deeo"] = "bubu"
        assertEquals("bubu", editor["deeo"])
        editor.commit()
        val t: Bb = editor.build()
        println("Built -> $t")
        println("Original -> $fff")
        editor.sync(fff, true)
        editor.remove("deeo")
        editor.sync(fff)
        println("Sync -> $fff")
        assertFalse(editor.isEmpty)
        editor.clear()
        assertTrue(editor.isEmpty)
        editor.date = Date()
        editor.bibbo = editor.date.time.toString()
        assertEquals(editor.date, Date(editor.bibbo.toLong()))
        editor.commit()
    }

    data class Bb(val prop1: Int, val prop2: String?, val prop3: Float) : JesObject

    interface TestEditor : DTOEditor {
        var prop1: Int
        var prop2: String?
        var prop3: Float
        @set:JesDate("YY-mm HH-ss")
        var date: Date
        @set:JesName("pullo")
        var bibbo: String
    }

    @Test
    fun testLazy() {
        sooca
        sooca
        sooca
        sooca
        sooca
        sooca
        sooca
        sooca
        sooca
    }

    val sooca: String by lazy {
        System.err.println("LAZYYYYY")
        "dildo"
    }
}
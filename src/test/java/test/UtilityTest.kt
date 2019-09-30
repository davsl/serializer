package test

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import sliep.jes.serializer.*
import sliep.jes.serializer.impl.JesDate
import sliep.jes.serializer.impl.JesName
import java.lang.reflect.Modifier
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
        val flags = Flags()
        flags += Modifier.TRANSIENT
        flags += Modifier.PRIVATE
        flags += Modifier.FINAL
        assertTrue(flags includes Modifier.FINAL)
        assertTrue(Modifier.isFinal(flags.flags))
        assertFalse(Modifier.isPublic(flags.flags))
        flags -= Modifier.FINAL
        assertTrue(flags excludes Modifier.FINAL)
        assertFalse(Modifier.isFinal(flags.flags))
        assertTrue(Modifier.isTransient(flags.flags))
        assertTrue(Modifier.isPrivate(flags.flags))
        assertTrue(flags[Modifier.PRIVATE])
    }

    @Test
    fun dtoEditorTest() {
        val c = Cc(1, "we", 1.3f, "GO")
        val fff = Bb(1, "we", 1.3f, c)
        val editor = edit<TestEditor>(object : DTOEditorCallback {
            override fun onCommit(result: JSONObject) = System.err.println("COMMIT $result")

            override fun onPropertyChanged(owner: Class<*>, prop: String, newValue: Any?, state: PropertyState) =
                when (state) {
                    PropertyState.ADDED -> println("Added ${owner.simpleName}.$prop -> $newValue")
                    PropertyState.CHANGED -> println("Editing ${owner.simpleName}.$prop -> $newValue")
                    PropertyState.REMOVED -> println("Removed ${owner.simpleName}.$prop")
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
        editor.editor2.bibbo = "GA"
        check(null == editor.prop2)
        editor.prop3 = 2.34f
        editor.remove(Bb::prop3)
        if (editor.has(editor::prop3)) editor.prop3
        editor["deeo"] = "bubu"
        assertEquals("bubu", editor["deeo"])
        editor.commit()
        editor.editor2.bibbo = "GA"
        editor.editor2.bibbo = "GU"
        editor.editor2.prop2 = "sei ghei"
        val t: Bb = editor.build()
        println("Built -> $t")
        println("Original -> $fff")
        editor.sync(fff, true)
        editor.remove("deeo")
        editor.sync(fff)
        println("Sync -> $fff")
        check(!editor.isEmpty)
        editor.clear()
        check(editor.isEmpty)
        editor.date = Date()
        editor.bibbo = editor.date.time.toString()
        assertEquals(editor.date, Date(editor.bibbo.toLong()))
        editor.commit()
    }

    data class Bb(val prop1: Int, val prop2: String?, val prop3: Float, val editor2: Cc) : JesObject
    data class Cc(val prop1: Int, val prop2: String?, val prop3: Float, var bibbo: String) : JesObject

    interface TestEditor : DTOEditor {
        var prop1: Int
        var prop2: String?
        var prop3: Float
        @set:JesDate("YY-mm HH-ss")
        var date: Date
        @set:JesName("pullo")
        var bibbo: String
        val editor2: TestEditor2

        interface TestEditor2 : DTOEditor {
            var prop1: Int
            var prop2: String?
            var prop3: Float
            @set:JesDate("YY-mm HH-ss")
            var date: Date
            @set:JesName("pullo")
            var bibbo: String
        }
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
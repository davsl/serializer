package test

import org.junit.Assert.assertTrue
import org.junit.Test
import sliep.jes.serializer.excludes
import sliep.jes.serializer.includes
import sliep.jes.serializer.toDynamic

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
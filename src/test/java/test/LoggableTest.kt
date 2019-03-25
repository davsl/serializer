@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")

package test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import sliep.jes.serializer.LateInitVal
import sliep.jes.serializer.Loggable
import sliep.jes.serializer.lateInit

class LoggableTest {
    @Test
    fun serializeDeserialize() {
        Loggable.logger = object : Loggable.Logger {
            override fun log(tag: String, message: Any) {
                System.err.println("$tag $message")
                if (tag == "JDSG")
                    assertEquals("    true", message)
            }
        }
        LateInitVal.LOG = true
        val d = JDSG()
        val b = JDSG()
        d.depth++
        d.main()
        System.err.println(d.sooca == b.sooca)
        assertFalse(d.sooca == b.sooca)
    }

    class JDSG : MyClass()

    abstract class MyClass : Loggable {
        val sooca: String get() = lateInit(::sooca) { System.currentTimeMillis().toString() }
        fun main() {
            log { sooca == sooca }
            log { sooca == sooca }
            log { sooca == sooca }
            log { sooca == sooca }
        }

        var depth = 0

        companion object {
            var LOG = true
        }
    }
}
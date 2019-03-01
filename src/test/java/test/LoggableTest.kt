@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")

package test

import org.junit.Assert.assertEquals
import org.junit.Test
import sliep.jes.serializer.Loggable

class LoggableTest {
    @Test
    fun serializeDeserialize() {
        Loggable.logger = object : Loggable.Logger {
            override fun log(tag: String, message: Any) {
                assertEquals("    HELLO", message)
            }
        }
        val d = MyClass()
        d.depth++
        d.main()
    }

    class MyClass : Loggable {
        fun main() {
            log { "HELLO" }
        }

        var depth = 0

        companion object {
            var LOG = true
        }
    }
}
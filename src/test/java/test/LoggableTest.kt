@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")

package test

import org.junit.Test
import sliep.jes.serializer.Loggable

class LoggableTest {
    @Test
    fun loggableTest() {
        MyClass.LOG = true
        MyClass().fuck()
    }

    class MyClass : Loggable {
        override var depth: Int = 1
        override val logEnabled = LOG

        init {
            log { "DSVHUSJ DSJVSNB" }
        }

        fun fuck() {
            log { "YOU" }
        }

        companion object {
            var LOG = true
        }
    }
}
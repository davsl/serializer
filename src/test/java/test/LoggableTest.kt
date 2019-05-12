@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")

package test

import org.junit.Test
import sliep.jes.serializer.Loggable
import sliep.jes.serializer.logAs

class LoggableTest {
    @Test
    fun loggableTest() {
        MyClass.LOG = true
        MyClass().fuck()
        logAs<MyClass>(1) { "SOOCA" }
    }

    class MyClass : Loggable {
        override var depth: Int = 1
        override var LOG = Companion.LOG

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
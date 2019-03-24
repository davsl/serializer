@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")

package test

import org.junit.Assert.assertEquals
import org.junit.Test
import sliep.jes.serializer.Loggable
import sliep.jes.serializer.lateInit

class LoggableTest {
    @Test
    fun serializeDeserialize() {
        Loggable.logger = object : Loggable.Logger {
            override fun log(tag: String, message: Any) {
//                System.err.println(tag)
                assertEquals("    HELLO", message)
            }
        }
        val d = object : MyClass() {}
        d.depth++
        d.main()

        d.apply {

            System.err.println(gesoo)
            System.err.println(gesoou)
            System.err.println(gesoo)
            System.err.println(gesoou)
            System.err.println(gesoo)
            System.err.println(gesoou)
            System.err.println(gesoo)
            System.err.println(gesoou)
        }
        object : MyClass() {}.apply {

            System.err.println(gesoo)
            System.err.println(gesoou)
            System.err.println(gesoo)
            System.err.println(gesoou)
            System.err.println(gesoo)
            System.err.println(gesoou)
            System.err.println(gesoo)
            System.err.println(gesoou)
        }
        System.err.println(ddd)
        System.err.println(xxx)
        System.err.println(ddd)
        System.err.println(xxx)
        System.err.println(ddd)
        System.err.println(xxx)
        System.err.println(ddd)
        System.err.println(xxx)

    }


    class JDSG : MyClass()

    abstract class MyClass : Loggable {
        fun main() {
            log { "HELLO" }
            log { "HELLO" }
            log { "HELLO" }
            log { "HELLO" }
            log { "HELLO" }
        }

        val gesoo
            get() = lateInit {
                System.err.println("PORCO")
            }
        val gesoou
            get() = lateInit {
                System.err.println("DIO")
            }

        var depth = 0

        companion object {
            var LOG = true
        }
    }
}

val ddd
    get() = lateInit {
        System.err.println("ddd")
    }
val xxx
    get() = lateInit {
        System.err.println("xxx")
    }
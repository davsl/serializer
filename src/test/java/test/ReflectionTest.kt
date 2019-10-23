@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")

package test

import org.junit.Assert.assertEquals
import org.junit.Test
import sliep.jes.serializer.*
import java.lang.reflect.Modifier
import kotlin.system.measureNanoTime

class ReflectionTest {

    open class Mother {
        @JvmField
        var hello = "hello"
        private var world = "world"
        protected open var pro = "pro"
        private fun helloo() {
            System.err.println("HELLOO")
        }
    }

    class Child : Mother() {
        @JvmField
        var hello2 = "hello2"
        private var world = "world"
        override var pro: String = "pru"
        private fun world() {
            System.err.println("WORLD")
        }
    }

    @Test
    fun testMethod() {
        val child = Child()
        val allMethods = child::class.java.allMethods
        val allFields = child::class.java.allFields
        System.err.println(allFields.contentToString())
        System.err.println(allMethods.contentToString())
    }

    @Test
    fun constructorTest() {
        val newInstance = ModelTest::class.java.instantiate()
        assertEquals("Hello", newInstance.susu)
        assertEquals(3, ModelTest::class.java.declaredConstructors.filter { Modifier.isPublic(it.modifiers) }.size)
        assertEquals(5, ModelTest::class.java.instantiate(5).i)
        assertEquals(0, build<ModelTest> { }.i)
    }

    @Test
    fun fieldsTest() {
        val instance = ModelTest::class.java.instantiate()
        assertEquals(instance.susu, instance.fieldValue<String>("susu"))
        assertEquals(instance.susu, instance::class.java.field("susu")[instance])

        val d = Deeo()
        val size = 7
        val repeat = 100
        val firsRreflection = measureNanoTime {
            d::class.java.declaredFields.forEach {
                it.isAccessible = true
                it.get(d)
            }
        } / 10000f / size
        val firstJes = measureNanoTime {
            d::class.java.allFields.forEach { it.get(d) }
        } / 10000f / size
        val secondJes = measureNanoTime {
            repeat(repeat) {
                d::class.java.allFields.forEach { it.get(d) }
            }
        } / 10000f / repeat / size
        val secondReflection = measureNanoTime {
            repeat(repeat) {
                d::class.java.declaredFields.forEach {
                    it.isAccessible = true
                    it.get(d)
                }
            }
        } / 10000f / repeat / size
        println()

        var field = d.fieldValue<Deeo.Buddhah>("ciccio")
        d.setFieldValue("ciccio", field.also { it.ew = "suca" })
        field = d.fieldValue("ciccio")
        var primo: Int = field.fieldValue("primo")
        field.setFieldValue("primo", 233)
        primo = field.fieldValue("primo")
        assertEquals(233, primo)
        val kree = d.fieldValue<Any>("kree")
        val gg = d.fieldValue<Any>("ciccio", "ew")

        println()
    }

    class Deeo {
        @JvmField
        var ciccio = Buddhah()
        @JvmField
        var cane = 23
        @JvmField
        var sdcvvs = true
        @JvmField
        var dcsco: Float? = 23f
        @JvmField
        var suino = 666 to "fetish"

        class Buddhah {
            @JvmField
            var ew = "sdfg"
            val primo: Int? = 234
        }

        companion object {
            @JvmStatic
            val kree = Buddhah()
        }
    }

    @Test
    fun methodsTest() {
        val instance = ModelTest::class.java.instantiate()
        assertEquals(instance.susu, instance.fieldValue<String>("susu"))
    }

    @Test
    fun duplicateTest() {
        val instance = ModelTest::class.java.instantiate()
        assertEquals(instance, instance.cloneNative())
    }

    @Test
    fun superTest() {
        val subClass = SubClass()
        from
        subClass.jesus
        to
        from
        subClass.jesus
        to
        from
        subClass.jesus
        to

        assertEquals(78, subClass.jesus)
        from
        subClass.jesus = 15
        to
        from
        subClass.jesus = 16
        to
        assertEquals(16, subClass.jesus)
        assertEquals(16, subClass.getN())
    }

    open class SuperClass {
        private val jesus = 78

        fun getN() = jesus
    }

    class SubClass : SuperClass() {
        //        private val jesus = super.jesus
        var jesus: Int by internalField()
    }

    val jees: Int by lazy {
        e++
    }

    class ModelTest(@JvmField val i: Int) {
        @JvmField
        val susu = "Hello"
        private val private = 345

        constructor() : this(3)
        internal constructor(int: Int, float: Float) : this(int)
        private constructor(int: Int, double: Double) : this(int)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ModelTest

            if (i != other.i) return false
            if (susu != other.susu) return false
            if (private != other.private) return false

            return true
        }

        override fun hashCode(): Int {
            var result = i
            result = 31 * result + susu.hashCode()
            result = 31 * result + private
            return result
        }
    }

    class ModelTest2(s: String) {
        val i: Int

        init {
            i = 43
        }
    }
}

var e = 1

var start = 0L
inline val from: Unit
    get() {
        start = System.nanoTime()
    }
inline val to: Unit
    get() {
        System.out.println("Testing performance -> ${(System.nanoTime() - start) / 1000000f}ms")
    }
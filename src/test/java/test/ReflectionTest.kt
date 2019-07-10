@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")

package test

import org.junit.Assert.assertEquals
import org.junit.Test
import sliep.jes.serializer.*
import java.lang.reflect.Modifier

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
        val newInstance = ModelTest::class.java.newInstanceNative()
        assertEquals("Hello", newInstance.susu)
        assertEquals(3, ModelTest::class.java.declaredConstructors.filter(Modifier.PUBLIC).size)
        assertEquals(5, ModelTest::class.java.newInstanceNative(5).i)
        assertEquals(0, ModelTest::class.java.newUnsafeInstance().i)
    }

    @Test
    fun fieldsTest() {
        val instance = ModelTest::class.java.newInstanceNative()
        assertEquals(instance.susu, instance.getFieldValueNative<String>("susu"))
        assertEquals(instance.susu, instance::class.java.getFieldNative("susu")[instance])
    }

    @Test
    fun methodsTest() {
        val instance = ModelTest::class.java.newInstanceNative()
        assertEquals(instance.susu, instance.getFieldValueNative<String>("susu"))
    }

    @Test
    fun duplicateTest() {
        val instance = ModelTest::class.java.newInstanceNative()
        assertEquals(instance, instance.cloneInstance())
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
        var jesus: Int by Super<SuperClass, Int>()
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
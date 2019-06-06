@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")

package test

import org.junit.Assert.assertEquals
import org.junit.Test
import sliep.jes.serializer.*
import java.lang.reflect.Modifier

class ReflectionTest {

    @Test
    fun constructorTest() {
        val newInstance = constructor<ModelTest>().newInstance()
        assertEquals("Hello", newInstance.susu)
        assertEquals(4, constructors<ModelTest>().size)
        assertEquals(3, constructors<ModelTest>(Modifier.PUBLIC).size)
        assertEquals(1, constructors<ModelTest>(Modifier.PRIVATE).size)
        assertEquals(5, newInstance<ModelTest>(5).i)
        assertEquals(3, newInstance<ModelTest>().i)
        assertEquals(0, newUnsafeInstance<ModelTest2>().i)
    }

    @Test
    fun fieldsTest() {
        val instance = constructor<ModelTest>().newInstance()
        assertEquals(instance.susu, instance.getInstanceField<ModelTest, String>("susu"))
        assertEquals(instance.susu, field<ModelTest>("susu")[instance])
        assertEquals(1, fields<ModelTest>(modifiers = Modifier.PRIVATE).size)
        assertEquals(2, fields<ModelTest>(excludeModifiers = Modifier.PRIVATE).size)
    }

    @Test
    fun methodsTest() {
        val instance = constructor<ModelTest>().newInstance()
        assertEquals(instance.susu, instance.getInstanceField<ModelTest, String>("susu"))
        assertEquals(instance.susu, field<ModelTest>("susu")[instance])
        assertEquals(1, fields<ModelTest>(Modifier.PRIVATE).size)
    }

    @Test
    fun duplicateTest() {
        val instance = constructor<ModelTest>().newInstance()
        assertEquals(instance, instance.duplicate())
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
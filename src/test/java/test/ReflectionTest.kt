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
        assertEquals(instance.susu, instance.field<String>("susu"))
        assertEquals(instance.susu, fieldR<ModelTest>("susu")[instance])
        assertEquals(1, fields<ModelTest>(modifiers = Modifier.PRIVATE).size)
        assertEquals(2, fields<ModelTest>(excludeModifiers = Modifier.PRIVATE).size)
    }

    @Test
    fun methodsTest() {
        val instance = constructor<ModelTest>().newInstance()
        assertEquals(instance.susu, instance.field<String>("susu"))
        assertEquals(instance.susu, fieldR<ModelTest>("susu")[instance])
        assertEquals(1, fields<ModelTest>(Modifier.PRIVATE).size)
    }

    class ModelTest(@JvmField val i: Int) {
        @JvmField
        val susu = "Hello"
        private val private = 345

        constructor() : this(3)
        internal constructor(int: Int, float: Float) : this(int)
        private constructor(int: Int, double: Double) : this(int)
    }

    class ModelTest2(s: String) {
        val i: Int

        init {
            i = 43
        }
    }
}
package test

import org.junit.Test
import sliep.jes.serializer.constructor
import sliep.jes.serializer.constructors
import sliep.jes.serializer.newInstance
import sliep.jes.serializer.newUnsafeInstance
import java.lang.reflect.Modifier

class ReflectionTest {

    @Test
    fun constructorTest() {
        val newInstance = constructor<ModelTest>().newInstance()
        assert(newInstance.susu == "Hello")
        assert(constructors<ModelTest>().size == 4)
        assert(constructors<ModelTest>(Modifier.PUBLIC).size == 3)
        assert(constructors<ModelTest>(Modifier.PRIVATE).size == 1)
        assert(newInstance<ModelTest>(5).i == 5)
        assert(newInstance<ModelTest>().i == 3)
        assert(newUnsafeInstance<ModelTest2>().i == 0)
    }

    class ModelTest(val i: Int) {
        val susu = "Hello"

        constructor() : this(3)
        internal constructor(int: Int, float: Float) : this(int)
        private constructor(int: Int, double: Double) : this(int)
    }

    class ModelTest2 {
        val i: Int

        constructor(s: String) {
            i = 43
        }
    }
}
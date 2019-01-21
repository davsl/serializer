import sliep.jes.serializer.JesObject
import sliep.jes.serializer.invokeMethod

fun main(vararg args: String) {
    val hello = "*****Hello*****"
    System.out.println(hello.invokeMethod("substring", 5, 10))
}

class MyObj : JesObject {
    val var1: Int = 3
    val var2: String = "Hello"
    val var3: MyObj2 = MyObj2()
    override fun equals(other: Any?) = other is MyObj && var1 == other.var1 && var2 == other.var2 && var3 == other.var3
}

class MyObj2 : JesObject {
    val var1: Int = 5
    val var2: String = "World"
    override fun equals(other: Any?) = other is MyObj2 && var1 == other.var1 && var2 == other.var2
}
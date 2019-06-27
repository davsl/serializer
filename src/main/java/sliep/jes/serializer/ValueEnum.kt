package sliep.jes.serializer

/**
 * Value enum is a simple interface that will simplify your life: it lets you automatically convert a int value into an enum.
 *
 * E.g. json -> ```json { "myValue"=2 } ```
 * enum -> ```kotlin enum class MyEnumClass(override val value: Int) : ValueEnum {
 *           A(1),
 *           B(2),
 *           C(3)
 *         }
 *         ```
 * dto  -> ```kotlin data class MyResponse(val myValue: MyEnumClass) : JesObject ```
 */
interface ValueEnum {
    val value: Int
}
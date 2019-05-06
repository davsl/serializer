package sliep.jes.serializer

/**
 * Let your class implement [JesObjectImpl] instead of [JesObject] to control it's serialization
 *
 * Implementing the method [toJson] you convert the instance into a value of type [T]
 *
 * A [JesObjectImpl] instance must provide a constructor having [JesConstructor]<[T]> as only parameter to be deserialized as well
 *
 * Example of implementation:
 *```kotlin
 * class MyClass(val a: Int, val b: String) : JesObjectImpl<String> {
 *     constructor(jes: JesConstructor<String>) : this(0, jes.data)
 *
 *     override fun toJson() = b
 * }
 *```
 * @author sliep
 * @param T type of Json field in which the object will be serialized
 * @see JesObject
 */
interface JesObjectImpl<T : Any> : JesObject {
    /**
     * @return if this object is a property of a json object, this method can even return a primitive value or a String, else only a JSONObject or a JSONArray
     * @author sliep
     */
    fun toJson(): T
}
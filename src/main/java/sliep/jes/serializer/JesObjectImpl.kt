package sliep.jes.serializer

/**
 * @author sliep
 * Your JesObjectImpl class must be instantiable from it's JesConstructor
 * @param T type of Json field from which the object will be deserialized
 * @property data source from json
 * @see JesObjectImpl
 */
interface JesConstructor<T : Any> {
    val data: T
}

/**
 * @author sliep
 * Let your class implement [JesObjectImpl] instead of [JesObject] to control it's serialization
 * Implementing the method [toJson] you convert the instance into a value of type [T]
 * A [JesObjectImpl] instance must provide a constructor having [JesConstructor]<[T]> as only parameter to be deserialized as well
 * @param T type of Json field in which the object will be serialized
 * @see JesObject
 * @see JesConstructor
 */
interface JesObjectImpl<T : Any> : JesObject {
    /**
     * @author sliep
     * @return if this object is a property of a json object, this method can even return a primitive value or a String, else only a JSONObject or a JSONArray
     */
    fun toJson(): T
}
@file:Suppress("NOTHING_TO_INLINE", "unused", "UNCHECKED_CAST")

package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONObject
import sliep.jes.reflection.accessor
import kotlin.reflect.KClass

inline fun <reified T : Any> build(block: T.() -> Unit): T {
    val instance = accessor.allocateInstance(T::class.java)
    instance.block()
    return instance
}

inline fun <T> suppress(block: () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        null
    }
}

inline fun <T> suppress(vararg throwable: KClass<out Throwable>, block: () -> T): T? {
    try {
        return block()
    } catch (e: Throwable) {
        for (t in throwable) if (t.java.isInstance(e)) return null
        throw e
    }
}

inline infix fun Int.includes(flag: Int) = this and flag == flag
inline infix fun Int.excludes(flag: Int) = this and flag == 0

infix fun <T : ValueEnum> Class<*>.vEnum(id: Int): T {
    for (value in (this as Class<T>).enumConstants) if (value.value == id) return value
    throw IllegalArgumentException("No enum value for: $id in $this")
}

infix fun <T : Enum<*>> Class<*>.enum(name: String): T {
    for (value in (this as Class<T>).enumConstants) if ((value as Enum<*>).name == name) return value
    throw IllegalArgumentException("No enum name for: $name in $this")
}

fun <T> Number.toDynamic(clazz: Class<T>): T = when ((clazz as Class<*>).kotlin) {
    Int::class -> toInt() as T
    Float::class -> toFloat() as T
    Double::class -> toDouble() as T
    Long::class -> toLong() as T
    Byte::class -> toByte() as T
    Short::class -> toShort() as T
    Char::class -> toChar() as T
    String::class -> toString() as T
    Boolean::class -> (toInt() != 0) as T
    else -> throw IllegalArgumentException("Can't convert a number to an instance of type $clazz")
}

fun <T> String.toDynamic(clazz: Class<T>): T = when ((clazz as Class<*>).kotlin) {
    Int::class -> toInt() as T
    Float::class -> toFloat() as T
    Double::class -> toDouble() as T
    Long::class -> toLong() as T
    Byte::class -> toByte() as T
    Short::class -> toShort() as T
    Char::class -> first() as T
    String::class -> this as T
    Boolean::class -> toBoolean() as T
    else -> throw IllegalArgumentException("Can't convert a number to an instance of type $clazz")
}

inline fun <reified T : JesObject> JSONObject.fromJson(target: T? = null) = fromJson(T::class.java, target)
inline fun <reified T : JesObject> JSONArray.fromJson(target: Array<T>? = null) = fromJson(T::class.java, target)

inline fun <T : JesObject> JSONObject.fromJson(type: Class<T>, target: T? = null): T =
    if (target == null) Deserializer().objectValue(this, type) as T
    else Deserializer().objectValue(this, type, target) as T

inline fun <T : JesObject> JSONArray.fromJson(type: Class<T>, target: Array<T>? = null): Array<T> =
    if (target == null) Deserializer().objectValue(this, type) as Array<T>
    else Deserializer().objectValue(this, type, target) as Array<T>

fun toGenericJson(any: Any): Any = when (any) {
    is JesObject -> any.toJson()
    is Map<*, *> -> any.toJson()
    is Array<*> -> any.toJson()
    is List<*> -> any.toJson()
    is JSONObject, is JSONArray -> any
    else -> throw NonJesObjectException(any::class.java)
}

inline fun JesObject.toJson(): JSONObject = Serializer().jsonValue(this) as JSONObject
inline fun Map<*, *>.toJson(): JSONObject = Serializer().jsonValue(this) as JSONObject
inline fun Array<*>.toJson(): JSONArray = Serializer().jsonValue(this) as JSONArray
inline fun List<*>.toJson(): JSONArray = Serializer().jsonValue(this) as JSONArray

inline fun <reified T : Any> JSONArray.toTypedArray(): Array<T> = Array(length()) { i -> opt(i) as T }

fun String.tryAsJSON(): Any? {
    suppress { return JSONObject(this) }
    suppress { return JSONArray(this) }
    return null
}
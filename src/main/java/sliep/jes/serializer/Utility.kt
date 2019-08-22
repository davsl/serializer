@file:Suppress("unused")

package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import kotlin.reflect.KClass

/**
 * Add element to list if not contained. Useful to avoid duplicates
 * @author sliep
 * @receiver list
 * @param value
 */
fun <T> ArrayList<T>.addIfNotContained(value: T) {
    if (!contains(value)) add(value)
}

/**
 * Only capitalize first character of a string
 * @author sliep
 * @receiver content
 * @return Content
 */
fun String.capitalizeFirst() = if (isEmpty()) this else this[0].toUpperCase() + substring(1)

/**
 * Try to convert string to [JSONObject] or [JSONArray], if conversion fails return null
 * @author sliep
 * @receiver content
 * @return json instance or null
 */
fun String.tryAsJSON(): Any? {
    suppress { return JSONObject(this) }
    suppress { return JSONArray(this) }
    return null
}

/**
 * Check if flag is contained in int
 * @author sliep
 * @receiver flags
 * @param flag to check
 * @return if flag is contained
 */
infix fun Int.includes(flag: Int) = this and flag == flag

/**
 * Check if flag is not contained in int
 * @author sliep
 * @receiver flags
 * @param flag to check
 * @return if flag is not contained
 */
infix fun Int.excludes(flag: Int) = this and flag == 0

inline val Int.sec get() = this * 1000
inline val Int.min get() = this * 60000
inline val Int.hour get() = this * 3600000

inline val Long.sec get() = this * 1000
inline val Long.min get() = this * 60000
inline val Long.hour get() = this * 3600000

inline val Double.sec get() = (this * 1000).toLong()
inline val Double.min get() = (this * 60000).toLong()
inline val Double.hour get() = (this * 3600000).toLong()

inline val Int.day get() = this.toLong() * 86400000
inline val Int.week get() = this.toLong() * 604800000
inline val Int.year get() = this.toLong() * 220752000000

/**
 * Return md5 as string.
 *
 * @receiver source string
 * @return md5 encrypted source
 */
fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    md.update(toByteArray())
    val hash = BigInteger(1, md.digest()).toString(16)
    return StringBuilder(hash).apply {
        while (length < 32) insert(0, "0")
    }.toString()
}


@Suppress("UNCHECKED_CAST")
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

@Suppress("UNCHECKED_CAST")
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

fun Throwable.traceToString(): String = StringWriter().use { writer ->
    PrintWriter(writer).use {
        printStackTrace(it)
        return writer.toString()
    }
}

inline fun <reified T> Array<T>.remove(element: T): Array<T> {
    val indexOf = indexOf(element)
    return if (indexOf == -1) this
    else removeAt(indexOf)
}

inline fun <reified T> Array<T>.removeAt(index: Int): Array<T> {
    if (index >= size) throw IndexOutOfBoundsException("Index: $index, Size: $size")
    if (index < 0) throw NegativeArraySizeException()
    return Array(size - 1) { i ->
        when {
            i >= index -> this@removeAt[i + 1]
            else -> this@removeAt[i]
        }
    }
}

inline fun <reified T> Array<T>.add(element: T, index: Int = size): Array<T> {
    if (index > size) throw IndexOutOfBoundsException("Index: $index, Size: $size")
    if (index < 0) throw NegativeArraySizeException()
    return Array(size + 1) { i ->
        when {
            i < index -> this@add[i]
            i > index -> this@add[i - 1]
            else -> element
        }
    }
}

fun <T : ValueEnum> Class<T>.fromId(id: Int): T {
    for (value in enumConstants) if (value.value == id) return value
    throw IllegalArgumentException("No enum value for: $id in $this")
}

inline fun <reified T : Any> build(block: T.() -> Unit) = T::class.java.newUnsafeInstance().apply(block)

inline fun suppress(vararg throwable: KClass<out Throwable>, block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        if (throwable.isEmpty()) return
        for (t in throwable) if (t.java.isInstance(e)) return
        throw e
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> Collection<*>.toTypedArray(type: Class<T>): Array<T> {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    val thisCollection = this as java.util.Collection<T>
    val component = java.lang.reflect.Array.newInstance(type, 0) as Array<T>
    return thisCollection.toArray(component)
}
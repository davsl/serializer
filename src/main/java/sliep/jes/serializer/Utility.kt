@file:Suppress("unused", "UNCHECKED_CAST")

package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

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

inline val Number.sec get() = toLong() * 1000
inline val Number.min get() = toLong() * 60000
inline val Number.hour get() = toLong() * 3600000

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

fun Throwable.traceToString(): String = StringWriter().use { writer ->
    PrintWriter(writer).use {
        printStackTrace(it)
        return writer.toString()
    }
}

fun <T : ValueEnum> Class<T>.fromId(id: Int): T {
    for (value in enumConstants) if (value.value == id) return value
    throw IllegalArgumentException("No enum value for: $id in $this")
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun <T> Collection<*>.toTypedArray(type: Class<T>): Array<T> =
    (this as java.util.Collection<T>).toArray(type.instantiateArray(0))

class Flags(var flags: Int = 0) {
    infix fun includes(flag: Int) = (flags and flag) == flag
    infix fun excludes(flag: Int) = (flags and flag) == 0

    operator fun set(flag: Int, enabled: Boolean) {
        if (enabled) plusAssign(flag)
        else minusAssign(flag)
    }

    operator fun get(flag: Int) = includes(flag)

    operator fun plusAssign(flag: Int) {
        flags = flags or flag
    }

    operator fun minusAssign(flag: Int) {
        flags = flags and flag.inv()
    }
}

inline val Class<*>.Companion: Any get() = staticFieldValue("Companion")

fun Array<*>?.differsFrom(o2: Array<*>?): Boolean {
    if (this === o2) return false
    if (this.isNullOrEmpty()) return !o2.isNullOrEmpty()
    if (o2.isNullOrEmpty()) return true
    val length = this.size
    if (o2.size != length) return true
    for (i in 0 until length) if (!(if (this[i] == null) o2[i] == null else this[i] == o2[i])) return true
    return false
}

fun <T, R> Array<T>.search(block: (T, Int) -> R?): R {
    for (i in 0 until size) return block(this[i], i) ?: continue
    throw IllegalStateException("Item not found")
}

fun <T, R> Collection<T>.search(block: (T, Int) -> R?): R {
    for ((i, item) in this.withIndex()) return block(item, i) ?: continue
    throw IllegalStateException("Item not found")
}

inline fun <reified T> Array<T>.remove(index: Int) = Array(size - 1) { i ->
    if (i < index) this[i]
    else this[i + 1]
}

inline fun <reified T> Array<T>.add(index: Int = size, newItem: T) = Array(size + 1) { i ->
    when {
        i < index -> this[i]
        i == index -> newItem
        else -> this[i - 1]
    }
}
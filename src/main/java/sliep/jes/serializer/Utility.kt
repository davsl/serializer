@file:Suppress("unused")

import org.json.JSONArray
import org.json.JSONObject
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

fun <T> List<T>.having(comparison: T.() -> Boolean): T? {
    for (elem in this) if (elem.comparison()) return elem
    return null
}

fun <T> Array<T>.having(comparison: T.() -> Boolean): T? {
    for (elem in this) if (elem.comparison()) return elem
    return null
}

/**
 * Only capitalize first character of a string
 * @author sliep
 * @receiver content
 * @return Content
 */
fun String.capitalizeFirst() = this[0].toUpperCase() + substring(1)

/**
 * Try to convert string to [JSONObject] or [JSONArray], if conversion fails return null
 * @author sliep
 * @receiver content
 * @return json instance or null
 */
fun String.tryAsJSON(): Any? {
    kotlin.runCatching { return JSONObject(this) }
    kotlin.runCatching { return JSONArray(this) }
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
fun <T> Number.toDynamic(clazz: Class<T>): T = when (clazz) {
    Int::class.java -> toInt() as T
    Float::class.java -> toFloat() as T
    Double::class.java -> toDouble() as T
    Long::class.java -> toLong() as T
    Byte::class.java -> toByte() as T
    Short::class.java -> toShort() as T
    Char::class.java -> toChar() as T
    String::class.java -> toString() as T
    else -> throw IllegalArgumentException("Can't convert a number to an instance of type $clazz")
}

@Suppress("UNCHECKED_CAST")
fun <T> String.toDynamic(clazz: Class<T>): T = when (clazz) {
    Int::class.java -> toInt() as T
    Float::class.java -> toFloat() as T
    Double::class.java -> toDouble() as T
    Long::class.java -> toLong() as T
    Byte::class.java -> toByte() as T
    Short::class.java -> toShort() as T
    Char::class.java -> toInt().toChar() as T
    String::class.java -> this as T
    else -> throw IllegalArgumentException("Can't convert a number to an instance of type $clazz")
}
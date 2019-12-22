@file:Suppress("NOTHING_TO_INLINE", "unused", "UNCHECKED_CAST")

package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONObject
import sliep.jes.serializer.Deserializer.objectValueArray
import sliep.jes.serializer.Deserializer.objectValueObject
import sliep.jes.serializer.Serializer.jsonValue

inline fun <reified T> JSONObject.fromJson(target: T? = null): T =
    objectValueObject(this, T::class.java, target) as T

inline fun <reified T> JSONArray.fromJson(target: T? = null): T =
    objectValueArray(this, T::class.java, target) as T

inline fun <T> JSONObject.fromJson(type: Class<T>, target: T? = null): T =
    objectValueObject(this, type, target) as T

inline fun <T> JSONArray.fromJson(type: Class<T>, target: T? = null): T =
    objectValueArray(this, type, target) as T

inline fun Any.toJson(): JSONObject = jsonValue(this) as JSONObject
inline fun Array<*>.toJson(): JSONArray = jsonValue(this) as JSONArray
inline fun Iterable<*>.toJson(): JSONArray = jsonValue(this) as JSONArray

inline fun <reified T : Any> JSONArray.toTypedArray(): Array<T> = Array(length()) { i -> opt(i) as T }

fun String.tryAsJSON(): Any? {
    try {
        return JSONObject(this)
    } catch (ignored: Throwable) {
    }
    try {
        return JSONArray(this)
    } catch (ignored: Throwable) {
    }
    return null
}

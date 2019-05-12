@file:Suppress("UNCHECKED_CAST", "unused")

package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Modifier
/**
 * Welcome to JesSerializer
 *
 * This class does only two operations, but it does it really well :)
 *  - Serialize a JVM instance into a json object
 *  - Deserialize a json object to a JVM instance
 *
 * use [JesObject.toJson] to serialize an object
 *
 * use [JSONObject.fromJson] to deserialize a json
 * @author sliep
 */
object JesSerializer : Loggable {
    override var depth: Int = 0
    override var LOG: Boolean = true
}

fun fromJson(string: String, type: Class<*>): Any = try {
    JSONObject(string).fromJson(type)
} catch (e: JSONException) {
    try {
        JSONArray(string).fromJson(type)
    } catch (e: JSONException) {
        throw JSONException("Not a json string!", e)
    }
}

fun toJson(any: Any): Any = when (any) {
    is Array<*> -> (any as Array<out JesObject>).toJson()
    else -> (any as JesObject).toJson()
}

/**
 * Serialize an object instance into a json object
 * No matter if fields are not accessible
 * A field to be serialized must be a [JesObject] or a primitive type or a String.
 * @author sliep
 * @receiver instance to be serialized
 * @return json object representing serialized class
 * @throws IllegalArgumentException if the instance is a primitive type/array/enum
 */
fun JesObject.toJson(): JSONObject {
    JesSerializer.depth = 0
    JesSerializer.log { "Serializing object $this {" }
    when {
        this::class.isTypePrimitive -> throw IllegalArgumentException("Can't convert primitive value to json!")
        this::class.java.isArray -> throw IllegalArgumentException("Can't convert array to json object!")
        this::class.java.isEnum -> throw IllegalArgumentException("Can't convert enum to json object!")
        else -> {
            val response = jsonValue(this) as JSONObject
            JesSerializer.log { "}" }
            return response
        }
    }
}

/**
 * Serialize an array instance into a json array
 * Convert an [Array] of [JesObject] into a [JSONArray]
 * @author sliep
 * @receiver instance to be serialized
 * @return json object representing serialized class
 */
fun Array<out JesObject>.toJson(): JSONArray {
    JesSerializer.depth = 0
    JesSerializer.log { "Serializing array ${this::class.java.simpleName} [" }
    val response = jsonValue(this) as JSONArray
    JesSerializer.log { "]" }
    return response
}

/**
 * Deserialize a json object into a JVM instance
 * No matter if fields are not accessible
 * Every field present in json object will be loaded into the new instance field (if exists, else will be ignored)
 * Convert a [JSONObject] into an instance of [T]
 * @author sliep
 * @receiver object to be deserialized
 * @param T type of instance
 * @param target to load json into or null to create a new instance
 * @return a new instance of [T] built from json object
 */
inline fun <reified T : Any> JSONObject.fromJson(target: T? = null): T =
    if (target == null) objectValue(this, T::class.java) as T
    else objectValue(this, T::class.java, target) as T

fun <T : Any> JSONObject.fromJson(type: Class<T>, target: T? = null): T =
    if (target == null) objectValue(this, type) as T
    else objectValue(this, type, target) as T

/**
 * Deserialize a json array into an array object of [Class.componentType]
 * Convert a [JSONArray] into an [Array] of [T]
 * @author sliep
 * @receiver object to be deserialized
 * @param T type of instance component
 * @param target to load json into or null to create a new instance
 * @return a new instance of [T] built from json object
 */
inline fun <reified T : Any> JSONArray.fromJson(
    target: Array<T> = java.lang.reflect.Array.newInstance(T::class.java, length()) as Array<T>
): Array<T> = arrayObjectValue(this, T::class.java, target as Array<Any?>) as Array<T>

fun <T : Any> JSONArray.fromJson(
    type: Class<T>, target: Array<T> = java.lang.reflect.Array.newInstance(type, length()) as Array<T>
): Array<T> = arrayObjectValue(this, type, target as Array<Any?>) as Array<T>

/**
 * Create a primitive array from [JSONArray]
 * @author sliep
 */
inline fun <reified T : Any> JSONArray.toArrayOf(): Array<T> {
    val array = java.lang.reflect.Array.newInstance(T::class.java, length()) as Array<T>
    for (i in 0 until array.size) array[i] = opt(i) as T
    return array
}

fun <T : Any> JSONArray.toArrayOf(type: Class<T>): Array<T> {
    val array = java.lang.reflect.Array.newInstance(type, length()) as Array<T>
    for (i in 0 until array.size) array[i] = opt(i) as T
    return array
}

/**
 * Here happens the magic
 * @param instance in input can be
 * - [JesObjectImpl]
 * - [Array]
 * - [Enum]
 * - Primitive type
 * - [Any]
 * @author sliep
 * @param blackList is a list of unique identifier of a JVM instance from [System.identityHashCode] is necessary to keep track of already serialized instances to avoid infinite loop if an object field references itself
 * @return the json value ([JSONObject] or [JSONArray] or [String] or Primitive type)
 */
private fun jsonValue(instance: Any, blackList: ArrayList<Int> = ArrayList()): Any = when {
    instance is JesObjectImpl<*> -> instance.toJson()
    instance is Array<*> -> {
        val response = JSONArray()
        JesSerializer.depth++
        for (i in 0 until instance.size) {
            JesSerializer.log { "$i ->" }
            val element = instance[i]
            response.put(if (element == null) null else jsonValue(element, blackList))
        }
        JesSerializer.depth--
        response
    }
    instance is Enum<*> -> instance.name
    instance::class.isTypePrimitive || instance !is JesObject -> instance
    else -> {
        val hashCode = System.identityHashCode(instance)
        if (blackList.contains(hashCode)) throw IllegalStateException("Duplicate instance reference found: $instance")
        blackList.add(hashCode)
        val response = JSONObject()
        for (field in instance::class.java.fields(0, Modifier.STATIC or Modifier.TRANSIENT)) {
            field.isAccessible = true
            val fieldInstance = field[instance] ?: continue
            JesSerializer.depth++
            JesSerializer.log { "\"${field.name}\": $fieldInstance" }
            response.put(field.name, jsonValue(fieldInstance, blackList))
            JesSerializer.depth--
        }
        response
    }
}

/**
 * Here happens the magic
 * @author sliep
 * @param jes input json value can be
 * - [JSONArray]
 * - [JSONObject]
 * - Primitive type
 * - [String]
 * @param type of the field to be inflated can be
 * - Assignable by [jes]
 * - [Array]
 * - [Enum]
 * - [Any]
 * @return the object value of [jes] (an instance of [type])
 */
fun objectValue(jes: Any, type: Class<*>): Any = when {
    jes::class.java.isAssignableFrom(type) -> {
        JesSerializer.log { "${jes::class.java.simpleName}: $jes" }
        jes
    }
    JesObjectImpl::class.java.isAssignableFrom(type) -> type.newInstance(jes)
    jes is JSONArray -> {
        val componentType = type.componentType
            ?: throw IllegalStateException("Can't deserialize json array into $type: array type expected")
        arrayObjectValue(
            jes, componentType, java.lang.reflect.Array.newInstance(componentType, jes.length()) as Array<Any?>
        )
    }
    type.isEnum -> {
        JesSerializer.log { "Enum: $jes" }
        type.getDeclaredMethod("valueOf", String::class.java).invoke(null, jes.toString())
    }
    jes is JSONObject -> objectValue(jes, type, type.newUnsafeInstance())
    jes is String && type.isAssignableFrom(Char::class.java) -> {
        JesSerializer.log { "Char: $jes" }
        jes[0]
    }
    jes is Double && type.isAssignableFrom(Float::class.java) -> {
        JesSerializer.log { "Float: $jes" }
        jes.toFloat()
    }
    else -> jes
}

/**
 * Load json elements into array instance
 * @author sliep
 * @see objectValue
 */
fun arrayObjectValue(jes: JSONArray, componentType: Class<*>, instance: Array<Any?>): Array<*> {
    JesSerializer.log { "Deserializing JSONArray to array of ${componentType.simpleName} [" }
    JesSerializer.depth++
    for (i in 0 until jes.length()) {
        JesSerializer.log { "$i ->" }
        JesSerializer.depth++
        instance[i] = objectValue(jes.opt(i), componentType)
        JesSerializer.depth--
    }
    JesSerializer.depth--
    JesSerializer.log { "]" }
    return instance
}

/**
 * Load json elements into object instance
 * @author sliep
 * @see objectValue
 */
fun objectValue(jes: JSONObject, type: Class<*>, instance: Any): Any {
    JesSerializer.log { "Deserializing JSONObject to ${type.simpleName}" }
    val keys = jes.keys()
    while (keys.hasNext()) {
        val name = keys.next()
        val field = kotlin.runCatching { type.field(name, true) }.getOrNull() ?: continue
        JesSerializer.depth++
        JesSerializer.log { "${field.name} <- ${jes[name]}" }
        JesSerializer.depth++
        try {
            val value = when {
                field.type.isArray -> {
                    val jsonArray = jes.optJSONArray(name)
                    if (jsonArray == null) null
                    else objectValue(jsonArray, field.type)
                }
                List::class.java.isAssignableFrom(field.type) ->
                    if (jes[name] is List<*>) jes[name]
                    else jes.getJSONArray(name).toList()
                Map::class.java.isAssignableFrom(field.type) ->
                    if (jes[name] is Map<*, *>) jes[name]
                    else jes.getJSONObject(name).toMap()
                else -> {
                    if (jes.isNull(name)) null
                    else objectValue(jes[name], field.type)
                }
            }
            field.isFinal = false
            field[instance] = value
        } catch (e: Throwable) {
            throw JSONException("Unable to deserialize field $name", e)
        }
        JesSerializer.depth -= 2
    }
    return instance
}
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
    override var LOG: Boolean = false
}

fun toGenericJson(any: Any): Any = when (any) {
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
inline fun <reified T : JesObject> JSONObject.fromJson(target: T? = null): T =
    if (target == null) objectValue(this, T::class.java) as T
    else objectValue(this, T::class.java, target) as T

fun <T : JesObject> JSONObject.fromJson(type: Class<T>, target: T? = null): T =
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
inline fun <reified T : JesObject> JSONArray.fromJson(
    target: Array<T> = newArrayInstance(length())
): Array<T> = arrayObjectValue(this, T::class.java, target as Array<Any?>) as Array<T>

fun <T : JesObject> JSONArray.fromJson(
    type: Class<T>, target: Array<T> = type.newArrayInstance(length())
): Array<T> = arrayObjectValue(this, type, target as Array<Any?>) as Array<T>

//TODO deserialize lists. idk how but Gson can do it...
/**
 * Create a primitive array from [JSONArray]
 * @author sliep
 */
inline fun <reified T : Any> JSONArray.toTypedArray(): Array<T> = Array(length()) { i -> opt(i) as T }

fun <T : Any> JSONArray.toTypedArray(type: Class<T>): Array<T> {
    val array = type.newArrayInstance(length())
    for (i in 0 until array.size) array[i] = opt(i) as T
    return array
}

fun JSONArray.toGenericList(): ArrayList<*> {
    val size = length()
    val results = ArrayList<Any?>(size)
    for (i in 0 until size)
        when (val element = opt(i)) {
            null, JSONObject.NULL -> results.add(null)
            is JSONArray -> results.add(element.toGenericList())
            is JSONObject -> results.add(element.toGenericMap())
            else -> results.add(element)
        }
    return results
}

fun JSONObject.toGenericMap(): Map<String, *> {
    val results = HashMap<String, Any?>()
    for (key in keys())
        results[key] = when (val value = opt(key)) {
            null, JSONObject.NULL -> null
            is JSONObject -> value.toGenericMap()
            is JSONArray -> value.toGenericList()
            else -> value
        }
    return results
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
    instance is Enum<*> -> if (instance is ValueEnum) instance.value else instance.name
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
        arrayObjectValue(jes, componentType, componentType.newArrayInstance(jes.length()))
    }
    type.isEnum -> {
        JesSerializer.log { "Enum: $jes" }
        if (ValueEnum::class.java.isAssignableFrom(type)) {
            var result: ValueEnum? = null
            for (value in type.enumConstants) {
                if ((value as ValueEnum).value == jes.toString().toInt()) {
                    result = value
                    break
                }
            }
            result ?: throw IllegalArgumentException("No enum value for: $jes")
        } else
            type.getDeclaredMethod("valueOf", String::class.java).invoke(null, jes.toString())
    }
    jes is JSONObject -> objectValue(jes, type, type.newUnsafeInstance())
    jes is String -> {
        JesSerializer.log { "String: $jes" }
        when {
            String::class.java.isAssignableFrom(type) -> jes
            type.kotlin.isTypePrimitive -> jes.toDynamic(type)
            JSONObject::class.java.isAssignableFrom(type) -> JSONObject(jes)
            JSONArray::class.java.isAssignableFrom(type) -> JSONArray(jes)
            JesObject::class.java.isAssignableFrom(type) -> JSONObject(jes).fromJson(type as Class<out JesObject>)
            type.componentType != null && JesObject::class.java.isAssignableFrom(type.componentType) ->
                JSONArray(jes).fromJson(type.componentType as Class<out JesObject>)
            else -> throw UnsupportedOperationException("Can't convert String to $type")
        }
    }
    jes is Number -> {
        JesSerializer.log { "Number: $jes" }
        jes.toDynamic(type)
    }
    else -> jes
}

/**
 * Load json elements into array instance
 * @author sliep
 * @see objectValue
 */
fun arrayObjectValue(jes: JSONArray, componentType: Class<*>, instance: Array<*>): Array<*> {
    JesSerializer.log { "Deserializing JSONArray to array of ${componentType.simpleName} [" }
    JesSerializer.depth++
    instance as Array<Any?>
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
                jes.isNull(name) -> null
                field.type.isArray -> objectValue(jes.getJSONArray(name), field.type)
                List::class.java.isAssignableFrom(field.type) ->
                    if (jes[name] is List<*>) jes[name]
                    else jes.getJSONArray(name).toGenericList()
                Map::class.java.isAssignableFrom(field.type) ->
                    if (jes[name] is Map<*, *>) jes[name]
                    else jes.getJSONObject(name).toGenericMap()
                else -> objectValue(jes[name], field.type)
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
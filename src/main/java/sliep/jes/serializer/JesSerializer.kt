@file:Suppress("UNCHECKED_CAST", "unused")

package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import sliep.jes.serializer.JesSerializer.toJson
import java.lang.reflect.Array
import java.lang.reflect.Modifier

/**
 * @author sliep
 * Welcome to JesSerializer
 * This class does only two operations, but it does it really well :)
 *  - Serialize a JVM instance into a json object
 *  - Deserialize a json object to a JVM instance
 * use [JesObject.toJson] to serialize an object
 * use [JSONObject.fromJson] to deserialize a json
 * For java boyz instead of method extensions you can call [JesSerializer.toJson], [JesSerializer.arrayToJson], [JesSerializer.fromJson], [JesSerializer.fromJsonArray],
 */
object JesSerializer : Loggable {
    /**
     * @see Loggable
     */
    private var depth = 0
    /**
     * Generate debug log
     * @see Loggable
     */
    @JvmField
    var LOG: Boolean = false

    /**
     * @author sliep
     * Serialize an object instance into a json object
     * No matter if fields are not accessible
     * A field to be serialized must be a [JesObject] or a primitive type or a String.
     * If you want to exclude some field, annotate it as [Transient]
     * @param instance to be serialized
     * @return json object representing serialized class
     * @throws IllegalArgumentException if the instance is a primitive type/array/enum
     */
    @JvmStatic
    fun toJson(instance: JesObject): JSONObject {
        depth = 0
        log { "Serializing object $instance {" }
        when {
            instance::class.isTypePrimitive -> throw IllegalArgumentException("Can't convert primitive value to json!")
            instance::class.java.isArray -> throw IllegalArgumentException("Can't convert array to json object!")
            instance::class.java.isEnum -> throw IllegalArgumentException("Can't convert enum to json object!")
            else -> {
                val response = jsonValue(instance) as JSONObject
                log { "}" }
                return response
            }
        }
    }

    /**
     * @author sliep
     * Serialize an array instance into a json array
     * @throws IllegalArgumentException if the instance is not an array
     * @see toJson
     */
    @JvmStatic
    fun arrayToJson(instance: kotlin.Array<out JesObject>): JSONArray {
        depth = 0
        log {
            val builder = StringBuilder()
            var baseClass = instance::class.java as Class<*>
            while (baseClass.componentType != null) {
                builder.append("[]")
                baseClass = baseClass.componentType
            }
            "Serializing array ${baseClass.name}$builder ["
        }
        val response = when {
            !instance::class.java.isArray -> throw IllegalArgumentException("Instance is not array instance")
            else -> jsonValue(instance) as JSONArray
        }
        log { "]" }
        return response
    }


    /**
     * @author sliep
     * Deserialize a json object into a JVM instance
     * No matter if fields are not accessible
     * Every field present in json object will be loaded into the new instance field (if exists, else will be ignored)
     * @param jes json object input
     * @param type type of instance
     * @return a new instance of [type] built from [jes]
     * @throws IllegalArgumentException if the type is a primitive type/array/enum
     */
    @JvmStatic
    fun <T : Any> fromJson(jes: JSONObject, type: Class<T>): T {
        depth = 0
        log { "Deserializing JSONObject to ${type.name} <- $jes" }
        return when {
            type.isTypePrimitive -> throw IllegalArgumentException("Can't convert json object to primitive value!")
            type.isArray -> throw IllegalArgumentException("Can't convert json object to array!")
            type.isEnum -> throw IllegalArgumentException("Can't convert json object to enum!")
            else -> objectValue(jes, type) as T
        }
    }

    /**
     * @author sliep
     * Deserialize a json array into an array object of [componentType]
     * @see fromJson
     */
    @JvmStatic
    fun <T : Any> fromJsonArray(jes: JSONArray, componentType: Class<T>): kotlin.Array<T> {
        depth = 0
        log { "Deserializing JSONArray to ${componentType.name}[] <- $jes" }
        return objectValue(jes, componentType) as kotlin.Array<T>
    }

    /**
     * @author sliep
     * Here happens the magic
     * @param instance in input can be
     * - [JesObjectImpl]
     * - [Array]
     * - [Enum]
     * - Primitive type
     * - [Any]
     * @param blackList is a list of unique identifier of a JVM instance from [System.identityHashCode] is necessary to keep track of already serialized instances to avoid infinite loop if an object field references itself
     * @return the json value ([JSONObject] or [JSONArray] or [String] or Primitive type)
     */
    @JvmStatic
    private fun jsonValue(instance: Any, blackList: ArrayList<Int> = ArrayList()): Any = when {
        instance is JesObjectImpl<*> -> instance.toJson()
        instance::class.java.isArray -> {
            val response = JSONArray()
            val logArray = LOG && !instance::class.java.componentType.isTypePrimitive
            depth++
            for (i in 0 until Array.getLength(instance)) {
                if (logArray) log { "$i ->" }
                response.put(jsonValue(Array.get(instance, i), blackList))
            }
            depth--
            response
        }
        instance::class.java.isEnum -> (instance as Enum<*>).name
        instance::class.isTypePrimitive || !JesObject::class.java.isAssignableFrom(instance::class.java) -> instance
        else -> {
            val hashCode = System.identityHashCode(instance)
            if (blackList.contains(hashCode)) throw IllegalStateException("Infinite json object parameter: $instance")
            blackList.add(hashCode)
            val response = JSONObject()
            for (field in instance::class.java.fields(0, Modifier.STATIC or Modifier.TRANSIENT)) {
                field.isAccessible = true
                val fieldInstance = field[instance] ?: continue
                val logArray = LOG && fieldInstance::class.java.isArray
                depth++
                log { if (logArray) "\"${field.name}\": [" else "\"${field.name}\": $fieldInstance" }
                if (logArray && fieldInstance::class.java.componentType.isTypePrimitive) {
                    depth++
                    for (i in 0 until Array.getLength(fieldInstance))
                        log { " ${Array.get(fieldInstance, i)}" }
                    depth--
                }
                response.put(field.name, jsonValue(fieldInstance, blackList))
                if (logArray) log { "]" }
                depth--
            }
            response
        }
    }

    /**
     * @author sliep
     * Here happens the magic
     * @param jes input json value can be
     * - [JSONArray]
     * - [JSONObject]
     * - Primitive type
     * - [String]
     * @param type of the field to be inflated can be
     * - Any that has a [JesConstructor]
     * - Assignable by [jes]
     * - [Array]
     * - [Enum]
     * - [Any]
     * @return the object value of [jes] (an instance of [type])
     */
    @JvmStatic
    private fun objectValue(jes: Any, type: Class<*>): Any = when {
        kotlin.runCatching { type.constructor(JesConstructor::class); true }.getOrDefault(false) -> type.constructor(JesConstructor::class).newInstance(object : JesConstructor<Any> {
            override val data = jes
        })
        jes::class.java.isAssignableFrom(type) -> jes
        jes is JSONArray -> {
            val response = Array.newInstance(type, jes.length())
            val logArray = LOG && !type.isTypePrimitive
            if (logArray) log { " [" }
            depth++
            for (i in 0 until jes.length()) {
                if (logArray) log { "$i ->" }
                Array.set(response, i, objectValue(jes.opt(i), type))
            }
            depth--
            if (logArray) log { " ]" }
            response
        }
        type.isEnum && jes is String -> type.getDeclaredMethod("valueOf", String::class.java).invoke(null, jes)
        jes is JSONObject -> {
            val keys = jes.keys()
            val response = type.newUnsafeInstance()
            while (keys.hasNext()) {
                val name = keys.next()
                val field = kotlin.runCatching { type.fieldR(name, true) }.getOrNull() ?: continue
                depth++
                log { " ${field.name} <- ${jes[name]}" }
                field.isAccessible = true
                try {
                    val value = when {
                        field.type.isArray -> objectValue(jes.optJSONArray(name), field.type.componentType)
                        List::class.java.isAssignableFrom(field.type) ->
                            if (jes[name] is List<*>) jes[name] else jes.getJSONArray(name).toList()
                        Map::class.java.isAssignableFrom(field.type) ->
                            if (jes[name] is Map<*, *>) jes[name] else jes.getJSONObject(name).toMap()
                        else -> objectValue(jes[name], field.type)
                    }
                    field.isFinal = false
                    field[response] = value
                } catch (e: Throwable) {
                    throw JSONException("Unable to deserialize field $name", e)
                }
                depth--
            }
            response
        }
        else -> jes
    }
}

/**
 * @author sliep
 * Convert a [JesObject] into a [JSONObject]
 * @receiver instance to be serialized
 * @return json object representing serialized class
 * @see JesSerializer.toJson
 */
fun JesObject.toJson(): JSONObject = JesSerializer.toJson(this)

/**
 * @author sliep
 * Convert an [Array] of [JesObject] into a [JSONArray]
 * @receiver instance to be serialized
 * @return json object representing serialized class
 * @see JesSerializer.arrayToJson
 */
fun kotlin.Array<out JesObject>.toJson(): JSONArray = JesSerializer.arrayToJson(this)

/**
 * @author sliep
 * Convert a [JSONObject] into an instance of [T]
 * @receiver object to be deserialized
 * @param T type of instance
 * @return a new instance of [T] built from json object
 * @see JesSerializer.fromJson
 */
inline fun <reified T : Any> JSONObject.fromJson() = JesSerializer.fromJson(this, T::class.java)

/**
 * @author sliep
 * Convert a [JSONArray] into an [Array] of [T]
 * @receiver object to be deserialized
 * @param T type of instance component
 * @return a new instance of [T] built from json object
 * @see JesSerializer.fromJsonArray
 */
inline fun <reified T : Any> JSONArray.fromJson() = JesSerializer.fromJsonArray(this, T::class.java)
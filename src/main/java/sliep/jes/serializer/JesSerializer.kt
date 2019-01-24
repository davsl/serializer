package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Array
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

@Suppress("unused", "MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
object JesSerializer : Loggable {
    var depth = 0
    var LOG: Boolean = false

    fun resolvePrimitive(type: Class<*>, value: String): Any = when {
        Boolean::class.java.isAssignableFrom(type) -> try {
            value.toBoolean()
        } catch (e: Throwable) {
            value.toInt() > 0
        }
        Byte::class.java.isAssignableFrom(type) -> value.toByte()
        Char::class.java.isAssignableFrom(type) -> value[0]
        Double::class.java.isAssignableFrom(type) -> value.toDouble()
        Float::class.java.isAssignableFrom(type) -> value.toFloat()
        Int::class.java.isAssignableFrom(type) -> value.toInt()
        Long::class.java.isAssignableFrom(type) -> value.toLong()
        Short::class.java.isAssignableFrom(type) -> value.toShort()
        else -> throw UnsupportedOperationException("UNKNOWN PRIMITIVE TYPE: $type")
    }

    fun toJson(instance: JesObject): JSONObject {
        depth = 0
        log(depth) { "Serializing object $instance {" }
        when {
            instance::class.isTypePrimitive -> throw IllegalArgumentException("Can't convert primitive value to json!")
            instance::class.java.isArray -> throw IllegalArgumentException("Can't convert array to json object!")
            instance::class.java.isEnum -> throw IllegalArgumentException("Can't convert enum to json object!")
            else -> {
                val response = jsonValue(instance) as JSONObject
                log(depth) { "}" }
                return response
            }
        }
    }

    fun arrayToJson(instance: kotlin.Array<out JesObject>): JSONArray {
        depth = 0
        log(depth) {
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
        log(depth) { "]" }
        return response
    }

    fun <T : Any> fromJson(jes: JSONObject, type: KClass<T>): T = fromJson(jes, type.java)
    fun <T : Any> fromJson(jes: JSONObject, type: Class<T>): T {
        depth = 0
        log(depth) { "Deserializing JSONObject to ${type.name} <- $jes" }
        return when {
            type.isTypePrimitive -> throw IllegalArgumentException("Can't convert json object to primitive value!")
            type.isArray -> throw IllegalArgumentException("Can't convert json object to array!")
            type.isEnum -> throw IllegalArgumentException("Can't convert json object to enum!")
            else -> objectValue(jes, type) as T
        }
    }

    fun <T : Any> fromJsonArray(jes: JSONArray, componentType: KClass<T>): kotlin.Array<T> = fromJsonArray(jes, componentType.java)
    fun <T : Any> fromJsonArray(jes: JSONArray, componentType: Class<T>): kotlin.Array<T> {
        depth = 0
        log(depth) { "Deserializing JSONArray to ${componentType.name}[] <- $jes" }
        return objectValue(jes, componentType) as kotlin.Array<T>
    }

    private fun jsonValue(instance: Any, blackList: ArrayList<Int> = ArrayList()): Any = when {
        instance::class.java.isArray -> {
            val response = JSONArray()
            val logArray = LOG && !instance::class.java.componentType.isTypePrimitive
            depth++
            for (i in 0 until Array.getLength(instance)) {
                if (logArray) log(depth) { "$i ->" }
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
                log(depth) { if (logArray) "\"${field.name}\": [" else "\"${field.name}\": $fieldInstance" }
                if (logArray && fieldInstance::class.java.componentType.isTypePrimitive) {
                    depth++
                    for (i in 0 until Array.getLength(fieldInstance))
                        log(depth) { " ${Array.get(fieldInstance, i)}" }
                    depth--
                }
                response.put(field.name, jsonValue(fieldInstance, blackList))
                if (logArray) log(depth) { "]" }
                depth--
            }
            response
        }
    }

    private fun objectValue(jes: Any, type: Class<*>): Any = when {
        jes::class.java.isAssignableFrom(type) -> jes
        jes is JSONArray -> {
            val response = Array.newInstance(type, jes.length())
            val logArray = LOG && !type.isTypePrimitive
            if (logArray) log(depth) { " [" }
            depth++
            for (i in 0 until jes.length()) {
                if (logArray) log(depth) { "$i ->" }
                Array.set(response, i, objectValue(jes.opt(i), type))
            }
            depth--
            if (logArray) log(depth) { " ]" }
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
                log(depth) { " ${field.name} <- ${jes[name]}" }
                field.isAccessible = true
                val value =
                        if (field.type.isArray)
                            objectValue(jes.optJSONArray(name), field.type.componentType)
                        else
                            objectValue(jes[name], field.type)
                field.isFinal = false
                field[response] = value
                depth--
            }
            response
        }
        else -> jes
    }
}
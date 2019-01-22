package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@Suppress("unused", "MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
object JesSerializer {

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
        else -> throw java.lang.UnsupportedOperationException("UNKNOWN PRIMITIVE TYPE: $type")
    }

    fun toJson(instance: Any): JSONObject {
        when {
            instance::class.isTypePrimitive -> throw IllegalArgumentException("Can't convert primitive value to json!")
            instance.javaClass.isArray -> throw IllegalArgumentException("Can't convert array to json object!")
            instance.javaClass.isEnum -> throw IllegalArgumentException("Can't convert enum to json object!")
            !JesObject::class.java.isAssignableFrom(instance.javaClass) -> throw IllegalArgumentException("Can't serialize non JesObject instance!")
            else -> return jsonValue(instance) as JSONObject
        }
    }

    fun arrayToJson(instance: Any): JSONArray = when {
        !instance.javaClass.isArray -> throw IllegalArgumentException("Instance is not array instance")
        else -> jsonValue(instance) as JSONArray
    }

    fun <T : Any> fromJson(jes: JSONObject, type: Class<T>): T = when {
        type.isTypePrimitive -> throw IllegalArgumentException("Can't convert json object to primitive value!")
        type.isArray -> throw IllegalArgumentException("Can't convert json object to array!")
        type.isEnum -> throw IllegalArgumentException("Can't convert json object to enum!")
        else -> objectValue(jes, type) as T
    }

    fun <T : Any> fromJsonArray(jes: JSONArray, componentType: Class<T>): kotlin.Array<T> =
            objectValue(jes, componentType) as kotlin.Array<T>

    fun getInstanceFields(clazz: Class<*>): ArrayList<Field> {
        val response = ArrayList<Field>()
        var workingClass = clazz
        while (true) {
            for (field in workingClass.declaredFields)
                if (!Modifier.isTransient(field.modifiers) &&
                        !Modifier.isStatic(field.modifiers) &&
                        !response.contains(field)
                ) response.add(field)
            if (workingClass.superclass == null) return response
            workingClass = workingClass.superclass as Class<*>
        }
    }

    private fun jsonValue(instance: Any, blackList: ArrayList<Int> = ArrayList()): Any = when {
        instance.javaClass.isArray -> {
            val response = JSONArray()
            for (i in 0 until Array.getLength(instance)) response.put(
                    jsonValue(
                            Array.get(
                                    instance,
                                    i
                            ), blackList
                    )
            )
            response
        }
        instance.javaClass.isEnum -> (instance as Enum<*>).name
        instance::class.isTypePrimitive || !JesObject::class.java.isAssignableFrom(instance.javaClass) -> instance
        else -> {
            val hashCode = System.identityHashCode(instance)
            if (blackList.contains(hashCode)) throw IllegalStateException()
            blackList.add(hashCode)
            val response = JSONObject()
            for (field in getInstanceFields(instance.javaClass)) {
                field.isAccessible = true
                val fieldInstance = field.get(instance) ?: continue
                response.put(
                        field.name,
                        jsonValue(fieldInstance, blackList)
                )
            }
            response
        }
    }

    private fun objectValue(jes: Any, type: Class<*>): Any = when {
        jes.javaClass.isAssignableFrom(type) -> jes
        jes is JSONArray -> {
            val response = Array.newInstance(type, jes.length())
            for (i in 0 until jes.length())
                Array.set(response, i, objectValue(jes.opt(i), type))
            response
        }
        type.isEnum && jes is String -> type.getDeclaredMethod("valueOf", String::class.java).invoke(null, jes)
        jes is JSONObject -> {
            val keys = jes.keys()
            val response = type.newUnsafeInstance()
            while (keys.hasNext()) {
                val name = keys.next()
                val field = kotlin.runCatching { type.fieldR(name, true) }.getOrNull() ?: continue
                field.isAccessible = true
                val value =
                        if (field.type.isArray) objectValue(
                                jes.optJSONArray(name),
                                field.type.componentType
                        )
                        else objectValue(jes.get(name), field.type)
                field.isFinal = false
                field.set(response, value)
            }
            response
        }
        else -> jes
    }
}
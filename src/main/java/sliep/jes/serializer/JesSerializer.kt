@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Modifier

fun toGenericJson(any: Any): Any = when (any) {
    is JesObject -> any.toJson()
    is Map<*, *> -> any.toJson()
    is Array<*> -> any.toJson()
    is List<*> -> any.toJson()
    is JSONObject, is JSONArray -> any
    else -> throw NonJesObjectException(any::class.java)
}

fun JesObject.toJson(): JSONObject = JesSerializer().jsonValue(this) as JSONObject
fun Map<*, *>.toJson(): JSONObject = JesSerializer().jsonValue(this) as JSONObject
fun Array<*>.toJson(): JSONArray = JesSerializer().jsonValue(this) as JSONArray
fun List<*>.toJson(): JSONArray = JesSerializer().jsonValue(this) as JSONArray

class JesSerializer {
    private val blackList: ArrayList<Int> = ArrayList()

    @Throws(NonJesObjectException::class)
    fun jsonValue(instance: Any): Any = when {
        instance is JesObjectImpl<*> -> instance.toJson()
        isArrayInstance(instance) -> serializeArray(instance)
        isPrimitiveInstance(instance) -> serializePrimitive(instance)
        isBlackListed(instance) -> throw BlackListedFieldException()
        isObjectInstance(instance) -> serializeObject(instance)
        else -> throw NonJesObjectException(instance::class.java)
    }

    fun isObjectInstance(instance: Any) = instance is JesObject || instance is Map<*, *>

    fun serializeObject(instance: Any): JSONObject = JSONObject().apply {
        when (instance) {
            is JesObject -> {
                var clazz = instance::class.java as Class<*>
                while (true) {
                    for (field in clazz.declaredFields) if (field.modifiers excludes TO_EXCLUDE) {
                        val key = field.getDeclaredAnnotation(JesName::class.java)?.name ?: field.name
                        if (has(key)) continue
                        field.isAccessible = true
                        val value = field[instance] ?: continue
                        val jesDate = field.getDeclaredAnnotation(JesDate::class.java)
                        try {
                            when {
                                jesDate != null -> put(key, formats[jesDate].format(value))
                                else -> put(key, jsonValue(value))
                            }
                        } catch (ignored: NonJesObjectException) {
                        }
                    }
                    clazz = clazz.superclass ?: break
                }
            }
            is Map<*, *> -> for (entry in instance.entries) try {
                entry.value?.let { put(entry.key.toString(), jsonValue(it)) }
            } catch (ignored: NonJesObjectException) {
            }
            else -> throw IllegalArgumentException("Only Array<*> and List<*> are accepted")
        }
    }

    fun isArrayInstance(instance: Any) = instance is Array<*> || instance is List<*>

    fun serializeArray(instance: Any): JSONArray = JSONArray().apply {
        when (instance) {
            is Array<*> -> for (element in instance) element?.let { put(jsonValue(it)) }
            is List<*> -> for (element in instance) element?.let { put(jsonValue(it)) }
            else -> throw IllegalArgumentException("Only Array<*> and List<*> are accepted")
        }
    }

    fun isPrimitiveInstance(instance: Any) =
        instance is String || instance::class.javaPrimitiveType != null || instance is Enum<*>

    fun serializePrimitive(instance: Any): Any = when (instance) {
        is Enum<*> -> if (instance is ValueEnum) instance.value else instance.name
        else -> instance
    }

    fun isBlackListed(instance: Any): Boolean {
        val hashCode = System.identityHashCode(instance)
        if (blackList.contains(hashCode)) return true
        blackList.add(hashCode)
        return false
    }

    class BlackListedFieldException : Exception("Field black listed")

    companion object {
        private const val TO_EXCLUDE = Modifier.STATIC or Modifier.TRANSIENT
    }
}

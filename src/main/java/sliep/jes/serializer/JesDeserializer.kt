@file:Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "unused")

package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Field
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

inline fun <reified T : JesObject> JSONObject.fromJson(target: T? = null) = fromJson(T::class.java, target)
inline fun <reified T : JesObject> JSONArray.fromJson(target: Array<T>? = null) = fromJson(T::class.java, target)

fun <T : JesObject> JSONObject.fromJson(type: Class<T>, target: T? = null): T =
    if (target == null) JesDeserializer().deserializeObject(type, this) as T
    else JesDeserializer().deserializeObject(type, this, target) as T

fun <T : JesObject> JSONArray.fromJson(type: Class<T>, target: Array<T>? = null): Array<T> =
    if (target == null) JesDeserializer().deserializeArray(type, this) as Array<T>
    else JesDeserializer().deserializeArray(type, this, target as Array<Any?>) as Array<T>

inline fun <reified T : Any> JSONArray.toTypedArray(): Array<T> = Array(length()) { i -> opt(i) as T }

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

class JesDeserializer {

    fun objectValue(jes: Any, type: Class<*>): Any = when {
        type.isInstance(jes) -> jes
        JesObjectImpl::class.java.isAssignableFrom(type) -> type.newInstanceNative(jes)
        jes is JSONArray -> deserializeArray(
            type.componentType ?: throw IllegalStateException("Expected array type, found $type"), jes
        )
        type.isEnum -> deserializeEnum(type, jes.toString())
        jes is Number -> jes.toDynamic(type)
        jes is String -> deserializeString(type, jes)
        jes is JSONObject -> deserializeObject(type, jes)
        else -> jes
    }

    fun deserializeEnum(type: Class<*>, jes: String): Any = if (ValueEnum::class.java.isAssignableFrom(type))
        (type as Class<out ValueEnum>).fromId(jes.toInt())
    else
        type.getDeclaredMethod("valueOf", String::class.java).invoke(null, jes)

    fun deserializeArray(
        componentType: Class<*>,
        jes: JSONArray,
        instance: Array<Any?> = java.lang.reflect.Array.newInstance(componentType, jes.length()) as Array<Any?>
    ): Array<*> {
        for (i in 0 until jes.length()) instance[i] = objectValue(jes.opt(i), componentType)
        return instance
    }

    fun deserializeString(type: Class<*>, jes: String): Any = when {
        String::class.java.isAssignableFrom(type) -> jes
        type.kotlin.javaPrimitiveType != null -> jes.toDynamic(type)
        JSONObject::class.java.isAssignableFrom(type) -> JSONObject(jes)
        JSONArray::class.java.isAssignableFrom(type) -> JSONArray(jes)
        JesObject::class.java.isAssignableFrom(type) ->
            deserializeObject(type, JSONObject(jes))
        type.componentType != null && JesObject::class.java.isAssignableFrom(type.componentType!!) ->
            deserializeArray(type.componentType, JSONArray(jes))
        else -> throw IllegalStateException("Can't convert String to $type")
    }

    fun deserializeObject(type: Class<*>, jes: JSONObject, instance: Any = type.newUnsafeInstance()): Any {
        val keys = jes.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (jes.isNull(key)) continue
            val field = try {
                type.getFieldNative { (it.getDeclaredAnnotation(JesName::class.java)?.name ?: it.name) == key }
            } catch (e: NoSuchFieldException) {
                continue
            }
            val fieldType = field.type
            val value = jes[key]
            field[instance] = when {
                fieldType.isInstance(value) -> value
                List::class.java.isAssignableFrom(fieldType) -> deserializeList(jes.getJSONArray(key), field)
                Map::class.java.isAssignableFrom(fieldType) -> deserializeMap(jes.getJSONObject(key), field)
                else -> objectValue(jes[key], field.type)
            }
        }
        return instance
    }

    fun deserializeList(jes: JSONArray, field: Field): List<*> {
        val componentType = field.typeArguments[0] as? Class<*>
            ?: throw IllegalArgumentException("List field ${field.name} has no component type, can't be deserialized")
        if (!field.type.isAssignableFrom(ArrayList::class.java))
            throw UnsupportedOperationException("Field ${field.name}: type ${field.type} is not compatible with ArrayList")
        return ArrayList<Any?>().apply {
            Collections.addAll(this, *deserializeArray(componentType, jes))
        }
    }

    fun deserializeMap(jes: JSONObject, field: Field): Map<*, *> {
        val keyType = field.typeArguments[0] as? Class<*>
            ?: throw IllegalArgumentException("Map field ${field.name} has no key type, can't be deserialized")
        if (keyType.kotlin.javaPrimitiveType == null && !String::class.java.isAssignableFrom(keyType))
            throw IllegalArgumentException("Field ${field.name}: Map key can only be of type String or primitive")
        val valueType = field.typeArguments[1] as? Class<*>
            ?: throw IllegalArgumentException("Map field ${field.name} has no value type, can't be deserialized")
        if (!field.type.isAssignableFrom(HashMap::class.java))
            throw UnsupportedOperationException("Field ${field.name}: type ${field.type} is not compatible with HashMap")
        return HashMap<Any?, Any?>().apply {
            for (key in jes.keySet()) {
                val unWrappedKey = key.toDynamic(keyType)
                this[unWrappedKey] = objectValue(jes[key], valueType)
            }
        }
    }
}
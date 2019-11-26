@file:Suppress("UNCHECKED_CAST")

package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import sliep.jes.reflection.accessor
import sliep.jes.reflection.field
import sliep.jes.reflection.instantiateArray
import sliep.jes.reflection.typeArguments
import sliep.jes.serializer.annotations.JesDate
import sliep.jes.serializer.annotations.JsonName
import sliep.jes.serializer.annotations.Serializer
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@PublishedApi
internal class Deserializer {
    fun objectValue(jes: Any, type: Class<*>, dst: Any? = null): Any = when {
        type.isInstance(jes) -> jes
        jes is JSONArray -> {
            val ct = type.componentType ?: throw IllegalStateException("Expected array type, found $type")
            val instance: Array<Any?> = (dst as Array<Any?>?) ?: (ct as Class<Any?>).instantiateArray(jes.length())
            for (i in instance.indices) instance[i] = objectValue(jes.opt(i), ct)
            instance
        }
        type.isEnum -> {
            val name = jes.toString()
            if (ValueEnum::class.java.isAssignableFrom(type)) type vEnum name.toInt()
            else type enum name
        }
        jes is Number -> jes.toDynamic(type)
        jes is String -> when {
            String::class.java.isAssignableFrom(type) -> jes
            type.kotlin.javaPrimitiveType != null -> jes.toDynamic(type)
            JSONObject::class.java.isAssignableFrom(type) -> JSONObject(jes)
            JSONArray::class.java.isAssignableFrom(type) -> JSONArray(jes)
            type.componentType != null -> objectValue(JSONArray(jes), type)
            else -> objectValue(JSONObject(jes), type)
        }
        jes is JSONObject -> {
            val instance = dst ?: accessor.allocateInstance(type)
            for (key in jes.keys()) {
                if (jes.isNull(key)) continue
                val field = suppress(NoSuchFieldException::class) {
                    type.field {
                        it.modifiers excludes (Modifier.TRANSIENT or Modifier.STATIC) &&
                                (it.getDeclaredAnnotation(JsonName::class.java)?.name ?: it.name) == key
                    }
                } ?: continue
                field[instance] = try {
                    valueFor(field, jes[key])
                } catch (e: Throwable) {
                    throw JSONException("Failed to deserialize field $key", e)
                }
            }
            instance
        }
        else -> jes
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun valueFor(field: Field, value: Any): Any? {
        val fieldType = field.type
        if (fieldType.isInstance(value)) return value
        if (List::class.java.isAssignableFrom(fieldType)) {
            value as JSONArray
            val componentType =
                requireNotNull(field.typeArguments[0] as? Class<*>) { "List field ${field.name} has no component type, can't be deserialized" }
            check(fieldType.isAssignableFrom(ArrayList::class.java)) { "Field ${field.name}: type $fieldType is not compatible with ArrayList" }
            val list = ArrayList<Any?>()
            for (i in 0 until value.length()) list.add(objectValue(value.opt(i), componentType))
            return list
        }
        if (Map::class.java.isAssignableFrom(fieldType)) {
            value as JSONObject
            val keyType =
                requireNotNull(field.typeArguments[0] as? Class<*>) { "Map field ${field.name} has no key type, can't be deserialized" }
            val valueType =
                requireNotNull(field.typeArguments[1] as? Class<*>) { "Map field ${field.name} has no value type, can't be deserialized" }
            check(fieldType.isAssignableFrom(HashMap::class.java)) { "Field ${field.name}: type $fieldType is not compatible with HashMap" }
            val map = HashMap<Any?, Any?>()
            for (key in value.keys()) map[key.toDynamic(keyType)] = objectValue(value[key], valueType)
            return map
        }
        val impl = field.getAnnotation(Serializer::class.java)
        if (impl != null) return Serializer.objectValue(impl, value, fieldType)
        val date = field.getAnnotation(JesDate::class.java)
        if (date != null) return JesDate.objectValue(date, value)
        return objectValue(value, fieldType)
    }
}
package sliep.jes.serializer

import org.json.JSONArray
import org.json.JSONObject
import sliep.jes.reflection.accessor
import sliep.jes.serializer.annotations.JesDate
import sliep.jes.serializer.annotations.JsonName
import sliep.jes.serializer.annotations.Serializer
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@PublishedApi
internal class Serializer {
    private val blackList: ArrayList<Int> = ArrayList()
    @Throws(NonJesObjectException::class)
    fun jsonValue(instance: Any): Any = when {
        instance is Array<*> -> JSONArray()
            .also { array -> for (element in instance) array.put(element?.let { jsonValue(it) }) }
        instance is Iterable<*> -> JSONArray()
            .also { array -> for (element in instance) array.put(element?.let { jsonValue(it) }) }
        instance is Enum<*> -> if (instance is ValueEnum) instance.value else instance.name
        instance is String -> instance
        instance::class.javaPrimitiveType != null -> instance
        instance.jesIsBlackListed -> throw BlackListedFieldException()
        instance is JesObject -> {
            val obj = JSONObject()
            for (field in accessor.fields(instance::class.java)) if (field.modifiers excludes (Modifier.TRANSIENT or Modifier.STATIC)) {
                val key = field.getAnnotation(JsonName::class.java)?.name ?: field.name
                if (obj.has(key)) continue
                val value: Any = field[instance] ?: continue
                suppress(NonJesObjectException::class, BlackListedFieldException::class) {
                    obj.put(key, valueFor(field, value))
                }
            }
            obj
        }
        instance is Map<*, *> -> {
            val obj = JSONObject()
            for (entry in instance.entries) suppress(NonJesObjectException::class) {
                entry.value?.let { obj.put(entry.key.toString(), jsonValue(it)) }
            }
            obj
        }
        else -> throw NonJesObjectException(instance::class.java)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun valueFor(field: Field, value: Any): Any? {
        val impl = field.getAnnotation(Serializer::class.java)
        if (impl != null) return Serializer.jsonValue(impl, value)
        val date = field.getAnnotation(JesDate::class.java)
        if (date != null) return JesDate.jsonValue(date, value)
        return jsonValue(value)
    }

    private inline val Any.jesIsBlackListed: Boolean
        get() {
            val hashCode = System.identityHashCode(this)
            if (blackList.contains(hashCode)) return true
            blackList.add(hashCode)
            return false
        }

    class BlackListedFieldException : Exception("Field black listed")
}
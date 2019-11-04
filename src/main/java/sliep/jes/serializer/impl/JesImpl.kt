package sliep.jes.serializer.impl

import org.json.JSONException
import org.json.JSONObject
import sliep.jes.serializer.NonJesObjectException
import sliep.jes.serializer.instantiate
import java.lang.reflect.AccessibleObject
import java.util.*
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class JesImpl(val serializer: KClass<out JesSerializerImpl<*, *>>, vararg val args: String)

interface JesSerializerImpl<JV, OV> {
    @Throws(NonJesObjectException::class)
    fun toJson(value: OV, vararg args: String): JV

    @Throws(JSONException::class)
    fun fromJson(value: JV, type: Class<*>, vararg args: String): OV
}

private val serializers = HashMap<KClass<*>, JesSerializerImpl<*, *>>()

@Suppress("UNCHECKED_CAST")
private operator fun HashMap<KClass<*>, JesSerializerImpl<*, *>>.get(impl: JesImpl): JesSerializerImpl<Any, Any> {
    serializers[impl.serializer]?.let { return it as JesSerializerImpl<Any, Any> }
    val serializer = impl.serializer.java.instantiate()
    serializers[impl.serializer] = serializer
    return serializer as JesSerializerImpl<Any, Any>
}

internal fun JSONObject.putJesImpl(field: AccessibleObject, key: String, value: Any): Boolean {
    val jesImpl = field.getDeclaredAnnotation(JesImpl::class.java) ?: return false
    put(key, serializers[jesImpl].toJson(value, *jesImpl.args))
    return true
}

internal fun JesImpl.objectValue(value: Any, type: Class<*>): Any {
    val serializer = serializers[this]
    return serializer.fromJson(value, type, *args)
}
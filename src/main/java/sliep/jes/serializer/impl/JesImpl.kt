package sliep.jes.serializer.impl

import org.json.JSONObject
import sliep.jes.serializer.newInstanceNative
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class JesImpl(val serializer: KClass<out JesSerializerImpl<*, *>>, vararg val args: String)

private val serializers = HashMap<KClass<*>, JesSerializerImpl<*, *>>()

@Suppress("UNCHECKED_CAST")
private operator fun HashMap<KClass<*>, JesSerializerImpl<*, *>>.get(impl: JesImpl): JesSerializerImpl<Any, Any> {
    serializers[impl.serializer]?.let { return it as JesSerializerImpl<Any, Any> }
    val serializer = impl.serializer.java.newInstanceNative()
    serializers[impl.serializer] = serializer
    return serializer as JesSerializerImpl<Any, Any>
}

internal fun JSONObject.putJesImpl(field: Field, key: String, value: Any): Boolean {
    val jesImpl = field.getDeclaredAnnotation(JesImpl::class.java) ?: return false
    put(key, serializers[jesImpl].toJson(value, *jesImpl.args))
    return true
}

internal fun JesImpl.objectValue(value: Any, type: Class<*>): Any {
    val serializer = serializers[this]
    return serializer.fromJson(value, type, *args)
}
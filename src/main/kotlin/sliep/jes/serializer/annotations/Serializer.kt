@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")

package sliep.jes.serializer.annotations

import sliep.jes.reflection.instantiate
import sliep.jes.serializer.UserSerializer
import java.util.*
import kotlin.collections.set
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Serializer(val serializer: KClass<out UserSerializer<*, *>>) {
    companion object {
        private val serializers = HashMap<KClass<*>, UserSerializer<*, *>>()
        private operator fun HashMap<KClass<*>, UserSerializer<*, *>>.get(impl: Serializer): UserSerializer<Any?, Any?> {
            serializers[impl.serializer]?.let { return it as UserSerializer<Any?, Any?> }
            val serializer = impl.serializer.java.instantiate()
            serializers[impl.serializer] = serializer
            return serializer as UserSerializer<Any?, Any?>
        }

        internal inline fun jsonValue(impl: Serializer, value: Any) = serializers[impl].toJson(value)
        internal inline fun objectValue(impl: Serializer, value: Any, type: Class<*>) =
            serializers[impl].fromJson(value, type)
    }
}

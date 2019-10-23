@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "unused")

package sliep.jes.serializer

import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

//region Field

inline fun <T> linkTo(prop: KProperty0<T>) = LinkDelegate(prop::get, null)
inline fun <T> linkTo(prop: KMutableProperty0<T>) = LinkDelegate(prop::get, prop::set)
inline fun <T> linkTo(noinline getter: (() -> T)?, noinline setter: ((T) -> Unit)? = null) =
    LinkDelegate(getter, setter)

class LinkDelegate<T>(private val getter: (() -> T)?, private val setter: ((T) -> Unit)?) {
    operator fun getValue(receiver: Any, property: KProperty<*>): T =
        if (getter != null) getter.invoke()
        else throw IllegalStateException("Getter was not provided")

    operator fun setValue(receiver: Any, property: KProperty<*>, value: T) =
        if (setter != null) setter.invoke(value)
        else throw IllegalStateException("Setter was not provided")
}

inline fun <reified T : Any, R : Any?> T.internalField(clazz: Class<out T> = T::class.java) = InternalField<T, R>(clazz)

class InternalField<T : Any, R : Any?>(private val clazz: Class<out T>) {
    private var field: Field? = null
    operator fun getValue(thisRef: Any, property: KProperty<*>): R =
        (field ?: clazz.field(property.name).also { field = it }).get(thisRef) as R

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: R) =
        (field ?: clazz.field(property.name).also { field = it }).set(thisRef, value)
}

//endregion
//region Method
inline fun <reified T, R> internalMethod0(): InternalMethod<T.() -> R> = object :
    InternalMethod<T.() -> R>(T::class.java) {
    override fun function(method: Method): T.() -> R = { method.invoke(this) as R }
}

inline fun <reified T, reified P0, R> internalMethod1(): InternalMethod<T.(P0) -> R> = object :
    InternalMethod<T.(P0) -> R>(T::class.java, P0::class.java) {
    override fun function(method: Method): T.(P0) -> R = { p0 -> method.invoke(this, p0) as R }
}

inline fun <reified T, reified P0, reified P1, R> internalMethod2(): InternalMethod<T.(P0, P1) -> R> = object :
    InternalMethod<T.(P0, P1) -> R>(T::class.java, P0::class.java, P1::class.java) {
    override fun function(method: Method): T.(P0, P1) -> R = { p0, p1 -> method.invoke(this, p0, p1) as R }
}

inline fun <reified T, reified P0, reified P1, reified P2, R> internalMethod3(): InternalMethod<T.(P0, P1, P2) -> R> =
    object : InternalMethod<T.(P0, P1, P2) -> R>(T::class.java, P0::class.java, P1::class.java, P2::class.java) {
        override fun function(method: Method): T.(P0, P1, P2) -> R =
            { p0, p1, p2 -> method.invoke(this, p0, p1, p2) as R }
    }

abstract class InternalMethod<F>(private val clazz: Class<*>, private vararg val args: Class<*>) {
    private var method: Method? = null
    protected abstract fun function(method: Method): F
    operator fun getValue(thisRef: Any?, property: KProperty<*>): F =
        function(method ?: clazz.method(property.name, *args).also { method = it })
}
//endregion
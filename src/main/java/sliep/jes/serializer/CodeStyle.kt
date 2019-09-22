@file:Suppress("NOTHING_TO_INLINE", "unused")

package sliep.jes.serializer

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

inline fun <reified T : Any> build(block: T.() -> Unit) = T::class.java.newUnsafeInstance().apply(block)

inline fun <T> suppress(vararg throwable: KClass<out Throwable>, block: () -> T): T? {
    try {
        return block()
    } catch (e: Throwable) {
        if (throwable.isEmpty()) return null
        for (t in throwable) if (t.java.isInstance(e)) return null
        throw e
    }
}

inline val Any?.unit get() = Unit
infix fun Int.includes(flag: Int) = this and flag == flag
infix fun Int.excludes(flag: Int) = this and flag == 0

inline fun <T> linkTo(prop: KProperty0<T>) = LinkDelegate(prop::get, null)
inline fun <T> linkTo(prop: KMutableProperty0<T>) = LinkDelegate(prop::get, prop::set)
inline fun <T> linkTo(noinline getter: (() -> T)?, noinline setter: ((T) -> Unit)? = null) =
    LinkDelegate(getter, setter)

inline fun <reified T : Any, R : Any?> Super() = Super<T, R>(T::class.java)

inline infix fun <T> Any?.then(any: T) = any

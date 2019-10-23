@file:Suppress("NOTHING_TO_INLINE", "unused")

package sliep.jes.serializer

import kotlin.reflect.KClass

inline fun <reified T : Any> build(block: T.() -> Unit) = T::class.java.allocateInstance().apply(block)

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
inline infix fun <T> Any?.then(any: T) = any

infix fun Int.includes(flag: Int) = this and flag == flag
infix fun Int.excludes(flag: Int) = this and flag == 0

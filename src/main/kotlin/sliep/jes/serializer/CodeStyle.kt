@file:Suppress("NOTHING_TO_INLINE", "unused", "UNCHECKED_CAST")

package sliep.jes.serializer

import sliep.jes.reflection.accessor
import kotlin.reflect.KClass

inline fun <reified T : Any> build(block: T.() -> Unit): T {
    val instance = accessor.allocateInstance(T::class.java)
    instance.block()
    return instance
}

inline fun <T> suppress(block: () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        null
    }
}

inline fun <T> suppress(vararg throwable: KClass<out Throwable>, block: () -> T): T? {
    try {
        return block()
    } catch (e: Throwable) {
        for (t in throwable) if (t.java.isInstance(e)) return null
        throw e
    }
}

inline infix fun Int.includes(flag: Int) = this and flag == flag
inline infix fun Int.excludes(flag: Int) = this and flag == 0

infix fun <T : ValueEnum> Class<*>.enumId(id: Int): T {
    for (value in (this as Class<T>).enumConstants) if (value.value == id) return value
    throw IllegalArgumentException("No enum value for: $id in $this")
}

infix fun <T : Enum<*>> Class<*>.enumName(name: String): T {
    for (value in (this as Class<T>).enumConstants) if ((value as Enum<*>).name == name) return value
    throw IllegalArgumentException("No enum name for: $name in $this")
}

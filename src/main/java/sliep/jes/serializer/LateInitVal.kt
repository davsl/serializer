@file:Suppress("UNCHECKED_CAST")

package sliep.jes.serializer

import sliep.jes.serializer.LateInitVal.instances
import kotlin.reflect.KProperty

object LateInitVal : Loggable {
    var LOG = false
    /**
     * All late-initialized instances
     */
    val instances = HashMap<Int, Any?>()
}

/**
 * Allows you to define a final variable (val) that wont be initialized until you access it
 *
 * This variable will be initialized the first time that you access it using your defined initializer, than the value will remain constant for all receiver instance lifeCycle
 * @author sliep
 * @param T type of the variable
 * @param field ::prop
 * @param init initializer of the variable will be called only once
 * @return result of initializer
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> lateInit(field: KProperty<T>, noinline init: () -> T): T {
    val theId = field.hashCode()
    return if (instances.containsKey(theId)) instances[theId] as T else synchronized(LateInitVal) {
        if (!instances.containsKey(theId)) {
            LateInitVal::class.log { "Initializing static property ${field.name} -> $theId" }
            instances[theId] = init()
        }
        instances[theId] as T
    }
}

/**
 * Allows you to define a final variable (val) that wont be initialized until you access it
 *
 * This variable will be initialized the first time that you access it using your defined initializer, than the value will remain constant for all receiver instance lifeCycle
 * @author sliep
 * @receiver instance
 * @param T type of the variable
 * @param field ::prop
 * @param init initializer of the variable will be called only once
 * @return result of initializer
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> Any.lateInit(field: KProperty<*>, noinline init: () -> T): T {
    val theId = System.identityHashCode(this) * field.hashCode()
    return if (instances.containsKey(theId)) instances[theId] as T else {
        LateInitVal::class.log { "Initializing property ${field.name} for instance ${this::class.java.name} -> $theId" }
        val instance = init()
        instances[theId] = instance
        instance
    }
}
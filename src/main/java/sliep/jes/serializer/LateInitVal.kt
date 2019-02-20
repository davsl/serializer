@file:Suppress("UNCHECKED_CAST")

package sliep.jes.serializer

/**
 * Allows you to define a final variable (val) that wont be initialized until you access it
 *
 * e.g. val myVariable get() = lateInit { System.currentTimeMillis() }
 *
 * This variable will be initialized the first time that you access it using your defined initializer, than the value will remain constant for all receiver instance lifeCycle
 * @author sliep
 * @receiver the declaring class instance to prevent the value to be static
 * @param T type of the variable
 * @param init initializer of the variable will be called only once
 * @return result of initializer
 */
fun <T> Any.lateInit(init: () -> T): T {
    val id = getterId(init)
    return if (instances.containsKey(id)) instances[id] as T else {
        val instance = init()
        instances[id] = instance
        instance
    }
}

/**
 * Allows you to define a final variable (val) that wont be initialized until you access it
 *
 * For static properties
 * @author sliep
 * @see [Any.lateInit]
 */
fun <T> lateInit(init: () -> T): T {
    val id = null.getterId(init)
    return if (instances.containsKey(id)) instances[id] as T else {
        val instance = init()
        instances[id] = instance
        instance
    }
}

/**
 * All late-initialized instances
 */
private val instances = HashMap<Int, Any?>()

/**
 * @author sliep
 * @receiver declaring class instance or null for static properties
 * @return unique id of property getter
 */
private fun Any?.getterId(init: Any): Int = if (this != null) System.identityHashCode(this) * System.identityHashCode(init) else System.identityHashCode(init)
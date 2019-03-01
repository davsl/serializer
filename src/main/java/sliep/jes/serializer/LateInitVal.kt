@file:Suppress("UNCHECKED_CAST")

package sliep.jes.serializer

/**
 * Allows you to define a final variable (val) that wont be initialized until you access it
 *
 * This variable will be initialized the first time that you access it using your defined initializer, than the value will remain constant for all receiver instance lifeCycle
 * @author sliep
 * @param T type of the variable
 * @param id UNIQUE id of the value must be constant and unique inside your jvm instance
 * @param init initializer of the variable will be called only once
 * @return result of initializer
 */
fun <T> lateInit(id: Int, init: () -> T) = if (instances.containsKey(id)) instances[id] as T else {
    val instance = init()
    instances[id] = instance
    instance
}

/**
 * @author sliep
 * @receiver instance that contains the variable for static variables
 * @see lateInit
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> lateInit(noinline init: () -> T): T {
    val stack = stack()
    return lateInit((stack.clazz.name + stack.lineNumber).hashCode(), init)
}

/**
 * @author sliep
 * @receiver instance that contains the variable for instance variables
 * @see lateInit
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> Any.lateInit(noinline init: () -> T): T {
    val stack = stack()
    return lateInit((stack.clazz.name + stack.lineNumber).hashCode() * System.identityHashCode(this), init)
}
/**
 * All late-initialized instances
 */
private val instances = HashMap<Int, Any?>()
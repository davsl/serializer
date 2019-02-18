@file:Suppress("UNCHECKED_CAST")

package sliep.jes.serializer

private val instances = HashMap<() -> Any?, Any?>()
private val (() -> Any?).isInitialized: Boolean
    get() = instances.containsKey(this)
private var <T> (() -> T).instance: T
    get() = instances[this] as T
    set(value) {
        instances[this] = value
    }

fun <T> lateInit(init: () -> T): T = if (init.isInitialized) init.instance else {
    val instance = init()
    init.instance = instance
    instance
}
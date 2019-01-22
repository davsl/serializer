package sliep.jes.serializer

abstract class LateInitVal<R : Any> {
    private var initialized = false
    private var instance: R? = null
    fun get(): R? {
        if (!initialized) {
            instance = initialize()
            initialized = true
        }
        return instance
    }

    abstract fun initialize(): R?
}
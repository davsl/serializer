package sliep.jes.serializer

abstract class LateInitVal<R : Any?> {
    private var initialized = false
    private var instance: R? = null
    @Suppress("UNCHECKED_CAST")
    fun get(): R {
        if (!initialized) {
            instance = initialize()
            initialized = true
        }
        return instance as R
    }

    abstract fun initialize(): R
}
package sliep.jes.serializer

interface JesConstructor<T : Any> {
    val data: T
}

interface JesObjectImpl<T : Any> : JesObject {
    fun toJson(): T
}
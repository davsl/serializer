package sliep.jes.serializer.reflect

import sun.misc.Unsafe

@Suppress("UNCHECKED_CAST")
object UnsafeAllocator : ObjectAllocator {
    private val unsafe: Unsafe

    init {
        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        unsafe = unsafeField.get(null) as Unsafe
    }

    override fun <T> allocateInstance(cls: Class<T>): T =
        unsafe.allocateInstance(cls) as T
}
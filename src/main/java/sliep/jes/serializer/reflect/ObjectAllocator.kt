package sliep.jes.serializer.reflect

import sliep.jes.serializer.instantiate

interface ObjectAllocator {
    fun <T> allocateInstance(cls: Class<T>): T

    companion object {
        @JvmStatic
        fun getAllocator(): ObjectAllocator = try {
            @Suppress("UNCHECKED_CAST")
            (Class.forName("sliep.jes.serializer.reflect.NativeAllocator") as Class<ObjectAllocator>)
                .instantiate()
        } catch (e: Throwable) {
            UnsafeAllocator
        }
    }
}
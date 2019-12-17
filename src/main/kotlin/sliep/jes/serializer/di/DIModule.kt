package sliep.jes.serializer.di

interface DIModule {
    @Throws(Throwable::class)
    fun resolve(requester: Any, dependency: Class<*>): Any
}
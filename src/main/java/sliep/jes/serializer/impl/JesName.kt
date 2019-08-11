package sliep.jes.serializer.impl

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class JesName(val name: String)
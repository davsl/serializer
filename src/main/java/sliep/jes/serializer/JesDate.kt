package sliep.jes.serializer

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class JesDate(val format: String, val locale: String = "")
@file:Suppress("NOTHING_TO_INLINE")

package sliep.jes.serializer.annotations

import java.text.SimpleDateFormat
import java.util.*

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class JesDate(val format: String) {
    companion object {
        private val formats = HashMap<String, SimpleDateFormat>()
        private operator fun HashMap<String, SimpleDateFormat>.get(date: JesDate): SimpleDateFormat {
            formats[date.format]?.let { return it }
            val format = SimpleDateFormat(date.format)
            formats[date.format] = format
            return format
        }

        internal inline fun jsonValue(format: JesDate, value: Any): String = formats[format].format(value)
        internal inline fun objectValue(format: JesDate, value: Any): Date = formats[format].parse(value.toString())
    }
}
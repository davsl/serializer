package sliep.jes.serializer.impl

import org.json.JSONObject
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.*

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class JesDate(val format: String, val locale: String = "")

private val formats = HashMap<String, SimpleDateFormat>()

private operator fun HashMap<String, SimpleDateFormat>.get(date: JesDate): SimpleDateFormat {
    formats[date.format]?.let { return it }
    val format = SimpleDateFormat(date.format)
    formats[date.format] = format
    return format
}

internal fun JSONObject.putJesDate(field: Field, key: String, value: Any): Boolean {
    val jesDate = field.getDeclaredAnnotation(JesDate::class.java) ?: return false
    put(key, formats[jesDate].format(value))
    return true
}

internal fun JesDate.objectValue(value: Any): Date = formats[this].parse(value.toString())
package dd

/*
import sliep.jes.serializer.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

fun Any.compress() = if (this is Array<*>) compressArrayInternal().toByteArray() else compressInternal().toByteArray()

inline fun <reified T : Any> ByteArray.decompress(): T = decompress(T::class.java) as T
fun ByteArray.decompress(clazz: Class<*>): Any = if (clazz.isArray) decompressArrayInternal(clazz.componentType) else decompressInternal(clazz)

private fun ByteArray.decompressArrayInternal(componentType: Class<*>): Any {
    val buffer = ByteBuffer.wrap(this)
    val length = buffer.int
    val instance = java.lang.reflect.Array.newInstance(componentType, length)
    for (i in 0 until length) {
        when {
            componentType.isTypePrimitive -> {
                val value = buffer.number
                val fieldClazz = componentType.wrappedClass
                val result: Any = when {
                    Double::class.java.isAssignableFrom(fieldClazz) -> value.toInt()
                    Float::class.java.isAssignableFrom(fieldClazz) -> value.toFloat()
                    Long::class.java.isAssignableFrom(fieldClazz) -> value.toLong()
                    Int::class.java.isAssignableFrom(fieldClazz) -> value.toInt()
                    Short::class.java.isAssignableFrom(fieldClazz) -> value.toShort()
                    Byte::class.java.isAssignableFrom(fieldClazz) -> value.toByte()
                    Char::class.java.isAssignableFrom(fieldClazz) -> value.toChar()
                    Boolean::class.java.isAssignableFrom(fieldClazz) -> value.toInt() == 1
                    else -> throw UnsupportedOperationException()
                }
                java.lang.reflect.Array.set(instance, i, result)
            }
            String::class.java.isAssignableFrom(componentType) -> java.lang.reflect.Array.set(instance, i, string)
            componentType.isArray -> java.lang.reflect.Array.set(instance, i, data.decompressArrayInternal(componentType.componentType))
            else -> java.lang.reflect.Array.set(instance, i, data.decompressInternal(componentType))
        }
    }
    return instance
}

private fun ByteArray.decompressInternal(clazz: Class<*>): Any {
    val instance = clazz.newUnsafeInstance()
    val fields = clazz.fields(Modifier.STATIC or Modifier.TRANSIENT)
    val buffer = ByteBuffer.wrap(this)
    while (buffer.remaining() > 0) {
        val id = buffer.int
        val field = fields.fromId(id)
        field.isAccessible = true
        field.isFinal = false
        when {
            field.type.isTypePrimitive -> {
                val value = buffer.number
                val fieldClazz = field.type.wrappedClass
                val result: Any = when {
                    Double::class.java.isAssignableFrom(fieldClazz) -> value.toInt()
                    Float::class.java.isAssignableFrom(fieldClazz) -> value.toFloat()
                    Long::class.java.isAssignableFrom(fieldClazz) -> value.toLong()
                    Int::class.java.isAssignableFrom(fieldClazz) -> value.toInt()
                    Short::class.java.isAssignableFrom(fieldClazz) -> value.toShort()
                    Byte::class.java.isAssignableFrom(fieldClazz) -> value.toByte()
                    Char::class.java.isAssignableFrom(fieldClazz) -> value.toChar()
                    Boolean::class.java.isAssignableFrom(fieldClazz) -> value.toInt() == 1
                    else -> throw UnsupportedOperationException()
                }
                field[instance] = result
            }
            String::class.java.isAssignableFrom(field.type) -> field[instance] = string
            field.type.isArray -> field[instance] = data.decompressArrayInternal(field.type.componentType)
            else -> field[instance] = data.decompressInternal(field.type)
        }
    }
    return instance
}

private fun Any.compressInternal(): dd.BinaryElement {
    val element = dd.BinaryElement()
    for (key in this::class.fields(0, Modifier.STATIC or Modifier.TRANSIENT)) {
        key.isAccessible = true
        val value = key[this]
        when {
            value::class.isTypePrimitive || value is String -> element[key] = value
            value is Array<*> -> element[key] = value.compressArrayInternal()
            else -> element[key] = value.compressInternal()
        }
    }
    return element
}

private fun Any.compressArrayInternal(): dd.BinaryArrayElement {
    val element = dd.BinaryArrayElement()
    for (i in 0 until java.lang.reflect.Array.getLength(this)) {
        val value = java.lang.reflect.Array.get(this, i)
        when (value) {
            value::class.isTypePrimitive || value is String -> element.put(value)
            value is Array<*> -> element.put(value.compressInternal())
            else -> element.put(value.compressInternal())
        }
    }
    return element
}

private class BinaryElement {
    var length = 0
    val queue = HashMap<Int, Any>()
    /**
     * ANY
     * ID SIZE DATA
     *
     * PRIMITIVE
     * ID TYPE DATA
     */
    operator fun set(field: Field, rawValue: Any) {
        val value = when (rawValue) {
            is Number -> reduceSize
            is Char -> reduceSize
            is Boolean -> if (rawValue) 1.toByte() else 0.toByte()
            else -> rawValue
        } /*NORMALIZE PRIMITIVE*/
        length += Int.SIZE_BYTES /*ID*/ + when (value) {
            is Number -> Byte.SIZE_BYTES /*TYPE*/ + size
            is String -> Int.SIZE_BYTES /*SIZE*/ + value.bytes.size
            is dd.BinaryElement -> Int.SIZE_BYTES /*SIZE*/ + value.length
            is dd.BinaryArrayElement -> Int.SIZE_BYTES /*SIZE*/ + Int.SIZE_BYTES /*LENGTH*/ + value.length
            else -> throw UnsupportedOperationException(value.toString())
        }
        queue[field.name.hashCode()] = value
    }

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(length)
        for (key in queue.keys) {
            val value = queue[key]
            buffer.putInt(key) /*ID*/
            when (value) {
                is Number -> buffer.putNumber(value)
                is String -> putString(value)
                is dd.BinaryElement -> putData(value.toByteArray())
                is dd.BinaryArrayElement -> putData(value.toByteArray())
            }
        }
        return buffer.array()
    }
}

private class BinaryArrayElement {
    var length = 0
    val queue = ArrayList<Any>()
    /**
     * LENGTH [DATA, DATA, DATA]
     */
    fun put(rawValue: Any) {
        val value = when (rawValue) {
            is Number -> reduceSize
            is Char -> reduceSize
            is Boolean -> if (rawValue) 1.toByte() else 0.toByte()
            else -> rawValue
        } /*NORMALIZE PRIMITIVE*/
        length += when (value) {
            is Number -> Byte.SIZE_BYTES /*TYPE*/ + size
            is String -> Int.SIZE_BYTES /*SIZE*/ + value.bytes.size
            is dd.BinaryElement -> Int.SIZE_BYTES /*SIZE*/ + value.length
            is dd.BinaryArrayElement -> Int.SIZE_BYTES /*SIZE*/ + Int.SIZE_BYTES /*LENGTH*/ + value.length
            else -> throw UnsupportedOperationException(value.toString())
        }
        queue.add(value)
    }

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES + length)
        buffer.putInt(queue.size)
        for (value in queue) {
            when (value) {
                is Number -> buffer.putNumber(value)
                is String -> putString(value)
                is dd.BinaryElement -> putData(value.toByteArray())
                is dd.BinaryArrayElement -> putData(value.toByteArray())
            }
        }
        return buffer.array()
    }
}

private const val TYPE_LONG: Byte = 2
private const val TYPE_DOUBLE: Byte = 3
private const val TYPE_FLOAT: Byte = 4
private const val TYPE_INT: Byte = 5
private const val TYPE_SHORT: Byte = 6
private const val TYPE_BYTE: Byte = 7

private val String.bytes: ByteArray
    get() = toByteArray(StandardCharsets.UTF_8)
val ByteBuffer.string: String
    get() = String(data, StandardCharsets.UTF_8)
val ByteBuffer.data: ByteArray
    get() {
        val bytes = ByteArray(int)
        get(bytes)
        return bytes
    }
val ByteBuffer.number: Number
    get() = when (get()) {
        dd.TYPE_LONG -> long
        dd.TYPE_DOUBLE -> double
        dd.TYPE_FLOAT -> float
        dd.TYPE_INT -> int
        dd.TYPE_SHORT -> short
        dd.TYPE_BYTE -> get()
        else -> throw UnsupportedOperationException()
    }

fun ByteBuffer.putData(data: ByteArray) {
    putInt(data.size)
    put(data)
}

fun ByteBuffer.putString(data: String) = putData(data.bytes)

fun ByteBuffer.putNumber(value: Number) {
    put(type)
    when (value) {
        is Long -> putLong(value)
        is Double -> putDouble(value)
        is Float -> putFloat(value)
        is Int -> putInt(value)
        is Short -> putShort(value)
        is Byte -> put(value)
    }
}

fun Array<Field>.fromId(id: Int): Field {
    for (field in this) {
        if (field.name.hashCode() == id) return field
    }
    throw NoSuchFieldException(id.toString())
}

val Number.size: Int
    get() = when (this) {
        is Long -> Long.SIZE_BYTES
        is Double -> java.lang.Double.BYTES
        is Float -> java.lang.Float.BYTES
        is Int -> Int.SIZE_BYTES
        is Short -> Short.SIZE_BYTES
        is Byte -> Byte.SIZE_BYTES
        else -> throw UnsupportedOperationException("not a dd.getNumber")
    }
val Number.type: Byte
    get() = when (this) {
        is Long -> dd.TYPE_LONG
        is Double -> dd.TYPE_DOUBLE
        is Float -> dd.TYPE_FLOAT
        is Int -> dd.TYPE_INT
        is Short -> dd.TYPE_SHORT
        is Byte -> dd.TYPE_BYTE
        else -> throw UnsupportedOperationException("not a dd.getNumber")
    }
val Number.reduceSize: Number
    get() = when (this) {
        is Double -> kotlin.runCatching { reduceSize }
                .getOrElse { if (toString() == toFloat().toString()) toFloat() else this }
        is Float -> kotlin.runCatching { reduceSize }
                .getOrDefault(this)
        is Long -> when {
            this > Byte.MIN_VALUE && this < Byte.MAX_VALUE -> toByte()
            this > Short.MIN_VALUE && this < Short.MAX_VALUE -> toShort()
            this > Int.MIN_VALUE && this < Int.MAX_VALUE -> toInt()
            else -> this
        }
        is Int -> when {
            this > Byte.MIN_VALUE && this < Byte.MAX_VALUE -> toByte()
            this > Short.MIN_VALUE && this < Short.MAX_VALUE -> toShort()
            else -> this
        }
        is Short -> if (this > Byte.MIN_VALUE && this < Byte.MAX_VALUE) toByte() else this
        is Byte -> this
        else -> throw UnsupportedOperationException("not a dd.getNumber")
    }
*/
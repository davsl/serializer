@file:Suppress("UNCHECKED_CAST", "unused", "NOTHING_TO_INLINE")

package sliep.jes.serializer

import sun.misc.Unsafe
import java.lang.reflect.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet

private fun initUnsafe(): Unsafe {
    val unsafeField = unsafeClass.getDeclaredField("theUnsafe")
    unsafeField.isAccessible = true
    return unsafeField.get(null) as Unsafe
}

private val unsafeClass = Unsafe::class.java
private val unsafe = initUnsafe()
private val cachedFields = HashMap<Class<*>, Array<Field>>()
private val cachedAccessors = HashMap<Field, JesAccessor>()
private val cachedMethods = HashMap<Class<*>, Array<Method>>()
private val cachedConstructors = HashMap<Class<*>, Array<Constructor<*>>>()

private inline fun addAll(collection: HashMap<MethodKey, Method>, array: Array<Method>, nonStatic: Boolean = false) {
    for (method in array) {
        if (nonStatic && method.isStatic) continue
        val key = MethodKey(method)
        if (!collection.containsKey(key)) collection[key] = method
    }
}

private class MethodKey internal constructor(method: Method) {
    private val name: String = method.name
    private val paramTypes: Array<Class<*>> = method.parameterTypes

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is MethodKey -> false
        else -> name == other.name && paramTypes.contentEquals(other.paramTypes)
    }

    override fun hashCode(): Int = name.hashCode() + 31 * paramTypes.contentHashCode()
}

private val accessFlagField: Field by lazy {
    try {
        Field::class.java.getFieldNative("modifiers")
    } catch (e: Throwable) {
        Field::class.java.getFieldNative("accessFlags")
    }
}

private fun methodToString(clazz: String, method: String, params: Array<out Class<*>?>): String =
    StringJoiner(", ", "$clazz.$method(", ")").apply {
        for (i in params.indices) add(params[i]?.name.toString())
    }.toString()

private inline val Field.accessor: JesAccessor
    get() {
        cachedAccessors[this]?.let { return it }
        return try {
            if (isStatic) JesAccessorStatic(this) else JesAccessorInstance(this)
        } catch (e: Throwable) {
            FuckOffAccessor(this)
        }.also { cachedAccessors[this] = it }
    }

private typealias FieldGetter = (Any?) -> Any?
private typealias FieldSetter = (Any?, Any?) -> Unit

abstract class JesAccessor(@JvmField val field: Field) {
    init {
        field.isFinal = false
    }

    private val fieldType: Class<*> = field.type
    open val getValue: FieldGetter =
        if (fieldType.isPrimitive) when (fieldType.name) {
            "int" -> ::getInt
            "float" -> ::getFloat
            "double" -> ::getDouble
            "long" -> ::getLong
            "byte" -> ::getByte
            "short" -> ::getShort
            "char" -> ::getChar
            "boolean" -> ::getBoolean
            else -> ::get
        } else ::get
    open val setValue: FieldSetter =
        if (fieldType.isPrimitive) when (fieldType.name) {
            "int" -> ::setInt
            "float" -> ::setFloat
            "double" -> ::setDouble
            "long" -> ::setLong
            "byte" -> ::setByte
            "short" -> ::setShort
            "char" -> ::setChar
            "boolean" -> ::setBoolean
            else -> ::set
        } as FieldSetter else ::set

    protected abstract fun getBoolean(receiver: Any?): Boolean
    protected abstract fun setBoolean(receiver: Any?, value: Boolean)
    protected abstract fun getInt(receiver: Any?): Int
    protected abstract fun setInt(receiver: Any?, value: Int)
    protected abstract fun getFloat(receiver: Any?): Float
    protected abstract fun setFloat(receiver: Any?, value: Float)
    protected abstract fun getDouble(receiver: Any?): Double
    protected abstract fun setDouble(receiver: Any?, value: Double)
    protected abstract fun getLong(receiver: Any?): Long
    protected abstract fun setLong(receiver: Any?, value: Long)
    protected abstract fun getChar(receiver: Any?): Char
    protected abstract fun setChar(receiver: Any?, value: Char)
    protected abstract fun setShort(receiver: Any?, value: Short)
    protected abstract fun getShort(receiver: Any?): Short
    protected abstract fun getByte(receiver: Any?): Byte
    protected abstract fun setByte(receiver: Any?, value: Byte)
    protected abstract fun get(receiver: Any?): Any?
    protected abstract fun set(receiver: Any?, value: Any?)
}

private open class JesAccessorInstance(field: Field) : JesAccessor(field) {
    private var offset: Long = unsafe.objectFieldOffset(field)
    override fun getBoolean(receiver: Any?): Boolean = unsafe.getBoolean(receiver, offset)
    override fun setBoolean(receiver: Any?, value: Boolean) = unsafe.putBoolean(receiver, offset, value)
    override fun getInt(receiver: Any?): Int = unsafe.getInt(receiver, offset)
    override fun setInt(receiver: Any?, value: Int) = unsafe.putInt(receiver, offset, value)
    override fun getFloat(receiver: Any?): Float = unsafe.getFloat(receiver, offset)
    override fun setFloat(receiver: Any?, value: Float) = unsafe.putFloat(receiver, offset, value)
    override fun getDouble(receiver: Any?): Double = unsafe.getDouble(receiver, offset)
    override fun setDouble(receiver: Any?, value: Double) = unsafe.putDouble(receiver, offset, value)
    override fun getLong(receiver: Any?): Long = unsafe.getLong(receiver, offset)
    override fun setLong(receiver: Any?, value: Long) = unsafe.putLong(receiver, offset, value)
    override fun getChar(receiver: Any?): Char = unsafe.getChar(receiver, offset)
    override fun setChar(receiver: Any?, value: Char) = unsafe.putChar(receiver, offset, value)
    override fun setShort(receiver: Any?, value: Short) = unsafe.putShort(receiver, offset, value)
    override fun getShort(receiver: Any?): Short = unsafe.getShort(receiver, offset)
    override fun getByte(receiver: Any?): Byte = unsafe.getByte(receiver, offset)
    override fun setByte(receiver: Any?, value: Byte) = unsafe.putByte(receiver, offset, value)
    override fun get(receiver: Any?): Any? = unsafe.getObject(receiver, offset)
    override fun set(receiver: Any?, value: Any?) = unsafe.putObject(receiver, offset, value)
}

private class FuckOffAccessor(field: Field) : JesAccessor(field) {
    init {
        field.isAccessible = true
    }

    override val getValue: FieldGetter = { receiver -> field.get(receiver) }
    override val setValue: FieldSetter = { receiver, value -> field.set(receiver, value) }

    override fun getBoolean(receiver: Any?): Boolean = throw NotImplementedError()
    override fun setBoolean(receiver: Any?, value: Boolean) = throw NotImplementedError()
    override fun getInt(receiver: Any?): Int = throw NotImplementedError()
    override fun setInt(receiver: Any?, value: Int) = throw NotImplementedError()
    override fun getFloat(receiver: Any?): Float = throw NotImplementedError()
    override fun setFloat(receiver: Any?, value: Float) = throw NotImplementedError()
    override fun getDouble(receiver: Any?): Double = throw NotImplementedError()
    override fun setDouble(receiver: Any?, value: Double) = throw NotImplementedError()
    override fun getLong(receiver: Any?): Long = throw NotImplementedError()
    override fun setLong(receiver: Any?, value: Long) = throw NotImplementedError()
    override fun getChar(receiver: Any?): Char = throw NotImplementedError()
    override fun setChar(receiver: Any?, value: Char) = throw NotImplementedError()
    override fun setShort(receiver: Any?, value: Short) = throw NotImplementedError()
    override fun getShort(receiver: Any?): Short = throw NotImplementedError()
    override fun getByte(receiver: Any?): Byte = throw NotImplementedError()
    override fun setByte(receiver: Any?, value: Byte) = throw NotImplementedError()
    override fun get(receiver: Any?): Any? = throw NotImplementedError()
    override fun set(receiver: Any?, value: Any?) = throw NotImplementedError()
}

private class JesAccessorStatic(field: Field) : JesAccessor(field) {
    private var offset: Long = unsafe.staticFieldOffset(field)
    private var companion: Any = unsafe.staticFieldBase(field)
    override fun getBoolean(receiver: Any?): Boolean = unsafe.getBoolean(companion, offset)
    override fun setBoolean(receiver: Any?, value: Boolean) = unsafe.putBoolean(companion, offset, value)
    override fun getInt(receiver: Any?): Int = unsafe.getInt(companion, offset)
    override fun setInt(receiver: Any?, value: Int) = unsafe.putInt(companion, offset, value)
    override fun getFloat(receiver: Any?): Float = unsafe.getFloat(companion, offset)
    override fun setFloat(receiver: Any?, value: Float) = unsafe.putFloat(companion, offset, value)
    override fun getDouble(receiver: Any?): Double = unsafe.getDouble(companion, offset)
    override fun setDouble(receiver: Any?, value: Double) = unsafe.putDouble(companion, offset, value)
    override fun getLong(receiver: Any?): Long = unsafe.getLong(companion, offset)
    override fun setLong(receiver: Any?, value: Long) = unsafe.putLong(companion, offset, value)
    override fun getChar(receiver: Any?): Char = unsafe.getChar(companion, offset)
    override fun setChar(receiver: Any?, value: Char) = unsafe.putChar(companion, offset, value)
    override fun setShort(receiver: Any?, value: Short) = unsafe.putShort(companion, offset, value)
    override fun getShort(receiver: Any?): Short = unsafe.getShort(companion, offset)
    override fun getByte(receiver: Any?): Byte = unsafe.getByte(companion, offset)
    override fun setByte(receiver: Any?, value: Byte) = unsafe.putByte(companion, offset, value)
    override fun get(receiver: Any?): Any? = unsafe.getObject(companion, offset)
    override fun set(receiver: Any?, value: Any?) = unsafe.putObject(companion, offset, value)
}

inline val Method.isGetter get() = name.startsWith("get") && parameterTypes.isEmpty()
inline val Method.isSetter get() = name.startsWith("set") && parameterTypes.size == 1
inline val Method.propName get() = name[3].toLowerCase() + name.substring(4)
inline val Class<*>.dimensions get() = name.lastIndexOf('[') + 1
inline val Class<*>.isAbstract get() = (modifiers and Modifier.ABSTRACT) != 0
inline val Member.isAbstract get() = (modifiers and Modifier.ABSTRACT) != 0
inline val Member.isPublic get() = (modifiers and Modifier.PUBLIC) != 0
inline val Member.isPrivate get() = (modifiers and Modifier.PRIVATE) != 0
inline val Member.isProtected get() = (modifiers and Modifier.PROTECTED) != 0
inline val Member.isStatic get() = (modifiers and Modifier.STATIC) != 0
inline val Member.isSynchronized get() = (modifiers and Modifier.SYNCHRONIZED) != 0
inline val Member.isVolatile get() = (modifiers and Modifier.VOLATILE) != 0
inline val Member.isTransient get() = (modifiers and Modifier.TRANSIENT) != 0
inline val Member.isNative get() = (modifiers and Modifier.NATIVE) != 0
inline val Member.isInterface get() = (modifiers and Modifier.INTERFACE) != 0
inline val Member.isStrict get() = (modifiers and Modifier.STRICT) != 0
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
inline var Member.isFinal
    get() = (modifiers and Modifier.FINAL) != 0
    set(value) = suppress {
        if (((modifiers and Modifier.FINAL) != 0) != value) accessFlagField
            .setNative(this, if (value) modifiers or Modifier.FINAL else modifiers and Modifier.FINAL.inv())
    }
inline val Executable.signature: String
    get() {
        val methodName = if (this is Constructor<*>) "<init>" else name
        val joiner = StringJoiner(", ", "$methodName(", ")")
        for (param in parameterTypes) joiner.add(param.name)
        return joiner.toString()
    }

inline val Array<*>.types: Array<Class<*>?> get() = Array(size) { i -> this[i]?.let { it::class.java } }

val Class<*>.allFields: Array<Field>
    get() {
        cachedFields[this]?.let { return it }
        val allFields = LinkedHashSet<Field>()
        for (field in declaredFields) allFields.add(field)
        for (iFace in interfaces) for (field in iFace.allFields) allFields.add(field)
        superclass?.let { for (field in it.allFields) allFields.add(field) }
        return allFields.toTypedArray().also { cachedFields[this] = it }
    }
val Class<*>.allMethods: Array<Method>
    get() {
        cachedMethods[this]?.let { return it }
        val allMethods = HashMap<MethodKey, Method>()
        addAll(allMethods, declaredMethods)
        for (iFace in interfaces) addAll(allMethods, iFace.allMethods, true)
        superclass?.let { addAll(allMethods, it.allMethods) }
        return allMethods.values.toTypedArray().also { cachedMethods[this] = it }
    }
val Class<*>.allConstructors: Array<Constructor<*>>
    get() {
        cachedConstructors[this]?.let { return it }
        return declaredConstructors.also { cachedConstructors[this] = it }
    }

inline val Field.typeArguments: Array<Type>
    get() = (genericType as? ParameterizedType)?.actualTypeArguments ?: arrayOf()

fun Executable.isCallableFrom(vararg types: Class<*>?): Boolean {
    val signature = parameterTypes
    if (signature.size != types.size) return false
    for (i in signature.indices) {
        val param = types[i] ?: continue
        if (!signature[i].kotlin.javaObjectType.isAssignableFrom(param.kotlin.javaObjectType)) return false
    }
    return true
}

inline fun <reified T : Any> T.cloneNative(): T = T::class.java.newUnsafeInstance()
    .also { for (field in T::class.java.allFields.filter { !it.isStatic }) suppress { field.copyNative(this, it) } }

inline fun <T> Class<T>.implement(handler: InvocationHandler): T =
    Proxy.newProxyInstance(classLoader, arrayOf(this), handler) as T

inline fun Class<*>.getFieldNative(name: String): Field {
    for (field in allFields) if (field.name == name) return field
    throw NoSuchFieldException(name)
}

inline fun Class<*>.getFieldNative(predicate: (field: Field) -> Boolean): Field {
    for (field in allFields) if (predicate(field)) return field
    throw NoSuchFieldException("Class $name contains no field matching the predicate.")
}

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
inline fun Class<*>.getMethodNative(name: String, vararg paramTypes: Class<*>?): Method {
    for (method in allMethods) if (method.name == name && method.isCallableFrom(*paramTypes)) return method
    throw NoSuchMethodException(methodToString(this.name, name, paramTypes))
}

inline fun Class<*>.getMethodNative(predicate: (method: Method) -> Boolean): Method {
    for (method in allMethods) if (predicate(method)) return method
    throw NoSuchFieldException("Class $name contains no method matching the predicate.")
}

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
inline fun <T> Class<T>.getConstructorNative(vararg paramTypes: Class<*>?): Constructor<T> {
    for (constructor in allConstructors) if (constructor.isCallableFrom(*paramTypes)) return constructor as Constructor<T>
    throw NoSuchMethodException(methodToString(name, "<init>", paramTypes))
}

inline fun <T> Class<T>.newInstanceNative(vararg params: Any?): T {
    check(!isAbstract) { "Cannot allocate abstract class!" }
    return getConstructorNative(*params.types).newInstance(*params)
}

inline fun <T> Class<*>.newArrayInstanceNative(length: Int): Array<T> =
    java.lang.reflect.Array.newInstance(this, length) as Array<T>

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
inline fun <T> Class<T>.newUnsafeInstance(): T {
    check(!isAbstract) { "Cannot allocate abstract class!" }
    return unsafe.allocateInstance(this) as T
}

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
inline fun <R : Any?> Field.getNative(receiver: Any?): R = accessor.getValue(receiver) as R

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
inline fun Field.setNative(receiver: Any?, value: Any?) = accessor.setValue(receiver, value)

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
inline fun Field.copyNative(from: Any?, to: Any?) = accessor.let { it.setValue(to, it.getValue(from)) }

inline fun <R : Any?> Class<*>.getStaticFieldValueNative(name: String): R = getFieldNative(name).getNative(null)
inline fun <R : Any?> Any.getFieldValueNative(name: String): R =
    this::class.java.getFieldNative(name).getNative(this)

inline fun Class<*>.setStaticFieldValueNative(name: String, value: Any?) = getFieldNative(name).setNative(null, value)
inline fun Any.setFieldValueNative(name: String, value: Any?) =
    this::class.java.getFieldNative(name).setNative(this, value)

inline fun <R : Any?> Any.getFieldValueNativeRecursive(name: String): R {
    var result: Any? = this
    for (part in name.split('.')) {
        checkNotNull(result) { "Failed to fetch $name because $part is null" }
        result = result::class.java.getFieldNative(part).getNative(result)
    }
    return result as R
}

inline fun <R> Class<*>.invokeStaticMethodNative(method: String, vararg params: Any?): R =
    getMethodNative(method, *params.types).invoke(null, *params) as R

inline fun <R> Any.invokeMethodNative(method: String, vararg params: Any?): R =
    this::class.java.getMethodNative(method, *params.types).invoke(this, *params) as R

inline fun <R> Any.invokeGetter(fieldName: String): R =
    this::class.java.getMethodNative("get${fieldName.capitalizeFirst()}").invoke(this) as R

inline fun Any.invokeSetter(fieldName: String, value: Any?) {
    this::class.java.getMethodNative("set${fieldName.capitalizeFirst()}", value?.let { it::class.java })
        .invoke(this, value)
}

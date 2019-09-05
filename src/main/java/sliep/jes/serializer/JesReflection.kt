@file:Suppress("UNCHECKED_CAST", "unused", "NOTHING_TO_INLINE")

package sliep.jes.serializer

import sun.misc.Unsafe
import java.lang.reflect.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet

private fun initUnsafe(): Unsafe {
    val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
    unsafeField.isAccessible = true
    return unsafeField.get(null) as Unsafe
}

private val unsafe = initUnsafe()
private val cachedFields = HashMap<Class<*>, Array<Field>>()
private val cachedMethods = HashMap<Class<*>, Array<Method>>()
private val cachedConstructors = HashMap<Class<*>, Array<Constructor<*>>>()

private inline fun addAll(collection: LinkedHashSet<Field>, array: Array<Field>) {
    for (field in array) {
        field.isAccessible = true
        field.isFinal = false
        collection.add(field)
    }
}

private inline fun addAll(collection: HashMap<MethodKey, Method>, array: Array<Method>, nonStatic: Boolean = false) {
    for (method in array) {
        if (nonStatic && method.isStatic) continue
        val key = MethodKey(method)
        if (!collection.containsKey(key)) {
            method.isAccessible = true
            collection[key] = method
        }
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
        Field::class.java.getDeclaredField("modifiers")
    } catch (e: Throwable) {
        Field::class.java.getDeclaredField("accessFlags")
    }.also { it.isAccessible = true }
}

private fun methodToString(clazz: String, method: String, params: Array<out Class<*>?>): String =
    StringJoiner(", ", "$clazz.$method(", ")").apply {
        for (i in params.indices) add(params[i]?.name.toString())
    }.toString()

private typealias FieldGetter = (Any?) -> Any?
private typealias FieldSetter = (Any?, Any?) -> Unit

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
            .set(this, if (value) modifiers or Modifier.FINAL else modifiers and Modifier.FINAL.inv())
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
        addAll(allFields, declaredFields)
        for (iFace in interfaces) addAll(allFields, iFace.allFields)
        superclass?.let { addAll(allFields, it.allFields) }
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
        val allConstructors = declaredConstructors
        for (constructor in allConstructors) constructor.isAccessible = true
        return allConstructors.also { cachedConstructors[this] = it }
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
inline fun Field.copyNative(from: Any?, to: Any?) = set(to, get(from))

inline fun <R : Any?> Class<*>.getStaticFieldValueNative(name: String): R = getFieldNative(name)[null] as R
inline fun <R : Any?> Any.getFieldValueNative(name: String): R =
    this::class.java.getFieldNative(name)[this] as R

inline fun Class<*>.setStaticFieldValueNative(name: String, value: Any?) = getFieldNative(name).set(null, value)
inline fun Any.setFieldValueNative(name: String, value: Any?) =
    this::class.java.getFieldNative(name).set(this, value)

inline fun <R : Any?> Any.getFieldValueNativeRecursive(name: String): R {
    var result: Any? = this
    for (part in name.split('.')) {
        checkNotNull(result) { "Failed to fetch $name because $part is null" }
        result = result::class.java.getFieldNative(part).get(result)
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

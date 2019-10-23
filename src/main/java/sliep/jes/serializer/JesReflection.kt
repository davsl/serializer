@file:Suppress("UNCHECKED_CAST", "unused")

package sliep.jes.serializer

import sliep.jes.serializer.reflect.ObjectAllocator
import java.lang.reflect.*

//region Instance
val Class<*>.allConstructors: Array<Constructor<*>>
    get() {
        cachedConstructors[this]?.let { return it }
        val allConstructors = declaredConstructors
        for (constructor in allConstructors) constructor.isAccessible = true
        return allConstructors.also { cachedConstructors[this] = it }
    }

fun <T> Class<T>.constructor(vararg paramTypes: Class<*>?): Constructor<T> {
    for (constructor in allConstructors) if (constructor.parameterTypes.isCallableFrom(*paramTypes)) return constructor as Constructor<T>
    throw NoSuchMethodException(methodToString(name, "<init>", paramTypes))
}

inline fun <reified T> allocateInstance(): T = T::class.java.allocateInstance()

fun <T> Class<T>.allocateInstance(): T = allocator.allocateInstance(this)

fun <T> Class<T>.instantiate(vararg params: Any?): T =
    constructor(*params.contentTypes).newInstance(*params)

fun <T> Class<*>.instantiateArray(length: Int): Array<T> =
    java.lang.reflect.Array.newInstance(this, length) as Array<T>

fun <T> Class<T>.implement(handler: InvocationHandler): T =
    Proxy.newProxyInstance(classLoader, arrayOf(this), handler) as T

inline fun <reified T : Any> T.cloneNative(): T = allocateInstance<T>().also { instance ->
    for (field in T::class.java.allFields.filter { !Modifier.isStatic(it.modifiers) })
        field.copyTo(this, instance)
}

//endregion
//region Fields
val Class<*>.allFields: Array<Field>
    get() {
        cachedFields[this]?.let { return it }
        val allFields = LinkedHashSet<Field>()
        addAll(allFields, declaredFields)
        for (iFace in interfaces) addAll(allFields, iFace.allFields)
        superclass?.let { addAll(allFields, it.allFields) }
        return allFields.toTypedArray().also { cachedFields[this] = it }
    }
var Field.isFinal
    get() = Modifier.isFinal(modifiers)
    set(value) {
        if (Modifier.isFinal(modifiers) != value) accessFlagField.set(
            this, if (value) modifiers or Modifier.FINAL else modifiers and Modifier.FINAL.inv()
        )
    }
inline val Field.typeArguments: Array<Type>
    get() = (genericType as? ParameterizedType)?.actualTypeArguments ?: arrayOf()

fun Field.copyTo(from: Any?, to: Any?) = set(to, get(from))

fun Class<*>.field(name: String): Field {
    for (field in allFields) if (field.name == name) return field
    throw NoSuchFieldException(name)
}

fun Class<*>.field(predicate: (field: Field) -> Boolean): Field {
    for (field in allFields) if (predicate(field)) return field
    throw NoSuchFieldException("Class $name contains no field matching the predicate.")
}

fun <R : Any?> Class<*>.staticFieldValue(name: String): R = field(name)[null] as R
fun Class<*>.setStaticFieldValue(name: String, value: Any?) = field(name).set(null, value)
fun <R : Any?> Any.fieldValue(name: String): R = this::class.java.field(name)[this] as R
fun Any.setFieldValue(name: String, value: Any?) = this::class.java.field(name).set(this, value)
fun <R : Any?> Any.fieldValue(vararg names: String, nullSafe: Boolean = false): R {
    var result: Any? = this
    for (part in names) {
        if (result == null)
            if (nullSafe) return null as R
            else throw IllegalStateException("$part is null")
        result = result::class.java.field(part)[result]
    }
    return result as R
}

//endregion
//region Methods
inline val Method.isGetter get() = (name.startsWith("get") || name.startsWith("is")) && parameterTypes.isEmpty()
inline val Method.isSetter get() = name.startsWith("set") && parameterTypes.size == 1
inline val Method.propName get() = name[3].toLowerCase() + name.substring(4)
val Class<*>.allMethods: Array<Method>
    get() {
        cachedMethods[this]?.let { return it }
        val allMethods = HashMap<MethodKey, Method>()
        addAll(allMethods, declaredMethods)
        for (iFace in interfaces) addAll(allMethods, iFace.allMethods, true)
        superclass?.let { addAll(allMethods, it.allMethods) }
        return allMethods.values.toTypedArray().also { cachedMethods[this] = it }
    }

fun Class<*>.method(name: String, vararg paramTypes: Class<*>?): Method {
    for (method in allMethods) if (method.name == name && method.parameterTypes.isCallableFrom(*paramTypes)) return method
    throw NoSuchMethodException(methodToString(this.name, name, paramTypes))
}

fun Class<*>.method(predicate: (method: Method) -> Boolean): Method {
    for (method in allMethods) if (predicate(method)) return method
    throw NoSuchFieldException("Class $name contains no method matching the predicate.")
}

fun <R> Class<*>.invokeStatic(method: String, vararg params: Any?): R =
    method(method, *params.contentTypes).invoke(null, *params) as R

fun <R> Any.invoke(method: String, vararg params: Any?): R =
    this::class.java.method(method, *params.contentTypes).invoke(this, *params) as R

fun <R> Any.invokeGetter(field: String): R {
    val name = field[0].toUpperCase() + field.substring(1)
    val names = arrayOf("get$name", "is$name")
    return this::class.java.method { it.name in names }.invoke(this) as R
}

fun Any.invokeSetter(field: String, value: Any?) {
    val name = field[0].toUpperCase() + field.substring(1)
    this::class.java.method("set$name", value?.let { it::class.java }).invoke(this, value)
}

//endregion
//region Internal
inline val Class<*>.dimensions get() = name.lastIndexOf('[') + 1
inline val Array<*>.contentTypes get() = Array(size) { i -> this[i]?.let { it::class.java } }

private val allocator = ObjectAllocator.getAllocator()
private val cachedFields = HashMap<Class<*>, Array<Field>>()
private val cachedMethods = HashMap<Class<*>, Array<Method>>()
private val cachedConstructors = HashMap<Class<*>, Array<Constructor<*>>>()

private fun addAll(collection: LinkedHashSet<Field>, array: Array<Field>) {
    for (field in array) {
        field.isAccessible = true
        field.isFinal = false
        collection.add(field)
    }
}

private fun addAll(
    collection: HashMap<MethodKey, Method>,
    array: Array<Method>,
    nonStatic: Boolean = false
) {
    for (method in array) {
        if (nonStatic && Modifier.isStatic(method.modifiers)) continue
        val key = MethodKey(method)
        if (!collection.containsKey(key)) {
            method.isAccessible = true
            collection[key] = method
        }
    }
}

private fun Array<Class<*>>.isCallableFrom(vararg types: Class<*>?): Boolean {
    if (size != types.size) return false
    for (i in indices) {
        val param = types[i] ?: continue
        if (!this[i].kotlin.javaObjectType.isAssignableFrom(param.kotlin.javaObjectType)) return false
    }
    return true
}

private fun methodToString(clazz: String, method: String, params: Array<out Class<*>?>): String =
    StringBuilder().apply {
        append("$clazz.$method(")
        for (i in params.indices) {
            if (i != 0) append(", ")
            append(params[i]?.name.toString())
        }
        append(")")
    }.toString()

private val accessFlagField: Field by lazy {
    try {
        Field::class.java.getDeclaredField("modifiers")
    } catch (e: Throwable) {
        Field::class.java.getDeclaredField("accessFlags")
    }.also { it.isAccessible = true }
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
//endregion
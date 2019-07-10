@file:Suppress("UNCHECKED_CAST", "unused")

package sliep.jes.serializer

import sun.misc.Unsafe
import java.lang.reflect.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KProperty

val Class<*>.allFields: Array<Field>
    get() {
        cachedFields[this]?.let { return it }
        val allFields = LinkedHashSet<Field>()
        addAll(allFields, privateGetDeclaredFieldsMethod(this, false))
        for (iFace in interfaces) addAll(allFields, iFace.allFields)
        superclass?.let { addAll(allFields, it.allFields) }
        return allFields.toTypedArray().apply { cachedFields[this@allFields] = this }
    }

val Class<*>.allMethods: Array<Method>
    get() {
        cachedMethods[this]?.let { return it }
        val publicMethods = publicMethodsConstructor.newInstance()
        var methods = privateGetDeclaredMethodsMethod(this, false) as Array<Method>
        var methodsSize = methods.size
        var i = 0
        while (i < methodsSize) mergeMethod(publicMethods, methods[i++])
        superclass?.let {
            methods = it.allMethods
            methodsSize = methods.size
            i = 0
            while (i < methodsSize) mergeMethod(publicMethods, methods[i++])
        }
        val interfaces = interfaces
        val interfacesSize = interfaces.size
        i = 0
        while (i < interfacesSize) {
            methods = interfaces[i++].allMethods
            methodsSize = methods.size
            for (j in 0 until methodsSize) {
                val method = methods[j]
                if (!Modifier.isStatic(method.modifiers)) mergeMethod(publicMethods, method)
            }
        }
        return (toArrayMethod(publicMethods) as Array<Method>).apply { cachedMethods[this@allMethods] = this }
    }

fun Class<*>.getFieldNative(name: String): Field {
    val allFields = allFields
    val allFieldsSie = allFields.size - 1
    var i = -1
    while (i < allFieldsSie) if (allFields[++i].name == name) return allFields[i]
    throw NoSuchFieldException(name)
}

fun Class<*>.getFieldNative(filter: (field: Field) -> Boolean): Field {
    val allFields = allFields
    val allFieldsSie = allFields.size - 1
    var i = -1
    while (i < allFieldsSie) if (filter(allFields[++i])) return allFields[i]
    throw NoSuchFieldException(filter.toString())
}

fun Class<*>.getMethodNative(name: String, vararg paramTypes: Class<*>?): Method {
    val allMethods = allMethods
    val allMethodsSie = allMethods.size - 1
    var i = -1
    while (i < allMethodsSie)
        if (allMethods[++i].name == name && checkSignatureParams(allMethods[i].parameterTypes, paramTypes))
            return allMethods[i]
    throw NoSuchMethodException(methodToString(name, paramTypes))
}

fun Class<*>.getMethodNative(filter: (method: Method) -> Boolean): Method {
    val allMethods = allMethods
    val allMethodsSie = allMethods.size - 1
    var i = -1
    while (i < allMethodsSie) if (filter(allMethods[++i])) return allMethods[i]
    throw NoSuchMethodException(filter.toString())
}

fun <T> Class<T>.getConstructorNative(vararg paramTypes: Class<*>?): Constructor<T> {
    val allConstructors = declaredConstructors
    val allConstructorsSize = allConstructors.size - 1
    var i = -1
    while (i < allConstructorsSize)
        if (checkSignatureParams(allConstructors[++i].parameterTypes, paramTypes))
            return allConstructors[i].apply { isAccessible = true } as Constructor<T>
    throw NoSuchMethodException(methodToString("<init>", paramTypes))
}

fun <T> Class<T>.newInstanceNative(vararg params: Any?): T {
    if (Modifier.isAbstract(modifiers)) throw UnsupportedOperationException("Cannot allocate abstract class!")
    return getConstructorNative(*Array(params.size) { j -> params[j]?.let { it::class.java } }).newInstance(*params)
}

fun <T> Class<T>.newArrayInstanceNative(length: Int): Array<T> =
    java.lang.reflect.Array.newInstance(this, length) as Array<T>

fun <T> Class<T>.newUnsafeInstance(): T {
    if (Modifier.isAbstract(modifiers)) throw UnsupportedOperationException("Cannot allocate abstract class!")
    return Unsafe::class.java.getStaticFieldValueNative<Unsafe>("theUnsafe").allocateInstance(this) as T
}

fun <R> Any.getFieldValueNative(name: String): R = this::class.java.getFieldNative(name)[this] as R

fun <R> Class<*>.getStaticFieldValueNative(name: String): R = getFieldNative(name)[null] as R

fun Any.setFieldValueNative(name: String, value: Any?) {
    val field = this::class.java.getFieldNative(name)
    field.isFinal = false
    field[this] = value
}

fun Class<*>.setStaticFieldValueNative(name: String, value: Any?) {
    val field = getFieldNative(name)
    field.isFinal = false
    field[null] = value
}

inline fun <reified T : Member> Array<out T>.filter(includeModifiers: Int? = null, excludeModifiers: Int? = null) =
    filter {
        val modifiers = it.modifiers
        (includeModifiers == null || modifiers includes includeModifiers) && (excludeModifiers == null || modifiers excludes excludeModifiers)
    }.toTypedArray()

val Class<*>.CONSTANTS: HashMap<String, Any?>
    get() {
        val constants = HashMap<String, Any?>()
        allFields.filter(Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL)
            .forEach { constant -> constants[constant.name] = constant[null] }
        return constants
    }

var Field.isFinal: Boolean
    get() = Modifier.isFinal(modifiers)
    set(value) {
        val accessFlags = accessFlagField ?: return
        if (Modifier.isFinal(modifiers) != value)
            accessFlags[this] = modifiers and if (value) Modifier.FINAL else Modifier.FINAL.inv()
    }

val Field.typeArguments: Array<Type>
    get() = (genericType as? ParameterizedType)?.actualTypeArguments ?: arrayOf()

fun <R> Any.invokeMethodNative(fieldName: String, vararg params: Any?): R =
    this::class.java.getMethodNative(fieldName, *Array(params.size) { j -> params[j]?.let { it::class.java } })
        .invoke(this, *params) as R

fun <R> Class<*>.invokeStaticMethodNative(fieldName: String, vararg params: Any?): R =
    this::class.java.getMethodNative(fieldName, *Array(params.size) { j -> params[j]?.let { it::class.java } })
        .invoke(null, *params) as R

fun <R> Any.invokeGetter(fieldName: String): R =
    this::class.java.getMethodNative("get${fieldName.capitalizeFirst()}").invoke(this) as R

fun Any.invokeSetter(fieldName: String, value: Any?) {
    this::class.java.getMethodNative("set${fieldName.capitalizeFirst()}", value?.let { it::class.java })
        .invoke(this, value)
}

val Executable.signature
    get() = declaringClass.methodToString(if (this is Constructor<*>) "<init>" else name, parameterTypes)
val Method.isGetter get() = name.startsWith("get") && parameterTypes.isEmpty()
val Method.isSetter get() = name.startsWith("set") && parameterTypes.size == 1
val Method.propName get() = name[3].toLowerCase() + name.substring(4)
val Class<*>.dimensions get() = name.lastIndexOf('[') + 1


@Suppress("FunctionName")
inline fun <reified T : Any, R : Any?> Super() = Super<T, R>(T::class.java)

class Super<T : Any, R : Any?>(private val clazz: Class<T>) {

    operator fun getValue(thisRef: Any, property: KProperty<*>): R =
        getField(property.hashCode(), property.name).get(thisRef) as R

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: R) =
        getField(property.hashCode(), property.name).set(thisRef, value)

    private fun getField(fieldId: Int, name: String): Field = superFields[fieldId] ?: synchronized(this) {
        superFields[fieldId] ?: clazz.getDeclaredField(name).apply {
            isAccessible = true
            isFinal = false
            superFields[fieldId] = this
        }
    }

    companion object {
        val superFields = HashMap<Int, Field>()
    }
}

inline fun <reified T : Any> T.cloneInstance(): T {
    val duplicate = T::class.java.newInstanceNative()
    for (field in T::class.java.allFields.filter(excludeModifiers = Modifier.STATIC)) kotlin.runCatching {
        field.isFinal = false
        field[duplicate] = field[this]
    }
    return duplicate
}

fun <T> Class<T>.implement(handler: InvocationHandler): T =
    Proxy.newProxyInstance(classLoader, arrayOf(this), handler) as T

private val cachedFields = HashMap<Class<*>, Array<Field>>()
private val cachedMethods = HashMap<Class<*>, Array<Method>>()
private val privateGetDeclaredFieldsMethod =
    Class::class.java.getDeclaredMethod("privateGetDeclaredFields", Boolean::class.javaPrimitiveType)
        .apply { isAccessible = true }
private val publicMethodsClass = Class.forName("java.lang.PublicMethods")
private val publicMethodsConstructor = publicMethodsClass.getDeclaredConstructor().apply { isAccessible = true }
private val mergeMethod =
    publicMethodsClass.getDeclaredMethod("merge", Method::class.java).apply { isAccessible = true }
private val toArrayMethod = publicMethodsClass.getDeclaredMethod("toArray").apply { isAccessible = true }
private val privateGetDeclaredMethodsMethod =
    Class::class.java.getDeclaredMethod("privateGetDeclaredMethods", Boolean::class.javaPrimitiveType)
        .apply { isAccessible = true }
private val accessFlagField: Field? by lazy {
    try {
        Field::class.java.getFieldNative("accessFlags")
    } catch (e: Throwable) {
        try {
            Field::class.java.getFieldNative("modifiers")
        } catch (e: Throwable) {
            null
        }
    }
}

@Suppress("DEPRECATION")
private fun <T : AccessibleObject> addAll(collection: MutableCollection<T>, array: Any) {
    array as Array<T>
    for (i in 0 until array.size)
        collection.add(array[i].apply { if (!isAccessible) isAccessible = true })
}

private fun checkSignatureParams(signature: Array<out Class<*>>, paramTypes: Array<out Class<*>?>): Boolean {
    if (signature.size != paramTypes.size) return false
    for (i in signature.indices) {
        val param = paramTypes[i] ?: continue
        if (!signature[i].kotlin.javaObjectType.isAssignableFrom(param.kotlin.javaObjectType)) return false
    }
    return true
}

private fun Class<*>.methodToString(methodName: String, params: Array<out Class<*>?>): String {
    val joiner = StringJoiner(", ", "$name.$methodName(", ")")
    for (i in params.indices) joiner.add(params[i]?.name.toString())
    return joiner.toString()
}

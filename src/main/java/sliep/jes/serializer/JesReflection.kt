@file:Suppress("UNCHECKED_CAST", "unused")

package sliep.jes.serializer

import addIfNotContained
import capitalizeFirst
import excludes
import includes
import sun.misc.Unsafe
import java.lang.reflect.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/* ********************************************************** *\
 ** -------------------  CONSTRUCTORS  ------------------- **
\* ********************************************************** */
/**
 * Instantiate a class using reflection like calling it's constructor
 *
 * No matter if it's not accessible
 * @author sliep
 * @receiver the [Class] to be instantiated
 * @param T Type of new instance
 * @param params constructor arguments
 * @return the newly created instance
 * @throws NoSuchMethodException if a matching method is not found.
 * @throws UnsupportedOperationException if you're trying to instantiate a non-instantiable class
 * @throws InvocationTargetException if the underlying constructor throws an exception.
 * @see [checkAllocationPossible]
 * @see [Constructor.newInstance]
 */
@Throws(NoSuchMethodException::class, UnsupportedOperationException::class, InvocationTargetException::class)
fun <T> Class<T>.newInstance(vararg params: Any?): T {
    checkAllocationPossible()
    val constructor = guessFromParametersTypes(
        name,
        declaredConstructors,
        null,
        Array(params.size) { i -> if (params[i] == null) null else params[i]!!::class }
    )
    constructor.isAccessible = true
    return constructor.newInstance(*params) as T
}

/**
 * @author sliep
 * @see newInstance
 */
@Throws(NoSuchMethodException::class, UnsupportedOperationException::class, InvocationTargetException::class)
inline fun <reified T : Any> newInstance(vararg params: Any?) = T::class.java.newInstance(*params)

/**
 * Get the constructor of a specific class
 * @author sliep
 * @receiver the declaring [Class] of the constructor
 * @param T Type of the class
 * @param paramsTypes constructor arguments types
 * @return the constructor that matches the parameter types
 * @throws NoSuchMethodException if a matching method is not found.
 * @see [Class.getDeclaredConstructor]
 */
@Throws(NoSuchMethodException::class)
fun <T> Class<T>.constructor(vararg paramsTypes: Class<*>): Constructor<out T> {
    val constructor = getDeclaredConstructor(*paramsTypes)
    constructor.isAccessible = true
    return constructor
}

/**
 * @author sliep
 * @see Class.constructor
 */
@Throws(NoSuchMethodException::class)
inline fun <reified T : Any> constructor(vararg paramsTypes: Class<*>) = T::class.java.constructor(*paramsTypes)

/**
 * Create an instance of a given class if possible using it's constructor, otherwise through [Unsafe] bypassing the constructor
 * @author sliep
 * @receiver the [Class] to be instantiated
 * @param T Type of new instance
 * @return the newly created instance
 * @throws NoSuchMethodException if a matching method is not found.
 * @throws UnsupportedOperationException if you're trying to instantiate a non-instantiable class
 * @see [checkAllocationPossible]
 * @see [Unsafe.allocateInstance]
 */
@Throws(NoSuchMethodException::class, UnsupportedOperationException::class)
fun <T> Class<T>.newUnsafeInstance(): T {
    checkAllocationPossible()
    return Unsafe::class.getField<Unsafe>("theUnsafe").allocateInstance(this) as T
}

/**
 * @author sliep
 * @see Class.newUnsafeInstance
 */
@Throws(NoSuchMethodException::class, UnsupportedOperationException::class)
inline fun <reified T : Any> newUnsafeInstance() = T::class.java.newUnsafeInstance()

/**
 * Get all declared constructor of the receiver class matching given parameters
 * @author sliep
 * @receiver the declaring [Class] of the constructors
 * @param T Type of the class
 * @param modifiers modifiers that a constructor should have to be included or 0 to include all except excluded. default is include all
 * @param excludeModifiers modifiers that a constructor should not have to be included or 0 to exclude none. default is exclude none
 * @return an array of found matching constructors
 */
fun <T> Class<T>.constructors(modifiers: Int = 0, excludeModifiers: Int = 0): Array<Constructor<out T>> {
    val response = ArrayList<Constructor<out T>>()
    for (constructor in declaredConstructors)
        if ((modifiers == 0 || constructor.modifiers includes modifiers) &&
            (excludeModifiers == 0 || constructor.modifiers excludes excludeModifiers) &&
            !response.contains(constructor)
        ) response.add(constructor as Constructor<out T>)
    return response.toTypedArray()
}

/**
 * @author sliep
 * @see Class.constructors
 */
inline fun <reified T : Any> constructors(modifiers: Int = 0, excludeModifiers: Int = 0) =
    T::class.java.constructors(modifiers, excludeModifiers)

/* ********************************************************** *\
 ** ----------------------  FIELDS  ---------------------- **
\* ********************************************************** */

/**
 * Get the property value of a receiver object through reflection
 *
 * No matter if it's not accessible
 * @author sliep
 * @receiver the instance having the magic property or the property declaring class to fetch a static value
 * @param R Type of return field
 * @param name of the property
 * @param inParent tell the function to search the field not only in the declaring class, but even in it's superclasses. default is true
 * @return the value of the wanted property
 * @throws NoSuchFieldException if a field with the specified name is not found.
 * @see [Class.getDeclaredField]
 */
@Throws(NoSuchFieldException::class)
fun <R : Any?> Any.getField(name: String, inParent: Boolean = true): R {
    val field = asClass.field(name, inParent)
    return field[if (Modifier.isStatic(field.modifiers)) null else this] as R
}

/**
 * Set the property value of a receiver object through reflection
 *
 * No matter if it's not accessible or final
 * @author sliep
 * @receiver the instance having the magic property or the property declaring class to fetch a static value
 * @param name of the property
 * @param value to be set
 * @param inParent tell the function to search the field not only in the declaring class, but even in it's superclasses. default is true
 * @throws NoSuchFieldException if a field with the specified name is not found.
 * @see [Class.getDeclaredField]
 */
@Throws(NoSuchFieldException::class)
fun Any.setField(name: String, value: Any?, inParent: Boolean = true) {
    val field = asClass.field(name, inParent)
    field.isFinal = false
    field[if (Modifier.isStatic(field.modifiers)) null else this] = value
}

/**
 * Fetch the field of a receiver object through reflection
 *
 * No matter if it's not accessible
 * @author sliep
 * @receiver the declaring class of the magic property
 * @param name of the property
 * @param inParent tell the function to search the field not only in the declaring class, but even in it's superclasses. default is false
 * @return the already accessible field
 * @throws NoSuchFieldException if a field with the specified name is not found.
 * @see [Class.getDeclaredField]
 */
@Throws(NoSuchFieldException::class)
fun Class<*>.field(name: String, inParent: Boolean = false): Field {
    var clazz = this
    var firstError: Throwable? = null
    while (true)
        try {
            val field = clazz.getDeclaredField(name)
            field.isAccessible = true
            return field
        } catch (e: NoSuchFieldException) {
            if (!inParent) throw e
            if (firstError == null) firstError = e
            clazz = clazz.superclass ?: throw firstError
        }
}

/**
 * @author sliep
 * @see Class.field
 */
@Throws(NoSuchFieldException::class)
inline fun <reified T : Any> field(name: String, inParent: Boolean = false) = T::class.java.field(name, inParent)

/**
 * Get all fields (not only declared) of the receiver class matching given parameters
 * @author sliep
 * @receiver the declaring [Class] of the fields
 * @param modifiers modifiers that a field should have to be included or 0 to include all except excluded. default is include all
 * @param excludeModifiers modifiers that a field should not have to be included or 0 to exclude none. default is exclude none
 * @return an array of found matching fields
 */
fun Class<*>.fields(modifiers: Int = 0, excludeModifiers: Int = 0): Array<Field> {
    val response = ArrayList<Field>()
    var clazz = this
    while (true) {
        for (field in clazz.declaredFields)
            if ((modifiers == 0 || field.modifiers includes modifiers) && (excludeModifiers == 0 || field.modifiers excludes excludeModifiers))
                response.addIfNotContained(field)
        clazz = clazz.superclass ?: return response.toTypedArray()
    }
}

/**
 * @author sliep
 * @see Class.fields
 */
inline fun <reified T : Any> fields(modifiers: Int = 0, excludeModifiers: Int = 0) =
    T::class.java.fields(modifiers, excludeModifiers)

/**
 * Call kotlin (or java) property getters of a receiver object through reflection
 *
 * No matter if it's not accessible
 *
 * If the property name is 'foo', 'getFoo()' method will be called (if exists)
 * @author sliep
 * @receiver the instance having the magic property
 * @param R Type of return value
 * @param fieldName of the property
 * @return the value of the wanted property
 * @throws NoSuchMethodException if a matching method is not found.
 * @see [invokeMethod]
 */
@Throws(NoSuchMethodException::class)
fun <R : Any?> Any.callGetter(fieldName: String) = invokeMethod("get${fieldName.capitalizeFirst()}") as R

/**
 * Call kotlin (or java) property setters of a receiver object through reflection
 *
 * No matter if it's not accessible
 *
 * If the property name is 'foo', 'setFoo()' method will be called (if exists)
 * @author sliep
 * @receiver the instance having the magic property
 * @param fieldName of the property
 * @param value new value for the property
 * @throws NoSuchMethodException if a matching method is not found.
 * @throws InvocationTargetException if the underlying setter throws an exception.
 * @see [invokeMethod]
 */
@Throws(NoSuchMethodException::class, InvocationTargetException::class)
fun Any.callSetter(fieldName: String, value: Any?) {
    invokeMethod<Any?>("set${fieldName.capitalizeFirst()}", value)
}

/**
 * Get constant fields of a given class (all public static final fields) as Array
 * @author sliep
 */
val Class<*>.CONSTANTS: HashMap<String, Any?>
    get() {
        val constants = HashMap<String, Any?>()
        fields(Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL).forEach { constant -> constants[constant.name] = constant[null] }
        return constants
    }
/**
 * Allows you to change the final modifier of a field at runtime
 *
 * After setting field.[isFinal] = false you are free to edit it's value
 * @author sliep
 */
var Field.isFinal: Boolean
    get() = Modifier.isFinal(modifiers)
    set(value) {
        val accessFlags = MODIFIERS ?: return
        if (Modifier.isFinal(modifiers) != value) kotlin.runCatching {
            accessFlags[this] = modifiers and if (value) Modifier.FINAL else Modifier.FINAL.inv()
        }
    }

/* ********************************************************** *\
 ** ---------------------  METHODS  ---------------------- **
\* ********************************************************** */
/**
 * Invoke a specific method (not only declared) of an object through reflection
 *
 * No matter if it's not accessible
 * @author sliep
 * @receiver the instance that provide the method or it's declaring class to invoke a static method
 * @param R return type of the method
 * @param name of the method
 * @param params arguments of the method (must be compatible with the length and the types of the method)
 * @return the result of method
 * @throws NoSuchMethodException if a matching method is not found.
 * @throws InvocationTargetException if the underlying method throws an exception.
 * @see [Method.invoke]
 */
@Throws(NoSuchMethodException::class, InvocationTargetException::class)
fun <R : Any?> Any.invokeMethod(name: String, vararg params: Any?): R {
    var clazz = asClass
    var firstError: Throwable? = null
    while (true)
        try {
            val method = guessFromParametersTypes(
                clazz.name,
                clazz.declaredMethods,
                name,
                Array(params.size) { i -> if (params[i] == null) null else params[i]!!::class }
            )
            method.isAccessible = true
            return method.invoke(if (Modifier.isStatic(method.modifiers)) null else this, *params) as R
        } catch (e: NoSuchMethodException) {
            if (firstError == null) firstError = e
            clazz = clazz.superclass ?: throw firstError
        }
}

/**
 * Get a method of a class from given name
 * @author sliep
 * @receiver the declaring class of the method
 * @param name of the method
 * @param searchParent tell the function to search the method not only in the declaring class, but even in it's superclasses. default true
 * @param paramsTypes method arguments types
 * @return method object
 * @throws NoSuchMethodException if a matching method is not found.
 * @see [Class.getDeclaredMethod]
 */
@Throws(NoSuchMethodException::class)
fun Class<*>.method(name: String, vararg paramsTypes: Class<*>, searchParent: Boolean = false): Method {
    var clazz = this
    while (true)
        try {
            val method = clazz.getDeclaredMethod(name, *paramsTypes)
            method.isAccessible = true
            return method
        } catch (e: Throwable) {
            if (!searchParent) throw e
            clazz = clazz.superclass ?: throw e
        }
}

/**
 * @author sliep
 * @see Class.method
 */
@Throws(NoSuchMethodException::class)
inline fun <reified T : Any> method(name: String, vararg params: Class<*>, searchParent: Boolean = false) =
    T::class.java.method(name, *params, searchParent = searchParent)

/**
 * Get all methods (not only declared) of the receiver class matching given parameters
 * @author sliep
 * @receiver the declaring [Class] of the methods
 * @param modifiers modifiers that a method should have to be included or 0 to include all except excluded. default is include all
 * @param excludeModifiers modifiers that a method should not have to be included or 0 to exclude none. default is exclude none
 * @return an array of found matching methods
 */
fun Class<*>.methods(modifiers: Int = 0, excludeModifiers: Int = 0): Array<Method> {
    val response = ArrayList<Method>()
    var clazz = this
    while (true) {
        for (method in clazz.declaredMethods)
            if ((modifiers == 0 || method.modifiers includes modifiers) &&
                (excludeModifiers == 0 || method.modifiers excludes excludeModifiers)
            ) response.addIfNotContained(method)
        clazz = clazz.superclass ?: return response.toTypedArray()
    }
}

/**
 * @author sliep
 * @see Class.methods
 */
inline fun <reified T : Any> methods(modifiers: Int = 0, excludeModifiers: Int = 0) =
    T::class.java.methods(modifiers, excludeModifiers)

/**
 * Makes possible to find a compatible method having only it's parameter types (or sub types) and the name
 * @author sliep
 * @param clazzName only for debug: the name of declaring class
 * @param members all methods in that class
 * @param name of the method
 * @param params method arguments types (null types are jolly, this can lead to ambiguity problems)
 * @return the matching method
 * @throws NoSuchMethodException if a matching method is not found.
 */
@Throws(NoSuchMethodException::class)
fun <M : Executable> guessFromParametersTypes(
    clazzName: String,
    members: Array<M>,
    name: String?,
    params: Array<KClass<*>?>
): M {
    bob@ for (method in members) {
        if (name != null && method.name != name) continue
        val types = method.parameterTypes
        if (types.size != params.size) continue
        for (i in types.indices)
            if (params[i] != null && !types[i].kotlin.javaObjectType.isAssignableFrom(params[i]!!.javaObjectType)) continue@bob
        method.isAccessible = true
        return method
    }
    throw NoSuchMethodException(
        methodToString(clazzName, name ?: "<init>", Array(params.size) { i -> params[i]?.java })
    )
}

/**
 * For debug create a signature descriptor of a method
 * @author sliep
 * @param className the name of declaring class
 * @param name of the method
 * @param argTypes method arguments types
 * @return a method descriptor like java.lang.String.substring(int, int)
 */
fun methodToString(className: String?, name: String, argTypes: Array<out Class<*>?>): String {
    val prefix = StringBuilder()
    if (className != null) prefix.append(className).append('.')
    prefix.append(name).append('(')
    val joiner = StringJoiner(", ", prefix.toString(), ")")
    for (i in argTypes.indices) joiner.add(argTypes[i]?.name)
    return joiner.toString()
}

/**
 * signature descriptor of a method/constructor
 * @author sliep
 * @see methodToString
 */
val Executable.signature
    get() = methodToString(if (this is Constructor<*>) declaringClass.name else null, name, parameterTypes)

/* ********************************************************** *\
 ** ---------------------  CLASSES  ---------------------- **
\* ********************************************************** */
/**
 * Implement an interface through reflection
 * @author sliep
 * @param T the class to be instantiated
 * @param handler invocation handler to dispatch method invocations to
 * @return a new proxy instance of the given class
 * @throws IllegalArgumentException if [T] is not an interface
 */
@Throws(IllegalArgumentException::class)
fun <T> Class<T>.implement(handler: InvocationHandler): T =
    Proxy.newProxyInstance(classLoader, arrayOf(this), handler) as T

/**
 * @author sliep
 * @see Class.implement
 */
inline fun <reified T : Any> implement(handler: InvocationHandler) = T::class.java.implement(handler)

val Method.isGetter get() = name.startsWith("get") && parameterTypes.isEmpty()
val Method.isSetter get() = name.startsWith("set") && parameterTypes.size == 1
val Method.propName get() = name[3].toLowerCase() + name.substring(4)

/**
 * Get the java primitive type of a class (e.g. [java.lang.Integer] -> [kotlin.Int])
 *
 * an [unwrappedClass] of a non primitive class is equal to the receiver object
 * @author sliep
 */
val KClass<*>.unwrappedClass: Class<*> get() = javaPrimitiveType ?: java

/**
 * Get the java object type of a class (e.g. [kotlin.Int] -> [java.lang.Integer])
 *
 * a [wrappedClass] of a non primitive class is equal to the receiver object
 * @author sliep
 */
val KClass<*>.wrappedClass: Class<*> get() = javaObjectType

/**
 * Unlike [Class.isPrimitive] this value result true even for wrapper types
 * @author sliep
 */
val KClass<*>.isTypePrimitive: Boolean get() = javaPrimitiveType != null
/**
 * Dimensions of an array type (zero for non array types)
 * @author sliep
 */
val Class<*>.dimensions get() = name.lastIndexOf('[') + 1

/**
 * @author sliep
 * @see Class.dimensions
 */
val KClass<*>.dimensions: Int get() = java.dimensions

/* ********************************************************** *\
 ** ---------------------  UTILITY  ---------------------- **
\* ********************************************************** */

/**
 * Modifier modifiers
 * @author sliep
 */
private val MODIFIERS: Field? by lazy {
    try {
        Field::class.java.field("accessFlags")
    } catch (e: Throwable) {
        try {
            Field::class.java.field("modifiers")
        } catch (e: Throwable) {
            null
        }
    }
}

/**
 * If your QI is very low and you're trying to instantiate a primitive type or an abstract class or a enum class, this method will tell you 'Hey man, you can't!' :)
 * @author sliep
 * @throws UnsupportedOperationException if you're trying to instantiate a non-instantiable class
 * @receiver the [Class] to check
 */
internal fun Class<*>.checkAllocationPossible() {
    if (isPrimitive) throw UnsupportedOperationException("Cannot allocate primitive type!")
    if (Modifier.isAbstract(modifiers)) throw UnsupportedOperationException("Cannot allocate abstract class!")
    if (isEnum) throw UnsupportedOperationException("Cannot allocate enum class!")
}

internal inline val Any.asClass
    get() = when {
        this is Class<*> -> this
        this is KClass<*> -> this.java
        else -> this::class.java
    }

fun Any.thisToString(
    toString: (field: Any?) -> String? = { f -> f?.toString() },
    arrayToString: (field: Array<*>?) -> String? = { f -> if (f.isNullOrEmpty()) null else Arrays.toString(f) },
    prefix: String = this::class.java.simpleName,
    separator: String = "->"
): String = StringBuilder(prefix).apply {
    if (prefix.isNotEmpty()) append('\n')
    bob@ for (field in this@thisToString::class.java.fields(Modifier.PUBLIC, Modifier.STATIC)) {
        var fieldVal = field[this@thisToString]
        fieldVal = when (fieldVal) {
            is Array<*> -> arrayToString(fieldVal) ?: continue@bob
            else -> toString(fieldVal) ?: continue@bob
        }
        append("    ${field.name} $separator $fieldVal\n")
    }
}.toString().trim()

val superInstances = HashMap<Int, Field>()

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <T> Any.getSuper(prop: KProperty<*>): T {
    val theId = System.identityHashCode(this) * prop.hashCode()
    var field = superInstances[theId]
    if (field == null) {
        field = this::class.java.superclass!!.field(prop.name, true)
        field.isFinal = false
        superInstances[theId] = field
    }
    return field[this] as T
}

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <T> Any.setSuper(prop: KProperty<*>, value: T) {
    val theId = System.identityHashCode(this) * prop.hashCode()
    var field = superInstances[theId]
    if (field == null) {
        field = this::class.java.superclass!!.field(prop.name, true)
        field.isFinal = false
        superInstances[theId] = field
    }
    field[this] = value
}
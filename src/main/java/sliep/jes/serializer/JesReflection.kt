@file:Suppress("UNCHECKED_CAST", "unused")

package sliep.jes.serializer

import sun.misc.Unsafe
import java.lang.reflect.*
import java.util.*
import kotlin.reflect.KClass

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
    val constructor = guessFromParameters(name, declaredConstructors, null, params)
    constructor.isAccessible = true
    return constructor.newInstance(*params) as T
}

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
    val clazz = this
    for (allocation in AllocationMethod.values())
        allocation.runCatching { return newInstance(clazz) }
    throw UnsupportedOperationException("Cannot allocate instance of type: $name")
}

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
    return response.toArray(arrayOf())
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
/* ********************************************************** *\
 ** ----------------------  FIELDS  ---------------------- **
\* ********************************************************** */

/**
 * Fetch the property value of a receiver object through reflection
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
fun <R : Any?> Any.field(name: String, inParent: Boolean = true): R {
    val clazz = when {
        this is Class<*> -> this
        this is KClass<*> -> this.java
        else -> this::class.java
    }
    val fieldR = clazz.fieldR(name, inParent)
    return fieldR[if (Modifier.isStatic(fieldR.modifiers)) null else this] as R
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
fun Class<*>.fieldR(name: String, inParent: Boolean = false): Field {
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
            if ((modifiers == 0 || field.modifiers includes modifiers) &&
                (excludeModifiers == 0 || field.modifiers excludes excludeModifiers) &&
                !response.contains(field)
            ) response.add(field)
        clazz = clazz.superclass ?: return response.toArray(arrayOf())
    }
}

/**
 * Call kotlin (or java) property getters of a receiver object through reflection
 *
 * No matter if it's not accessible
 *
 * If the property name is 'foo', 'getFoo()' or 'isFoo()' method will be called (if exists)
 * @author sliep
 * @receiver the instance having the magic property
 * @param R Type of return value
 * @param fieldName of the property
 * @return the value of the wanted property
 * @throws NoSuchMethodException if a matching method is not found.
 * @see [invokeMethod]
 */
@Throws(NoSuchMethodException::class)
fun <R : Any?> Any.callGetter(fieldName: String) = try {
    invokeMethod("get${fieldName[0].toUpperCase()}${fieldName.substring(1)}") as R
} catch (e: NoSuchMethodException) {
    try {
        invokeMethod("is${fieldName[0].toUpperCase()}${fieldName.substring(1)}") as R
    } catch (ignore: NoSuchMethodException) {
        throw e
    }
}

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
    invokeMethod<Any?>("set${fieldName[0].toUpperCase()}${fieldName.substring(1)}", value)
}

/**
 * Modifier modifiers
 * @author sliep
 */
private val MODIFIERS
    get() = lateInit {
        runCatching {
            runCatching { Field::class.java.fieldR("accessFlags") }
                .getOrDefault(Field::class.java.fieldR("modifiers"))
        }.getOrNull()
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
    var clazz = when {
        this is Class<*> -> this
        this is KClass<*> -> this.java
        else -> this::class.java
    }
    var firstError: Throwable? = null
    while (true)
        try {
            val method = guessFromParameters(clazz.name, clazz.declaredMethods, name, params)
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
fun Class<*>.method(name: String, vararg paramsTypes: Class<*>, searchParent: Boolean = true): Method {
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
                (excludeModifiers == 0 || method.modifiers excludes excludeModifiers) &&
                !response.contains(method)
            ) response.add(method)
        clazz = clazz.superclass ?: return response.toArray(arrayOf())
    }
}

/**
 * Makes possible to find a compatible method having only it's parameter values and the name
 * @author sliep
 * @param clazzName only for debug: the name of declaring class
 * @param members all methods in that class
 * @param name of the method
 * @param params input method arguments (null arguments are jolly, this can lead to ambiguity problems)
 * @return the matching method
 * @throws NoSuchMethodException if a matching method is not found.
 */
@Throws(NoSuchMethodException::class)
fun <M : Executable> guessFromParameters(
    clazzName: String,
    members: Array<out M>,
    name: String?,
    params: Array<out Any?>
) = guessFromParametersTypes(
    clazzName,
    members,
    name,
    kotlin.Array(params.size) { i -> if (params[i] == null) null else params[i]!!::class }
)

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
    members: Array<out M>,
    name: String?,
    params: Array<out KClass<*>?>
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
 * @param implementer invocation handler to dispatch method invocations to
 * @return a new proxy instance of the given class
 * @throws IllegalArgumentException if [T] is not an interface
 * @see JesImplementer
 */
@Throws(IllegalArgumentException::class)
fun <T> Class<T>.implement(implementer: JesImplementer<T>) =
    Proxy.newProxyInstance(classLoader, arrayOf(this)) { proxy, method, args ->
        val methodName = method.name
        val argsCount = args?.size ?: 0
        proxy as T
        when {
            implementer is PropertyImplementer<T> && argsCount == 0 && methodName.startsWith("get") && methodName[3].isUpperCase() ->
                implementer.apply {
                    return@newProxyInstance proxy.get(
                        methodName[3].toLowerCase() + methodName.substring(4),
                        method.returnType
                    )
                }
            implementer is PropertyImplementer<T> && argsCount == 0 && methodName.startsWith("is") && methodName[2].isUpperCase() ->
                implementer.apply {
                    return@newProxyInstance proxy.get(
                        methodName[2].toLowerCase() + methodName.substring(3),
                        method.returnType
                    )
                }
            implementer is PropertyImplementer<T> && argsCount == 1 && methodName.startsWith("set") && methodName[3].isUpperCase() ->
                implementer.apply {
                    return@newProxyInstance proxy.set(
                        methodName[3].toLowerCase() + methodName.substring(4), args[0],
                        method.parameterTypes[0]
                    )
                }
            implementer is FunctionImplementer<T> ->
                implementer.apply { return@newProxyInstance proxy.memberFunction(method, args ?: arrayOf()) }
            else -> throw UnsupportedOperationException("Incompatible implementer: method=$methodName implementer=$implementer")
        }
    } as T

/**
 * @author sliep
 * @see implement
 */
fun <T> Class<T>.implement(functionImplementer: T.(method: Method, args: Array<out Any>) -> Any?) =
    implement(object : FunctionImplementer<T> {
        override fun T.memberFunction(method: Method, args: Array<out Any>) = functionImplementer(method, args)
    })

/**
 * Get the java primitive type of a class (e.g. [java.lang.Integer] -> [kotlin.Int])
 *
 * an [unwrappedClass] of a non primitive class is equal to the receiver object
 * @author sliep
 */
val Class<*>.unwrappedClass: Class<*> get() = kotlin.unwrappedClass
/**
 * Get the java object type of a class (e.g. [kotlin.Int] -> [java.lang.Integer])
 *
 * a [wrappedClass] of a non primitive class is equal to the receiver object
 * @author sliep
 */
val Class<*>.wrappedClass: Class<*> get() = kotlin.wrappedClass
/**
 * Unlike [Class.isPrimitive] this value result true even for wrapper types
 * @author sliep
 */
val Class<*>.isTypePrimitive: Boolean get() = kotlin.isTypePrimitive
/**
 * Dimensions of an array type (zero for non array types)
 * @author sliep
 */
val Class<*>.dimensions get() = name.lastIndexOf('[') + 1

/* ********************************************************** *\
 ** ---------------------  UTILITY  ---------------------- **
\* ********************************************************** */

/**
 * Check if flag is contained in int
 * @author sliep
 * @receiver flags
 * @param flag to check
 * @return if flag is contained
 */
infix fun Int.includes(flag: Int) = this and flag == flag

/**
 * Check if flag is not contained in int
 * @author sliep
 * @receiver flags
 * @param flag to check
 * @return if flag is not contained
 */
infix fun Int.excludes(flag: Int) = this and flag == 0
/**
 * Utility class to instantiate an object
 * @author sliep
 */
internal enum class AllocationMethod {
    NATIVE_NEW_INSTANCE {
        @Suppress("RemoveRedundantSpreadOperator")
        override fun <T> newInstance(c: Class<T>) = c.constructor().newInstance()!!
    },
    UNSAFE {
        override fun <T> newInstance(c: Class<T>) = Unsafe::class.field<Unsafe>("theUnsafe").allocateInstance(c) as T
    };

    @Throws(Throwable::class)
    abstract fun <T> newInstance(c: Class<T>): T
}

/**
 * Handle simultaneously getters/setters and normal functions
 *
 * If the function name starts with get/is/set the invocation will be dispatched to [PropertyImplementer] else to [FunctionImplementer]
 * @author sliep
 * @param T proxy type
 * @see JesImplementer
 */
interface InterfaceImplementer<T> : PropertyImplementer<T>, FunctionImplementer<T>

/**
 * Handle every property access of the proxy interface
 * This implementer is great when you want to define an interface without methods but only properties (only in kotlin)
 *
 * Example of usage:
 * ```kotlin
 * interface MyInterface {
 *     var setting: String
 *     val name: String
 *     var something: Int
 * }
 *
 * fun main() {
 *     implement(object : PropertyImplementer<MyInterface> {
 *         override fun MyInterface.get(property: String): Any? = when (property) {
 *             "setting" -> mySettings
 *             "name" -> "Gian Paolo"
 *             "something" -> 1234
 *             else -> throw UnsupportedOperationException("UNIMPLEMENTED")
 *         }
 *
 *         override fun MyInterface.set(property: String, value: Any?) = when (property) {
 *             "setting" -> mySettings = value as String
 *             "something" -> setSomething(value as Int)
 *             else -> throw UnsupportedOperationException("UNIMPLEMENTED")
 *         }
 *     })
 * }
 * ```
 * @author sliep
 * @param T proxy type
 * @see JesImplementer
 */
interface PropertyImplementer<T> : JesImplementer<T> {
    /**
     * A property getter is a empty-parameter function called get[property] or is[property] where property is the name of the property having the first letter capitalized
     * @author sliep
     * @receiver proxy instance
     * @param property name
     * @param propertyType return type of the method
     * @return property value
     */
    fun T.get(property: String, propertyType: Class<*>): Any?

    /**
     * A property setter is a function with 1 parameter called set[property] where property is the name of the property having the first letter capitalized
     * @author sliep
     * @receiver proxy instance
     * @param property name
     * @param propertyType type of method parameter
     * @param value new value to be set to the property
     */
    fun T.set(property: String, value: Any?, propertyType: Class<*>)
}

/**
 * Handle every function call of the proxy interface
 *
 * Example of usage:
 * ```kotlin
 * interface MyInterface {
 *     fun doSomething(aNumber: Int, lol: Boolean): String
 *     fun doSomethingElse(): FloatArray
 * }
 *
 * fun main() {
 *     implement<MyInterface> { name, args ->
 *         when (name) {
 *             "doSomething" -> if (args[1] as Boolean) "lol" else args[0].toString()
 *             "doSomethingElse" -> throw IllegalStateException("Causal error just for fun")
 *             else -> throw UnsupportedOperationException("UNIMPLEMENTED")
 *         }
 *     }
 * }
 * ```
 * @author sliep
 * @param T proxy type
 * @see JesImplementer
 */
interface FunctionImplementer<T> : JesImplementer<T> {

    /**
     * Implement this method to handle a function call
     * @author sliep
     * @receiver proxy instance
     * @param method invoked
     * @param args parameters passed to the method (can be empty but non null)
     * @return function result or Unit for void functions
     */
    fun T.memberFunction(method: Method, args: Array<out Any>): Any?
}

/**
 * When a method is invoked on a proxy instance created by [implement], the method invocation is encoded and dispatched to the [JesImplementer]
 *
 * Do not pass to implement method a direct instance of this interface, instead use it's subtypes
 * - [FunctionImplementer] to handle any function call as a member function
 * - [PropertyImplementer] to handle only getters and setters
 * - [InterfaceImplementer] a mix of the previous implementers: perfect solution to handle getters setters and functions in a separate way
 * @author sliep
 * @param T proxy type
 * @see implement
 * @see InvocationHandler
 */
interface JesImplementer<T>
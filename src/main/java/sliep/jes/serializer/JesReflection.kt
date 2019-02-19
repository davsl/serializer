@file:Suppress("UNCHECKED_CAST", "unused")

package sliep.jes.serializer

import sun.misc.Unsafe
import java.lang.reflect.*
import java.util.*
import kotlin.reflect.KClass

/**
 * @author sliep
 * Reflective operation core
 */

/************************************************************\
 ** -------------------  CONSTRUCTORS  ------------------- **
\************************************************************/
/**
 * @author sliep
 * Instantiate a class using reflection like calling it's constructor
 * No matter if it's not accessible
 * @receiver the [Class] to be instantiated
 * @param T Type of new instance
 * @param params constructor arguments
 * @return the newly created instance
 * @throws NoSuchMethodException if a matching method is not found.
 * @throws UnsupportedOperationException if you're trying to instantiate a non-instantiable class
 * @throws InvocationTargetException if the underlying constructor throws an exception.
 * @see [checkAllocatePossible]
 * @see [Constructor.newInstance]
 */
@Throws(NoSuchMethodException::class, UnsupportedOperationException::class, InvocationTargetException::class)
fun <T> Class<T>.newInstance(vararg params: Any?): T {
    checkAllocatePossible()
    val constructor = guessFromParameters(name, declaredConstructors, null, params)
    constructor.isAccessible = true
    return constructor.newInstance(*params) as T
}

/**
 * @author sliep
 * Get the constructor of a specific class
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
 * Create an instance of a given class if possible using it's constructor, otherwise through [Unsafe] bypassing the constructor
 * @receiver the [Class] to be instantiated
 * @param T Type of new instance
 * @return the newly created instance
 * @throws NoSuchMethodException if a matching method is not found.
 * @throws UnsupportedOperationException if you're trying to instantiate a non-instantiable class
 * @see [checkAllocatePossible]
 * @see [Unsafe.allocateInstance]
 */
@Throws(NoSuchMethodException::class, UnsupportedOperationException::class)
fun <T> Class<T>.newUnsafeInstance(): T {
    checkAllocatePossible()
    val clazz = this
    for (allocation in AllocationMethod.values())
        allocation.runCatching { return newInstance(clazz) }
    throw UnsupportedOperationException("Cannot allocate instance of type: $name")
}

/**
 * @author sliep
 * Get all declared constructor of the receiver class matching given parameters
 * @receiver the declaring [Class] of the constructors
 * @param T Type of the class
 * @param modifiers modifiers that a constructor should have to be included or 0 to include all except excluded. default is include all
 * @param excludeModifiers modifiers that a constructor should not have to be included or 0 to exclude none. default is exclude none
 * @return an array of found matching constructors
 */
fun <T> Class<T>.constructors(modifiers: Int = 0, excludeModifiers: Int = 0): Array<Constructor<out T>> {
    val response = ArrayList<Constructor<out T>>()
    for (constructor in declaredConstructors)
        if ((modifiers == 0 || constructor.modifiers and modifiers == modifiers) && (excludeModifiers == 0 || constructor.modifiers and excludeModifiers == 0) && !response.contains(constructor))
            response.add(constructor as Constructor<out T>)
    return response.toArray(arrayOf())
}

/**
 * @author sliep
 * If your QI is very low and you're trying to instantiate a primitive type or an abstract class or a enum class, this method will tell you 'Hey man, you can't!' :)
 * @throws UnsupportedOperationException if you're trying to instantiate a non-instantiable class
 * @receiver the [Class] to check
 */
fun Class<*>.checkAllocatePossible() {
    if (isPrimitive) throw UnsupportedOperationException("Cannot allocate primitive type!")
    if (Modifier.isAbstract(modifiers)) throw UnsupportedOperationException("Cannot allocate abstract class!")
    if (isEnum) throw UnsupportedOperationException("Cannot allocate enum class!")
}
/************************************************************\
 ** ----------------------  FIELDS  ---------------------- **
\************************************************************/

/**
 * @author sliep
 * Fetch the property value of a receiver object through reflection
 * No matter if it's not accessible
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
 * @author sliep
 * Fetch the field of a receiver object through reflection
 * No matter if it's not accessible
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
 * @author sliep
 * Get all fields (not only declared) of the receiver class matching given parameters
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
            if ((modifiers == 0 || field.modifiers and modifiers == modifiers) && (excludeModifiers == 0 || field.modifiers and excludeModifiers == 0) && !response.contains(field))
                response.add(field)
        clazz = clazz.superclass ?: return response.toArray(arrayOf())
    }
}

/**
 * @author sliep
 * Call kotlin (or java) property getters of a receiver object through reflection
 * No matter if it's not accessible
 * If the property name is 'foo', 'getFoo()' or 'isFoo()' method will be called (if exists)
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
 * @author sliep
 * Call kotlin (or java) property setters of a receiver object through reflection
 * No matter if it's not accessible
 * If the property name is 'foo', 'setFoo()' method will be called (if exists)
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
 * @author sliep
 * Modifier modifiers
 */
private val MODIFIERS get() = lateInit { kotlin.runCatching { kotlin.runCatching { Field::class.java.fieldR("accessFlags") }.getOrDefault(Field::class.java.fieldR("modifiers")) }.getOrNull() }

/**
 * @author sliep
 * Get constant fields of a given class (all public static final fields) as Array
 */
val Class<*>.CONSTANTS: HashMap<String, Any?>
    get() {
        val constants = HashMap<String, Any?>()
        fields(Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL).forEach { constant -> constants[constant.name] = constant[null] }
        return constants
    }
/**
 * @author sliep
 * Allows you to change the final modifier of a field at runtime
 * After setting field.[isFinal] = false you are free to edit it's value
 */
var Field.isFinal: Boolean
    get() = Modifier.isFinal(modifiers)
    set(value) {
        val accessFlags = MODIFIERS ?: return
        if (Modifier.isFinal(modifiers) != value) kotlin.runCatching {
            accessFlags[this] = modifiers and if (value) Modifier.FINAL else Modifier.FINAL.inv()
        }
    }

/************************************************************\
 ** ---------------------  METHODS  ---------------------- **
\************************************************************/

/**
 * @author sliep
 * Invoke a specific method (not only declared) of an object through reflection
 * No matter if it's not accessible
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
 * @author sliep
 * Get a method (not only declared) of a class from given name
 * @receiver the declaring class of the method
 * @param name of the method
 * @param paramsTypes method arguments types
 * @return method object
 * @throws NoSuchMethodException if a matching method is not found.
 * @see [Class.getDeclaredMethod]
 */
@Throws(NoSuchMethodException::class)
fun Class<*>.method(name: String, vararg paramsTypes: Class<*>) = methodX(name, true, *paramsTypes)

/**
 * @author sliep
 * Get a method of a class from given name
 * @receiver the declaring class of the method
 * @param name of the method
 * @param searchParent tell the function to search the method not only in the declaring class, but even in it's superclasses.
 * @param paramsTypes method arguments types
 * @return method object
 * @throws NoSuchMethodException if a matching method is not found.
 * @see [Class.getDeclaredMethod]
 */
@Throws(NoSuchMethodException::class)
fun Class<*>.methodX(name: String, searchParent: Boolean, vararg paramsTypes: Class<*>): Method {
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
 * Get all methods (not only declared) of the receiver class matching given parameters
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
            if ((modifiers == 0 || method.modifiers and modifiers == modifiers) && (excludeModifiers == 0 || method.modifiers and excludeModifiers == 0) && !response.contains(method))
                response.add(method)
        clazz = clazz.superclass ?: return response.toArray(arrayOf())
    }
}

/**
 * @author sliep
 * Makes possible to find a compatible method having only it's parameter values and the name
 * @param clazzName only for debug: the name of declaring class
 * @param members all methods in that class
 * @param name of the method
 * @param params input method arguments (null arguments are jolly, this can lead to ambiguity problems)
 * @return the matching method
 * @throws NoSuchMethodException if a matching method is not found.
 */
@Throws(NoSuchMethodException::class)
fun <M : Executable> guessFromParameters(clazzName: String, members: Array<out M>, name: String?, params: Array<out Any?>) = guessFromParametersTypes(clazzName, members, name, kotlin.Array(params.size) { i -> if (params[i] == null) null else params[i]!!::class })

/**
 * @author sliep
 * Makes possible to find a compatible method having only it's parameter types (or sub types) and the name
 * @param clazzName only for debug: the name of declaring class
 * @param members all methods in that class
 * @param name of the method
 * @param params method arguments types (null types are jolly, this can lead to ambiguity problems)
 * @return the matching method
 * @throws NoSuchMethodException if a matching method is not found.
 */
@Throws(NoSuchMethodException::class)
fun <M : Executable> guessFromParametersTypes(clazzName: String, members: Array<out M>, name: String?, params: Array<out KClass<*>?>): M {
    bob@ for (method in members) {
        if (name != null && method.name != name) continue
        val types = method.parameterTypes
        if (types.size != params.size) continue
        for (i in types.indices)
            if (params[i] != null && !types[i].kotlin.javaObjectType.isAssignableFrom(params[i]!!.javaObjectType)) continue@bob
        method.isAccessible = true
        return method
    }
    throw NoSuchMethodException(methodToString(clazzName, name
            ?: "<init>", Array(params.size) { i -> params[i]?.java }))
}

/**
 * @author sliep
 * For debug create a signature descriptor of a method
 * @param className the name of declaring class
 * @param name of the method
 * @param argTypes method arguments types
 * @return a method descriptor like java.lang.String.substring(int, int)
 */
fun methodToString(className: String, name: String, argTypes: Array<out Class<*>?>): String {
    val joiner = StringJoiner(", ", "$className.$name(", ")")
    for (i in argTypes.indices) joiner.add(argTypes[i]?.name)
    return joiner.toString()
}

/**
 * @author sliep
 * signature descriptor of a method/constructor
 * @see methodToString
 */
val Executable.signature get() = methodToString(declaringClass.name, name, parameterTypes)

/************************************************************\
 ** ---------------------  CLASSES  ---------------------- **
\************************************************************/
/**
 * @author sliep
 * Get the java primitive type of a class (e.g. [java.lang.Integer] -> [kotlin.Int])
 * an [unwrappedClass] of a non primitive class is equal to the receiver object
 */
val Class<*>.unwrappedClass: Class<*> get() = kotlin.unwrappedClass
/**
 * @author sliep
 * Get the java object type of a class (e.g. [kotlin.Int] -> [java.lang.Integer])
 * a [wrappedClass] of a non primitive class is equal to the receiver object
 */
val Class<*>.wrappedClass: Class<*> get() = kotlin.wrappedClass
/**
 * @author sliep
 * Unlike [Class.isPrimitive] this value result true even for wrapper types
 */
val Class<*>.isTypePrimitive: Boolean get() = kotlin.isTypePrimitive
/**
 * @author sliep
 * Dimensions of an array type (zero for non array types)
 */
val Class<*>.dimensions get() = name.lastIndexOf('[') + 1
/************************************************************\
 ** ---------------------  UTILITY  ---------------------- **
\************************************************************/
enum class AllocationMethod {
    NATIVE_NEW_INSTANCE {
        @Suppress("RemoveRedundantSpreadOperator")
        override fun <T> newInstance(c: Class<T>) = c.constructor(*arrayOf<Class<*>>()).newInstance()!!
    },
    UNSAFE {
        override fun <T> newInstance(c: Class<T>) = Unsafe::class.field<Unsafe>("theUnsafe").allocateInstance(c) as T
    };

    @Throws(Throwable::class)
    abstract fun <T> newInstance(c: Class<T>): T
}
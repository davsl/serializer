@file:Suppress("unused")

package sliep.jes.serializer

import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass

/* ********************************************************** *\
 ** -------------------  CONSTRUCTORS  ------------------- **
\* ********************************************************** */
/**
 * @author sliep
 * @see newInstance
 */
@Throws(NoSuchMethodException::class, UnsupportedOperationException::class, InvocationTargetException::class)
inline fun <reified T : Any> newInstance(vararg params: Any?) = T::class.java.newInstance(*params)

/**
 * @author sliep
 * @see Class.constructor
 */
@Throws(NoSuchMethodException::class)
inline fun <reified T : Any> constructor(vararg paramsTypes: KClass<*>) =
    T::class.java.constructor(*Array(paramsTypes.size) { i -> paramsTypes[i].java })

/**
 * @author sliep
 * @see Class.newUnsafeInstance
 */
@Throws(NoSuchMethodException::class, UnsupportedOperationException::class)
inline fun <reified T : Any> newUnsafeInstance() = T::class.java.newUnsafeInstance()

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
 * @author sliep
 * @see Class.fieldR
 */
@Throws(NoSuchFieldException::class)
inline fun <reified T : Any> fieldR(name: String, inParent: Boolean = false) = T::class.java.fieldR(name, inParent)

/**
 * @author sliep
 * @see Class.fields
 */
inline fun <reified T : Any> fields(modifiers: Int = 0, excludeModifiers: Int = 0) =
    T::class.java.fields(modifiers, excludeModifiers)

/**
 * @author sliep
 * @see Class.CONSTANTS
 */
val KClass<*>.CONSTANTS get() = java.CONSTANTS

/* ********************************************************** *\
 ** ---------------------  METHODS  ---------------------- **
\* ********************************************************** */

/**
 * @author sliep
 * @see Class.method
 */
@Throws(NoSuchMethodException::class)
inline fun <reified T : Any> method(name: String, vararg params: KClass<*>, searchParent: Boolean = false) =
    T::class.java.method(name, *Array(params.size) { i -> params[i].java }, searchParent = searchParent)

/**
 * @author sliep
 * @see Class.methods
 */
inline fun <reified T : Any> methods(modifiers: Int = 0, excludeModifiers: Int = 0) =
    T::class.java.methods(modifiers, excludeModifiers)

/* ********************************************************** *\
 ** ---------------------  CLASSES  ---------------------- **
\* ********************************************************** */

/**
 * @author sliep
 * @see Class.implement
 */
inline fun <reified T : Any> implement(noinline functionImplementer: T.(name: String, args: Array<out Any>) -> Any?) =
    T::class.java.implement(functionImplementer)

/**
 * @author sliep
 * @see Class.implement
 */
inline fun <reified T : Any> implement(implementer: JesImplementer<T>) = T::class.java.implement(implementer)

/**
 * @author sliep
 * @see Class.unwrappedClass
 */
val KClass<*>.unwrappedClass: Class<*> get() = javaPrimitiveType ?: java

/**
 * @author sliep
 * @see Class.wrappedClass
 */
val KClass<*>.wrappedClass: Class<*> get() = javaObjectType

/**
 * @author sliep
 * @see Class.isTypePrimitive
 */
val KClass<*>.isTypePrimitive: Boolean get() = javaPrimitiveType != null

/**
 * @author sliep
 * @see Class.dimensions
 */
val KClass<*>.dimensions: Int get() = java.dimensions

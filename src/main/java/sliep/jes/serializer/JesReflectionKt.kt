@file:Suppress("unused")

package sliep.jes.serializer

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KClass

/* ********************************************************** *\
 ** -------------------  CONSTRUCTORS  ------------------- **
\* ********************************************************** */
@Throws(NoSuchMethodException::class, UnsupportedOperationException::class, InvocationTargetException::class)
inline fun <reified T : Any> newInstance(vararg params: Any?) = T::class.java.newInstance(*params)

@Throws(NoSuchMethodException::class)
inline fun <reified T : Any> constructor(vararg paramsTypes: KClass<*>) = T::class.java.constructor(*paramsTypes)

@Throws(NoSuchMethodException::class)
fun <T> Class<T>.constructor(vararg paramsTypes: KClass<*>): Constructor<out T> =
    constructor(*Array(paramsTypes.size) { i -> paramsTypes[i].java })

@Throws(NoSuchMethodException::class)
inline fun <reified T : Any> constructor(vararg paramsTypes: Class<*>) = T::class.java.constructor(*paramsTypes)

@Throws(NoSuchMethodException::class, UnsupportedOperationException::class)
inline fun <reified T : Any> newUnsafeInstance() = T::class.java.newUnsafeInstance()

inline fun <reified T : Any> constructors(modifiers: Int = 0, excludeModifiers: Int = 0) =
    T::class.java.constructors(modifiers, excludeModifiers)

/* ********************************************************** *\
 ** ----------------------  FIELDS  ---------------------- **
\* ********************************************************** */
@Throws(NoSuchFieldException::class)
inline fun <reified T : Any> fieldR(name: String, inParent: Boolean = false) = T::class.java.fieldR(name, inParent)

inline fun <reified T : Any> fields(modifiers: Int = 0, excludeModifiers: Int = 0) =
    T::class.java.fields(modifiers, excludeModifiers)

val KClass<*>.CONSTANTS get() = java.CONSTANTS

/* ********************************************************** *\
 ** ---------------------  METHODS  ---------------------- **
\* ********************************************************** */

@Throws(NoSuchMethodException::class)
fun Class<*>.method(name: String, vararg paramsTypes: KClass<*>) = methodX(name, true, *paramsTypes)

@Throws(NoSuchMethodException::class)
inline fun <reified T : Any> method(name: String, vararg params: KClass<*>) = T::class.java.method(name, *params)

@Throws(NoSuchMethodException::class)
inline fun <reified T : Any> method(name: String, vararg params: Class<*>) = T::class.java.method(name, *params)

@Throws(NoSuchMethodException::class)
fun Class<*>.methodX(name: String, searchParent: Boolean, vararg paramsTypes: KClass<*>): Method =
    methodX(name, searchParent, *Array(paramsTypes.size) { i -> paramsTypes[i].java })

@Throws(NoSuchMethodException::class)
inline fun <reified T : Any> methodX(name: String, searchParent: Boolean, vararg paramsTypes: KClass<*>) =
    T::class.java.methodX(name, searchParent, *paramsTypes)

@Throws(NoSuchMethodException::class)
inline fun <reified T : Any> methodX(name: String, searchParent: Boolean, vararg paramsTypes: Class<*>) =
    T::class.java.methodX(name, searchParent, *paramsTypes)

inline fun <reified T : Any> methods(modifiers: Int = 0, excludeModifiers: Int = 0) =
    T::class.java.methods(modifiers, excludeModifiers)

/* ********************************************************** *\
 ** ---------------------  CLASSES  ---------------------- **
\* ********************************************************** */
inline fun <reified T : Any> implement(noinline functionImplementer: T.(name: String, args: Array<out Any>) -> Any?) =
    T::class.java.implement(functionImplementer)

inline fun <reified T : Any> implement(implementer: JesImplementer<T>) = T::class.java.implement(implementer)
val KClass<*>.unwrappedClass: Class<*> get() = javaPrimitiveType ?: java
val KClass<*>.wrappedClass: Class<*> get() = javaObjectType
val KClass<*>.isTypePrimitive: Boolean get() = javaPrimitiveType != null
val KClass<*>.dimensions: Int get() = java.dimensions

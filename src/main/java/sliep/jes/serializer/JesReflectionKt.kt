@file:Suppress("unused")

package sliep.jes.serializer

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * @author sliep
 * See JesReflection.kt for documentation
 */

/************************************************************\
 ** -------------------  CONSTRUCTORS  ------------------- **
\************************************************************/
@Throws(NoSuchMethodException::class, UnsupportedOperationException::class, InvocationTargetException::class)
fun <T : Any> KClass<T>.newInstance(vararg params: Any?) = java.newInstance(*params)

@Throws(NoSuchMethodException::class)
fun <T : Any> KClass<T>.constructor(vararg paramsTypes: KClass<*>) = java.constructor(*paramsTypes)

@Throws(NoSuchMethodException::class)
fun <T> Class<T>.constructor(vararg paramsTypes: KClass<*>): Constructor<out T> = constructor(*Array(paramsTypes.size) { i -> paramsTypes[i].java })

@Throws(NoSuchMethodException::class)
fun <T : Any> KClass<T>.constructor(vararg paramsTypes: Class<*>) = java.constructor(*paramsTypes)

@Throws(NoSuchMethodException::class, UnsupportedOperationException::class)
fun <T : Any> KClass<T>.newUnsafeInstance() = java.newUnsafeInstance()

fun <T : Any> KClass<T>.constructors(modifiers: Int = 0, excludeModifiers: Int = 0) = java.constructors(modifiers, excludeModifiers)

/************************************************************\
 ** ----------------------  FIELDS  ---------------------- **
\************************************************************/
@Throws(NoSuchFieldException::class)
fun KClass<*>.fieldR(name: String, inParent: Boolean = false) = java.fieldR(name, inParent)

fun KClass<*>.fields(modifiers: Int = 0, excludeModifiers: Int = 0) = java.fields(modifiers, excludeModifiers)

val KClass<*>.CONSTANTS get() = java.CONSTANTS

/************************************************************\
 ** ---------------------  METHODS  ---------------------- **
\************************************************************/

@Throws(NoSuchMethodException::class)
fun Class<*>.method(name: String, vararg paramsTypes: KClass<*>) = methodX(name, true, *paramsTypes)

@Throws(NoSuchMethodException::class)
fun KClass<*>.method(name: String, vararg params: KClass<*>) = java.method(name, *params)

@Throws(NoSuchMethodException::class)
fun KClass<*>.method(name: String, vararg params: Class<*>) = java.method(name, *params)

@Throws(NoSuchMethodException::class)
fun Class<*>.methodX(name: String, searchParent: Boolean, vararg paramsTypes: KClass<*>): Method = methodX(name, searchParent, *Array(paramsTypes.size) { i -> paramsTypes[i].java })

@Throws(NoSuchMethodException::class)
fun KClass<*>.methodX(name: String, searchParent: Boolean, vararg paramsTypes: KClass<*>) = java.methodX(name, searchParent, *paramsTypes)

@Throws(NoSuchMethodException::class)
fun KClass<*>.methodX(name: String, searchParent: Boolean, vararg paramsTypes: Class<*>) = java.methodX(name, searchParent, *paramsTypes)

fun KClass<*>.methods(modifiers: Int = 0, excludeModifiers: Int = 0) = java.methods(modifiers, excludeModifiers)

/************************************************************\
 ** ---------------------  CLASSES  ---------------------- **
\************************************************************/
val KClass<*>.unwrappedClass: Class<*> get() = javaPrimitiveType ?: java
val KClass<*>.wrappedClass: Class<*> get() = javaObjectType
val KClass<*>.isTypePrimitive: Boolean get() = javaPrimitiveType != null
val KClass<*>.dimensions: Int get() = java.dimensions

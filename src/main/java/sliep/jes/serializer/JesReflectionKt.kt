@file:Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER", "unused")

package sliep.jes.serializer

import kotlin.reflect.KClass

/************************************************************\
 ** -------------------  CONSTRUCTORS  ------------------- **
\************************************************************/
/** --------  FUNCTIONS  -------- **/
fun <T : Any> KClass<T>.newInstance(vararg params: Any?) = java.newInstance(*params)

fun <T : Any> KClass<T>.constructor(vararg paramsTypes: KClass<*>) = java.constructor(*paramsTypes)

fun <T : Any> KClass<T>.constructor(vararg paramsTypes: Class<*>) = java.constructor(*paramsTypes)

@Throws(UnsupportedOperationException::class)
fun <T : Any> KClass<T>.newUnsafeInstance() = java.newUnsafeInstance()

fun <T : Any> KClass<T>.constructors(modifiers: Int = 0, excludeModifiers: Int = 0) = java.constructors(modifiers, excludeModifiers)

/** --------  VARIABLES  -------- **/
inline val KClass<*>.canAllocate: Boolean
    get() = java.canAllocate

/************************************************************\
 ** ----------------------  FIELDS  ---------------------- **
\************************************************************/
/** --------  FUNCTIONS  -------- **/
@Throws(NoSuchFieldException::class)
fun KClass<*>.fieldR(name: String, inParent: Boolean = false) = java.fieldR(name, inParent)

fun KClass<*>.fields(modifiers: Int = 0, excludeModifiers: Int = 0) = java.fields(modifiers, excludeModifiers)
/** --------  VARIABLES  -------- **/
inline val KClass<*>.CONSTANTS: Array<Any?>
    get() = java.CONSTANTS

/************************************************************\
 ** ---------------------  METHODS  ---------------------- **
\************************************************************/
/** --------  FUNCTIONS  -------- **/
@Throws(NoSuchMethodException::class)
fun KClass<*>.method(name: String, vararg params: KClass<*>) = java.method(name, *params)

@Throws(NoSuchMethodException::class)
fun KClass<*>.method(name: String, vararg params: Class<*>) = java.method(name, *params)

@Throws(NoSuchMethodException::class)
fun KClass<*>.methodX(name: String, searchParent: Boolean, vararg paramsTypes: KClass<*>) = java.methodX(name, searchParent, *paramsTypes)

@Throws(NoSuchMethodException::class)
fun KClass<*>.methodX(name: String, searchParent: Boolean, vararg paramsTypes: Class<*>) = java.methodX(name, searchParent, *paramsTypes)

fun KClass<*>.methods(modifiers: Int = 0, excludeModifiers: Int = 0) = java.methods(modifiers, excludeModifiers)

/************************************************************\
 ** ---------------------  CLASSES  ---------------------- **
\************************************************************/
/** --------  VARIABLES  -------- **/
inline val KClass<*>.unwrappedClass: Class<*> get() = javaPrimitiveType ?: java
inline val KClass<*>.wrappedClass: Class<*> get() = javaObjectType
inline val KClass<*>.isTypePrimitive: Boolean get() = javaPrimitiveType != null
inline val KClass<*>.dimensions: Int get() = java.dimensions

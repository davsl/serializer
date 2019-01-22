@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "UNUSED_PARAMETER", "unused")

package sliep.jes.serializer

import kotlin.reflect.KClass

/************************************************************\
 ** -------------------  CONSTRUCTORS  ------------------- **
\************************************************************/
/** --------  FUNCTIONS  -------- **/
inline fun <T : Any> KClass<T>.newInstance(vararg params: Any?) = java.newInstance(*params)

inline fun <T : Any> KClass<T>.constructor(vararg paramsTypes: KClass<*>) = java.constructor(*paramsTypes)

inline fun <T : Any> KClass<T>.constructor(vararg paramsTypes: Class<*>) = java.constructor(*paramsTypes)

@Throws(UnsupportedOperationException::class)
inline fun <T : Any> KClass<T>.newUnsafeInstance() = java.newUnsafeInstance()

/** --------  VARIABLES  -------- **/
inline val KClass<*>.canAllocate: Boolean
    get() = java.canAllocate

/************************************************************\
 ** ----------------------  FIELDS  ---------------------- **
\************************************************************/
/** --------  FUNCTIONS  -------- **/
@Throws(NoSuchFieldException::class)
inline fun KClass<*>.fieldR(name: String, inParent: Boolean = false) = java.fieldR(name, inParent)
/************************************************************\
 ** ---------------------  METHODS  ---------------------- **
\************************************************************/
/** --------  FUNCTIONS  -------- **/
@Throws(NoSuchMethodException::class)
inline fun KClass<*>.method(name: String, vararg params: KClass<*>) = java.method(name, *params)

@Throws(NoSuchMethodException::class)
inline fun KClass<*>.method(name: String, vararg params: Class<*>) = java.method(name, *params)

@Throws(NoSuchMethodException::class)
inline fun KClass<*>.methodX(name: String, searchParent: Boolean, vararg paramsTypes: KClass<*>) = java.methodX(name, searchParent, *paramsTypes)

@Throws(NoSuchMethodException::class)
inline fun KClass<*>.methodX(name: String, searchParent: Boolean, vararg paramsTypes: Class<*>) = java.methodX(name, searchParent, *paramsTypes)

/************************************************************\
 ** ---------------------  CLASSES  ---------------------- **
\************************************************************/
/** --------  VARIABLES  -------- **/
inline val KClass<*>.unwrappedClass: Class<*> get() = javaPrimitiveType ?: java
inline val KClass<*>.wrappedClass: Class<*> get() = javaObjectType
inline val KClass<*>.isTypePrimitive: Boolean get() = javaPrimitiveType != null
inline val Any.kClazz: KClass<*>
    get() = when {
        this is Class<*> -> this.kotlin
        this is KClass<*> -> this
        else -> this::class
    }
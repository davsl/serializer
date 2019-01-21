@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "UNUSED_PARAMETER", "unused")

package sliep.jes.serializer

import sun.misc.Unsafe
import java.io.ObjectInputStream
import java.io.ObjectStreamClass
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

inline var Field.isFinal: Boolean
    get() = Modifier.isFinal(modifiers)
    set(value) {
        if (Modifier.isFinal(modifiers) != value) {
            val modifiersField: Field = kotlin.runCatching { this.field("modifiers", false) }
                    .getOrElse { kotlin.runCatching { this.field("accessFlags", false) }.getOrNull() } ?: return
            kotlin.runCatching {
                modifiersField.setInt(
                        this,
                        modifiers and if (value) Modifier.FINAL else Modifier.FINAL.inv()
                )
            }
        }
    }

inline val Class<*>.canAllocate: Boolean
    get() = !Modifier.isAbstract(modifiers)

inline val Any.unwrappedClass: Class<*> get() = this.kClass.javaPrimitiveType ?: this.kClass.java
inline val Any.wrappedClass: Class<*> get() = this.kClass.javaObjectType
inline val Any.isTypePrimitive: Boolean get() = this.kClass.javaPrimitiveType != null

inline val Any.kClass: KClass<*>
    get() = when {
        this is Class<*> -> this.kotlin
        this is KClass<*> -> this
        else -> this::class
    }

@Throws(NoSuchFieldException::class, IllegalArgumentException::class)
inline fun <R : Any> Any.getField(name: String, type: KClass<R>, searchParent: Boolean = true): R =
        getField(name, searchParent) as R

@Throws(NoSuchFieldException::class, IllegalArgumentException::class)
inline fun Any.getField(name: String, searchParent: Boolean = true): Any = field(name, searchParent).get(this)

@Throws(NoSuchFieldException::class)
inline fun Any.field(name: String, searchParent: Boolean = true): Field {
    var clazz = this.kClass.javaObjectType as Class<*>
    var firstError: Throwable? = null
    while (true)
        try {
            val field = clazz.getDeclaredField(name)
            field.isAccessible = true
            return field
        } catch (e: NoSuchFieldException) {
            if (!searchParent) throw e
            if (firstError == null) firstError = e
            clazz = clazz.superclass ?: throw firstError
        }
}

@Throws(NoSuchMethodException::class, IllegalArgumentException::class, InvocationTargetException::class)
inline fun <R : Any> Any.invokeMethod(type: KClass<R>, name: String, vararg params: Any) =
        invokeMethod(name, *params) as R

@Throws(NoSuchMethodException::class, IllegalArgumentException::class, InvocationTargetException::class)
inline fun Any.invokeMethod(name: String, vararg params: Any?): Any {
    var clazz = this.kClass.javaObjectType as Class<*>
    var firstError: Throwable? = null
    while (true)
        try {
            val method = guessMethod(clazz, name, params)
            method.isAccessible = true
            return method.invoke(this, *params)
        } catch (e: NoSuchMethodException) {
            if (firstError == null) firstError = e
            clazz = clazz.superclass ?: throw firstError
        }
}

@Throws(NoSuchMethodException::class)
inline fun Any.declaredMethod(name: String, vararg params: KClass<*>) = method(name, false, *params)

@Throws(NoSuchMethodException::class)
inline fun Any.absoluteMethod(name: String, vararg params: KClass<*>) = method(name, true, *params)

@Throws(NoSuchMethodException::class)
inline fun Any.method(name: String, searchParent: Boolean, vararg params: KClass<*>): Method {
    var clazz = this.kClass.javaObjectType as Class<*>
    val unWrappedParams = Array(params.size) { i -> params[i].java }
    while (true)
        try {
            val method = clazz.getDeclaredMethod(name, *unWrappedParams)
            method.isAccessible = true
            return method
        } catch (e: Throwable) {
            if (!searchParent) throw e
            clazz = clazz.superclass ?: throw e
        }
}

@Throws(UnsupportedOperationException::class)
fun <T> Class<T>.newUnsafeInstance(): T {
    if (!canAllocate) throw UnsupportedOperationException("Cannot allocate abstract class!")
    for (allocation in AllocationMethod.values()) {
        try {
            return allocation.newInstance(this)
        } catch (e: Throwable) {
        }
    }
    throw UnsupportedOperationException("Cannot allocate instance of type: $name")
}

@Suppress("UNCHECKED_CAST")
enum class AllocationMethod {
    NATIVE_NEW_INSTANCE {
        override fun <T> newInstance(c: Class<T>): T {
            val constructor = c.getDeclaredConstructor()
            constructor.isAccessible = true
            return constructor.newInstance()
        }
    },
    UNSAFE {
        override fun <T> newInstance(c: Class<T>): T {
            val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
            theUnsafe.isAccessible = true
            val allocateInstance = Unsafe::class.java.getDeclaredMethod("allocateInstance", Class::class.java)
            return allocateInstance.invoke(theUnsafe.get(null), c) as T
        }
    },
    OBJ_INPUT_STREAM {
        override fun <T> newInstance(c: Class<T>): T {
            val newInstance = ObjectInputStream::class.java.getDeclaredMethod(
                    "newInstance",
                    Class::class.java,
                    Class::class.java
            )
            newInstance.isAccessible = true
            return newInstance.invoke(null, c, Object::class.java) as T
        }
    },
    CONSTRUCTOR_ID {
        override fun <T> newInstance(c: Class<T>): T {
            val getConstructorId =
                    ObjectStreamClass::class.java.getDeclaredMethod("getConstructorId", Class::class.java)
            getConstructorId.isAccessible = true
            val constructorId = getConstructorId.invoke(null, Object::class.java) as Int
            val newInstance = ObjectStreamClass::class.java.getDeclaredMethod(
                    "newInstance",
                    Class::class.java,
                    Int::class.javaPrimitiveType
            )
            newInstance.isAccessible = true
            return newInstance.invoke(null, c, constructorId) as T
        }
    };

    @Throws(Exception::class)
    abstract fun <T> newInstance(c: Class<T>): T
}

@Throws(NoSuchMethodException::class)
fun guessMethod(clazz: Class<*>, name: String, params: Array<out Any?>): Method {
    bob@ for (method in clazz.declaredMethods) {
        if (method.name != name || method.parameterTypes.size != params.size) continue
        for (i in method.parameterTypes.indices)
            if (params[i] != null && !method.parameterTypes[i].kotlin.javaObjectType.isAssignableFrom(params[i]!!::class.javaObjectType)) continue@bob
        method.isAccessible = true
        return method
    }
    throw NoSuchMethodException(
            "Can't find method ${signature(
                    name,
                    Array(params.size) { i -> if (params[i] == null) null else params[i]!!::class.java })} in $clazz"
    )
}

fun signature(method: Method) = signature(method.name, method.parameterTypes)

fun signature(name: String, args: Array<out Class<*>?>) =
        if (args.isEmpty())
            "$name()"
        else {
            val builder = StringBuilder()
            for (arg in args) builder.append(arg?.name).append(", ")
            builder.delete(builder.length - 2, builder.length)
            "$name($builder)"
        }

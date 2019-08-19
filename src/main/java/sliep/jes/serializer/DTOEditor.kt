@file:Suppress("unused")

package sliep.jes.serializer

import org.json.JSONObject
import sliep.jes.serializer.impl.JesName
import sliep.jes.serializer.impl.putJesDate
import sliep.jes.serializer.impl.putJesImpl
import java.lang.reflect.AccessibleObject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

inline fun <reified T : DTOEditor> edit(callback: DTOEditorCallback) =
    edit(T::class.java, callback)

fun <T : DTOEditor> edit(clazz: Class<T>, callback: DTOEditorCallback): T = clazz.implement(object : InvocationHandler {
    private var jes = JSONObject()
    private val raw = HashMap<String, Any?>()
    private val serializer = JesSerializer()
    private val deserializer = JesDeserializer()

    private fun insert(prop: String, value: Any?, member: AccessibleObject?) {
        raw[prop] = value
        val state = if (jes.has(prop)) PropertyState.CHANGED else PropertyState.ADDED
        val key = member?.getDeclaredAnnotation(JesName::class.java)?.name ?: prop
        when {
            value == null -> jes.put(key, JSONObject.NULL)
            member != null && jes.putJesImpl(member, key, value) -> Unit
            member != null && jes.putJesDate(member, key, value) -> Unit
            else -> jes.put(key, serializer.jsonValue(value))
        }
        callback.onPropertyChanged(prop, value, state)
    }

    private fun propName(value: Any?, method: Method) = when (value) {
        is KProperty1<*, *> -> value.name
        is String -> value
        else -> throw NotImplementedError(method.name)
    }

    @Suppress("UNCHECKED_CAST")
    override fun invoke(proxy: Any, method: Method, _args: Array<*>?): Any? {
        val args = _args ?: emptyArray<Any>()
        when {
            method.isSetter -> return insert(method.propName, args[0], method)
            method.isGetter -> return raw[method.propName]
            else -> when (method.name) {
                "remove" -> {
                    val prop = propName(args[0], method)
                    val key = method.getDeclaredAnnotation(JesName::class.java)?.name ?: prop
                    raw.remove(prop)
                    jes.remove(key)
                    callback.onPropertyChanged(prop, null, PropertyState.REMOVED)
                    return Unit
                }
                "set" -> {
                    val prop = propName(args[0], method)
                    val member = try {
                        clazz.getMethodNative("set${prop.capitalizeFirst()}", args[1]?.let { it::class.java })
                    } catch (e: Throwable) {
                        null
                    }
                    insert(prop, args[1], member)
                    return Unit
                }
                "get" -> {
                    val prop = propName(args[0], method)
                    return raw[prop]
                }
                "build" -> return jes.fromJson(args[0] as Class<out JesObject>)
                "commit" -> return callback.onCommit(jes)
                "isEmpty" -> return jes.isEmpty
                "toString" -> return jes.toString()
                "clear" -> {
                    jes = JSONObject()
                    raw.clear()
                    return Unit
                }
                "sync" -> {
                    val receiver = args[0] as Any
                    val safe = args[1] as Boolean
                    val rc = receiver::class.java
                    try {
                        for (prop in jes.keys())
                            rc.getFieldNative(prop)[args[0]] = raw[prop]
                    } catch (e: Throwable) {
                        if (!safe) throw IllegalStateException("Failed to sync object $receiver", e)
                    }
                    return Unit
                }
                else -> throw NotImplementedError(method.name)
            }
        }
    }
})

interface DTOEditorCallback {
    fun onCommit(result: JSONObject)
    fun onPropertyChanged(prop: String, newValue: Any?, state: PropertyState)
}

enum class PropertyState {
    ADDED,
    CHANGED,
    REMOVED
}

inline fun <reified T : JesObject> DTOEditor.build() = build(T::class.java)

interface DTOEditor : JesObject {
    fun remove(prop: KProperty<*>): Unit = throw NotImplementedError()
    fun remove(prop: String): Unit = throw NotImplementedError()
    val isEmpty: Boolean get() = throw NotImplementedError()
    fun clear(): Unit = throw NotImplementedError()
    operator fun set(prop: KProperty<*>, value: Any): Unit = throw NotImplementedError()
    operator fun set(prop: String, value: Any): Unit = throw NotImplementedError()
    operator fun get(prop: KProperty<*>): Any = throw NotImplementedError()
    operator fun get(prop: String): Any = throw NotImplementedError()
    fun <T : JesObject> build(clazz: Class<T>): T = throw NotImplementedError()
    fun sync(obj: Any, safeMode: Boolean = false): Unit = throw NotImplementedError()
    fun commit(): Unit = throw NotImplementedError()
}

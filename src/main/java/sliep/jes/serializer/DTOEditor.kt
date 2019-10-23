@file:Suppress("unused", "UNCHECKED_CAST")

package sliep.jes.serializer

import org.json.JSONObject
import sliep.jes.serializer.impl.JesName
import sliep.jes.serializer.impl.putJesDate
import sliep.jes.serializer.impl.putJesImpl
import java.lang.reflect.AccessibleObject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KProperty

inline fun <reified T : DTOEditor> edit(callback: DTOEditorCallback) =
    edit(T::class.java, callback)

fun <T : DTOEditor> edit(clazz: Class<T>, callback: DTOEditorCallback): T = clazz.implement(object : InvocationHandler {
    private var jes = JSONObject()
    private val raw = HashMap<String, Any?>()
    private val editors = HashMap<String, DTOEditor>()
    private val serializer = JesSerializer()
    private val deserializer = JesDeserializer()

    override fun invoke(proxy: Any, method: Method, _args: Array<*>?): Any? {
        val args = _args ?: emptyArray<Any>()
        val returnType = method.returnType
        when (method.name) {
            "remove" -> {
                val prop = propName(args[0])
                val key = method.getDeclaredAnnotation(JesName::class.java)?.name ?: prop
                raw.remove(prop)
                jes.remove(key)
                callback.onPropertyChanged(clazz, prop, null, PropertyState.REMOVED)
                return Unit
            }
            "set" -> {
                val prop = propName(args[0])
                return insert(prop, args[1], suppress {
                    clazz.method(
                        "set${prop.capitalizeFirst()}",
                        args[1]?.let { it::class.java })
                })
            }
            "get" -> return get(propName(args[0]), returnType)
            "has" -> return raw.containsKey(propName(args[0]))
            "build" -> return buildJson().fromJson(args[0] as Class<out JesObject>)
            "commit" -> return callback.onCommit(buildJson())
            "isEmpty" -> {
                if (raw.isNotEmpty()) return false
                for (editor in editors.values) if (!editor.isEmpty) return false
                return true
            }
            "getJson" -> return jes
            "toString" -> return jes.toString()
            "clear" -> {
                jes = JSONObject()
                raw.clear()
                for (editor in editors.values) editor.clear()
                return Unit
            }
            "sync" -> {
                val receiver = args[0] as Any
                val safe = args[1] as Boolean
                try {
                    for (field in raw.entries) receiver.setFieldValue(field.key, field.value)
                } catch (e: Throwable) {
                    if (!safe) throw IllegalStateException("Failed to sync object $receiver", e)
                }
                for (editor in editors)
                    editor.value.sync(receiver.fieldValue(editor.key) ?: continue, safe)
                return Unit
            }
            else -> return when {
                method.isSetter -> insert(method.propName, args[0], method)
                method.isGetter -> get(method.propName, returnType)
                else -> throw NotImplementedError(method.name)
            }
        }
    }

    private fun buildJson(): JSONObject = jes.also {
        for (editor in editors) if (!editor.value.isEmpty) it.put(editor.key, editor.value.json)
    }

    private fun insert(prop: String, value: Any?, member: AccessibleObject?) {
        check(value !is DTOEditor) { "Editor properties cannot be set" }
        raw[prop] = value
        val key = member?.getDeclaredAnnotation(JesName::class.java)?.name ?: prop
        val state = if (jes.has(key)) PropertyState.CHANGED else PropertyState.ADDED
        when {
            value == null -> jes.put(key, JSONObject.NULL)
            member != null && jes.putJesImpl(member, key, value) -> Unit
            member != null && jes.putJesDate(member, key, value) -> Unit
            else -> jes.put(key, serializer.jsonValue(value))
        }
        callback.onPropertyChanged(clazz, prop, value, state)
    }

    private fun propName(value: Any?) = when (value) {
        is KProperty<*> -> value.name
        is String -> value
        else -> throw NotImplementedError()
    }

    private fun get(propName: String, returnType: Class<*>): Any? {
        if (DTOEditor::class.java.isAssignableFrom(returnType)) return editors[propName]
            ?: edit(returnType as Class<out DTOEditor>, callback).also { editors[propName] = it }
        return raw[propName]
    }
})

interface DTOEditorCallback {
    fun onCommit(result: JSONObject)
    fun onPropertyChanged(owner: Class<*>, prop: String, newValue: Any?, state: PropertyState)
}

enum class PropertyState {
    ADDED,
    CHANGED,
    REMOVED
}

inline fun <reified T : JesObject> DTOEditor.build() = build(T::class.java)

interface DTOEditor : JesObject {
    fun remove(prop: KProperty<*>)
    fun remove(prop: String)
    val isEmpty: Boolean
    val json: JSONObject
    fun clear()
    operator fun set(prop: KProperty<*>, value: Any?)
    operator fun set(prop: String, value: Any?)
    operator fun get(prop: KProperty<*>): Any?
    operator fun get(prop: String): Any?
    fun has(prop: KProperty<*>): Boolean
    fun has(prop: String): Boolean
    fun <T : JesObject> build(clazz: Class<T>): T
    fun sync(obj: Any, safeMode: Boolean = false)
    fun commit()
}

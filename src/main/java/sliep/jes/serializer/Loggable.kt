package sliep.jes.serializer

import sliep.jes.serializer.Loggable.Companion.logger
import kotlin.reflect.KClass

interface Loggable {
    companion object {
        @JvmStatic
        var logger: Logger = if (AndroidLogger.isAvailable) AndroidLogger else SysErrLogger

        @JvmStatic
        fun <L : Loggable> setLog(vararg classes: L) {
            for (clazz in classes) clazz::class.fieldR("LOG")[null] = true
        }

        @JvmStatic
        fun <L : KClass<out Loggable>> setLog(vararg classes: L) {
            for (clazz in classes) clazz.fieldR("LOG")[null] = true
        }

        @JvmStatic
        fun <L : Class<out Loggable>> setLog(vararg classes: L) {
            for (clazz in classes) clazz.fieldR("LOG")[null] = true
        }

        interface Logger {
            fun log(tag: String, message: Any)
        }

        object SysErrLogger : Logger {
            override fun log(tag: String, message: Any) = System.err.println("$tag: $message")
        }

        object AndroidLogger : Logger {
            private val Log = kotlin.runCatching { Class.forName("android.util.Log") }.getOrNull()
            val isAvailable: Boolean
                get() = Log != null

            override fun log(tag: String, message: Any) = Log!!.invokeMethod<Unit>("e", tag, message)
        }
    }
}

inline fun Loggable.log(depth: Int = -1, message: () -> Any?) {
    if (this::class.field("LOG")) logger.log(this::class.java.simpleName, spaces(this::class.java, depth) + (message()?.toString()
            ?: return))
}

inline fun KClass<out Loggable>.log(depth: Int = -1, message: () -> Any?) {
    if (this.field("LOG")) logger.log(this.java.simpleName, spaces(java, depth) + (message()?.toString()
            ?: return))
}

inline fun Class<out Loggable>.log(depth: Int = -1, message: () -> Any?) {
    if (this.field("LOG")) logger.log(this.simpleName, spaces(this, depth) + (message()?.toString()
            ?: return))
}

fun spaces(clazz: Class<*>, depth: Int): String {
    val finalDepth = if (depth == -1) {
        kotlin.runCatching { clazz.field<Int>("depth") }.getOrDefault(0)
    } else depth
    val indent = StringBuilder()
    for (i in 0 until finalDepth)
        indent.append("    ")
    return indent.toString()
}

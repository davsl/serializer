@file:Suppress("unused")

package sliep.jes.serializer

import sliep.jes.serializer.Loggable.Companion.logger
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

/**
 * Logging utility
 * 1. Implement [Loggable]
 * 2. Define a static [Boolean] named 'LOG' (e.g. LOG  = BuildConfig.DEBUG)
 * 3. (optional) Define an instance field [Int] named 'depth'
 * 4. in your Loggable class just call [log]
 * @author sliep
 */
interface Loggable {
    private val log get() = lateInit { this::class.field<Boolean>("LOG") }
    private val tag get() = lateInit { this::class.java.simpleName }
    private val depthField get() = lateInit { kotlin.runCatching { this::class.java.fieldR("depth") }.getOrNull() }

    /**
     * Log a message
     * @author sliep
     * @param depth a positive number included 0. every unit of depth correspond to 4 spaces before the [message] when printing the log to indent the data. default is -1 it means that will be taken from the 'depth' static variable in the class or zero if not defined
     * @param message a function that will be executed only if 'LOG' is true. the result of this call must be the message to print or null to print nothing
     */
    fun log(depth: Int = -1, message: String) {
        if (log) logger.log(tag, spaces(depthField, depth) + message)
    }

    companion object {
        /**
         * Edit this variable to define a custom [Logger] for your application
         * @author sliep
         */
        @JvmStatic
        var logger: Logger = if (AndroidLogger.isAvailable) AndroidLogger else SysErrLogger
    }

    /**
     * Simple interface for logging
     * @author sliep
     */
    interface Logger {
        fun log(tag: String, message: Any)
    }

    /**
     * Default java implementation of [Logger]
     *
     * Print the log to the System.err stream
     * @author sliep
     */
    object SysErrLogger : Logger {
        override fun log(tag: String, message: Any) = System.err.println("$tag: $message")
    }

    /**
     * Default android implementation of [Logger]
     *
     * Print the log to the Android error log stream
     * @author sliep
     */
    object AndroidLogger : Logger {
        private val Log = kotlin.runCatching { Class.forName("android.util.Log") }.getOrNull()
        val isAvailable = Log != null

        override fun log(tag: String, message: Any) = Log!!.invokeMethod<Unit>("e", tag, message)
    }
}

/**
 * Allows you to log as a [Loggable] from outside it's class
 * @author sliep
 * @see Loggable.log
 */
fun KClass<out Loggable>.log(depth: Int = -1, message: () -> Any?) {
    if (field("LOG")) {
        val theMessage = message()?.toString() ?: return
        val depthField = kotlin.runCatching { java.fieldR("depth") }.getOrNull()
        logger.log(this.java.simpleName, null.spaces(depthField, depth) + theMessage)
    }
}

/**
 * Log a message
 * @author sliep
 * @see Loggable.log
 */
fun Loggable.log(depth: Int = -1, message: () -> Any?) {
    log(depth, message()?.toString() ?: return)
}

/**
 * Create spaces to indent messages
 * @author sliep
 * @receiver the loggable to get depth or null for singletons
 * @param depthField if exists
 * @param depth override value from call if not -1 (default)
 * @return spaces string
 */
private fun Loggable?.spaces(depthField: Field?, depth: Int): String {
    if (depthField != null && this == null && !Modifier.isStatic(depthField.modifiers)) throw IllegalStateException("Can't get depth from outside the instance")
    val finalDepth = if (depth == -1) depthField?.getInt(this) ?: 0 else depth
    val indent = StringBuilder()
    for (i in 0 until finalDepth) indent.append("    ")
    return indent.toString()
}
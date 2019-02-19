package sliep.jes.serializer

import sliep.jes.serializer.Loggable.Companion.logger
import java.lang.reflect.Field
import kotlin.reflect.KClass

/**
 * @author sliep
 * Logging utility
 * 1. Implement [Loggable]
 * 2. Define a static [Boolean] named 'LOG'
 * 3. (optional) Define a static [Int] named 'depth'
 * 4. in your Loggable class just call [log]
 */
interface Loggable {
    private val log get() = lateInit { this::class.field<Boolean>("LOG") }
    private val tag get() = lateInit { this::class.java.simpleName }
    private val depthField get() = lateInit { kotlin.runCatching { this::class.fieldR("depth") }.getOrNull() }

    /**
     * @author sliep
     * Log a message
     * @param depth a positive number included 0. every unit of depth correspond to 4 spaces before the [message] when printing the log to indent the data. default is -1 it means that will be taken from the 'depth' static variable in the class or zero if not defined
     * @param message a function that will be executed only if 'LOG' is true. the result of this call must be the message to print or null to print nothing
     */
    fun log(depth: Int = -1, message: () -> Any?) {
        if (log) {
            val theMessage = message()?.toString() ?: return
            logger.log(tag, spaces(depthField, depth) + theMessage)
        }
    }

    companion object {
        /**
         * @author sliep
         * Edit this variable to define a custom [Logger] for your application
         */
        @JvmStatic
        var logger: Logger = if (AndroidLogger.isAvailable) AndroidLogger else SysErrLogger
    }

    /**
     * @author sliep
     * Simple interface for logging
     */
    interface Logger {
        fun log(tag: String, message: Any)
    }

    /**
     * @author sliep
     * Default java implementation of [Logger]
     * Print the log to the System.err stream
     */
    object SysErrLogger : Logger {
        override fun log(tag: String, message: Any) = System.err.println("$tag: $message")
    }

    /**
     * @author sliep
     * Default android implementation of [Logger]
     * Print the log to the Android error log stream
     */
    object AndroidLogger : Logger {
        private val Log = kotlin.runCatching { Class.forName("android.util.Log") }.getOrNull()
        val isAvailable = Log != null

        override fun log(tag: String, message: Any) = Log!!.invokeMethod<Unit>("e", tag, message)
    }
}

/**
 * @author sliep
 * Allows you to log as a [Loggable] from outside it's class
 * @see [Loggable.log]
 */
fun KClass<out Loggable>.log(depth: Int = -1, message: () -> Any?) {
    if (field("LOG")) {
        val theMessage = message()?.toString() ?: return
        val depthField = kotlin.runCatching { fieldR("depth") }.getOrNull()
        logger.log(this.java.simpleName, spaces(depthField, depth) + theMessage)
    }
}

/**
 * @author sliep
 * Create spaces to indent messages
 * @param depthField if exists
 * @param depth override value from call if not -1 (default)
 * @return spaces string
 */
private fun spaces(depthField: Field?, depth: Int): String {
    val finalDepth = if (depth == -1) depthField?.getInt(null) ?: 0 else depth
    val indent = StringBuilder()
    for (i in 0 until finalDepth) indent.append("    ")
    return indent.toString()
}
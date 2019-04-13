@file:Suppress("unused")

package sliep.jes.serializer

import sliep.jes.serializer.Loggable.Companion.logger
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
    var depth: Int
        get() = depths[this] ?: 0
        set(value) {
            depths[this] = value
        }
    private val logIt: Boolean get() = lateInit(::logIt) { this::class.field("LOG") }
    private val tag: String
        get() = lateInit(::tag) {
            val simpleName = this::class.java.simpleName
            if (simpleName.isBlank()) "<unknown-class>" else simpleName
        }

    /**
     * Log a message
     * @author sliep
     * @param depth a positive number included 0. every unit of depth correspond to 4 spaces before the [message] when printing the log to indent the data. default is -1 it means that will be taken from the 'depth' static variable in the class or zero if not defined
     * @param message a function that will be executed only if 'LOG' is true. the result of this call must be the message to print or null to print nothing
     */
    fun log(depth: Int = -1, message: () -> Any?) {
        if (logIt) logger.log(tag, spaces(if (depth == -1) this.depth else depth) + (message() ?: return))
    }

    companion object {
        private val depths = HashMap<Loggable, Int>()
        /**
         * Edit this variable to define a custom [Logger] for your application
         * @author sliep
         */
        @JvmStatic
        var logger: Logger = if (AndroidLogger.isAvailable) AndroidLogger else SysErrLogger

        fun indentAll(content: String, startIndent: Int, tagOffset: Int): String {
            val spaces = StringBuilder().apply {
                for (i in 0 until startIndent + tagOffset) append(' ')
            }.toString()
            return content.replace("\n", "\n" + spaces)
        }
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
        override fun log(tag: String, message: Any) {
            val mess = message.toString()
            var spacesCount = 0
            while (mess[spacesCount++] == ' ');
            System.err.println("$tag: ${indentAll(mess, spacesCount, tag.length + 1)}")
        }
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

        override fun log(tag: String, message: Any) {
            val mess = message.toString()
            var spacesCount = 0
            while (mess[spacesCount++] == ' ');
            Log!!.invokeMethod<Unit>("e", tag, indentAll(mess, spacesCount, tag.length + 1))
        }
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
        logger.log(this.java.simpleName, null.spaces(depth) + theMessage)
    }
}

/**
 * Create spaces to indent messages
 * @author sliep
 * @receiver the loggable to get depth or null for singletons
 * @param depth override value from call if not -1 (default)
 * @return spaces string
 */
private fun Loggable?.spaces(depth: Int): String = StringBuilder().apply {
    for (i in 0 until depth) append("    ")
}.toString()
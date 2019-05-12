@file:Suppress("unused")

package sliep.jes.serializer

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
    val logEnabled: Boolean
    val tag: String get() = this::class.java.simpleName

    fun log(message: () -> Any?) {
        if (logEnabled) logger.log(tag, message() ?: return, depth)
    }

    companion object {
        /**
         * Edit this variable to define a custom [Logger] for your application
         * @author sliep
         */
        @JvmStatic
        var logger: Logger = if (AndroidLogger.isAvailable) AndroidLogger else SysErrLogger
    }

    interface Logger {
        fun log(tag: String, message: Any, indent: Int)
    }
}

private fun buildLog(message: Any, indent: Int): String? {
    val mess = message.toString()
    if (mess.isBlank()) return null
    val spaces = spaces(indent * 4)
    return spaces + mess.replace("\n", "\n" + spaces)
}

object SysErrLogger : Loggable.Logger {
    override fun log(tag: String, message: Any, indent: Int) {
        val mess = buildLog(message, indent) ?: return
        System.err.println("$tag:$mess")
    }
}

object AndroidLogger : Loggable.Logger {
    private val e = try {
        Class.forName("android.util.Log").method("e", String::class.java, String::class.java)
    } catch (e: Throwable) {
        null
    }
    val isAvailable = e != null

    override fun log(tag: String, message: Any, indent: Int) {
        val mess = buildLog(message, indent) ?: return
        e!!(null, tag, mess)
    }
}

private fun spaces(spacesCount: Int): String = StringBuilder().apply {
    for (i in 0 until spacesCount) append(' ')
}.toString()
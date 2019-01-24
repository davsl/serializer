package sliep.jes.serializer

interface Loggable {
    private val log: Boolean
        get() = try {
            this::class.field("LOG")
        } catch (e: Throwable) {
            throw IllegalStateException("Classes that implements Loggable must define a STATIC variable named LOG")
        }
    val depth: Int
        get() = 0
    val spaces: String
        get() {
            val indent = StringBuilder()
            for (i in 0 until JesSerializer.depth)
                indent.append("    ")
            return indent.toString()
        }

    fun log(message: () -> Any?) {
        if (log) logger.log(this::class.java.simpleName, spaces + (message()?.toString() ?: return))
    }

    companion object {
        @JvmStatic
        var logger: Logger = if (AndroidLogger.isAvailable) AndroidLogger else SysErrLogger

        @JvmStatic
        fun <L : Loggable> setLog(vararg classes: L) {
            for (clazz in classes) clazz::class.fieldR("LOG")[null] = true
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
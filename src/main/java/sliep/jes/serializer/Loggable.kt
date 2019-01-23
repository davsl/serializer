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
        if (log) logger(this::class.java.simpleName, spaces + (message()?.toString() ?: return))
    }

    companion object {
        @JvmStatic
        var logger: (String, String) -> Unit = { tag, message ->
            System.err.println("$tag: $message")
        }

        @JvmStatic
        fun <L : Loggable> setLog(vararg classes: L) {
            for (clazz in classes) clazz::class.fieldR("LOG")[null] = true
        }
    }
}
package sliep.jes.serializer

@Suppress("PropertyName")
interface Loggable {
    var LOG: Boolean
    var depth: Int
    val spaces: String
        get() {
            val indent = StringBuilder()
            for (i in 0 until JesSerializer.depth)
                indent.append("    ")
            return indent.toString()
        }

    fun log(message: () -> Any?) {
        if (LOG) logger(this::class.java.simpleName, spaces + (message()?.toString() ?: return))
    }

    companion object {
        @JvmStatic
        var logger: (String, String) -> Unit = { tag, message ->
            System.err.println("$tag: $message")
        }

        @JvmStatic
        fun <L : Loggable> setLog(vararg classes: L) {
            for (clazz in classes) clazz.LOG = true
        }
    }
}
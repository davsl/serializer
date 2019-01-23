package sliep.jes.serializer

@Suppress("PropertyName")
open class Loggable {
    var LOG: Boolean = false

    fun log(message: () -> Any?) {
        if (LOG) logger(this::class.java.simpleName, message()?.toString() ?: return)
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
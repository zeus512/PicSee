@file:SuppressWarnings("FunctionMinLength")

package com.darkbyte.groupit.logger

private const val LOG_TAG_SEARCH_DEPTH = 4
private const val SOURCE_CLASS_NAME_AFTER = "log"

/**
 * A logger implementation which supports logging for Android and JVM Modules as well.
 */
object Logger {

    private val frameworkLoggers = mutableSetOf<FrameworkLogger>()

    /**
     * initialize logger with [FrameworkLogger] to print messages as per platform.
     */
    fun init(frameworkLogger: FrameworkLogger) {
        frameworkLoggers.add(frameworkLogger)
    }

    /**
     * deInitialize logger
     */
    fun deInit() {
        frameworkLoggers.clear()
    }

    /**
     * Send a [LogLevel.DEBUG] message to logger.
     *
     * @param msg a nullable object which can be anything.
     */
    @JvmStatic
    fun d(msg: Any?) = log(LogLevel.DEBUG, msg.toLog())

    /**
     * Send a [LogLevel.ERROR] message to logger.
     *
     * @param msg a nullable object which can be anything.
     */
    fun e(msg: Any?) = log(LogLevel.ERROR, msg.toLog())

    /**
     * Send a [LogLevel.INFO] message to logger.
     *
     * @param msg a nullable object which can be anything.
     */
    fun i(msg: Any?) = log(LogLevel.INFO, msg.toLog())

    /**
     * Send a [LogLevel.VERBOSE] message to logger.
     *
     * @param msg a nullable object which can be anything.
     */
    fun v(msg: Any?) = log(LogLevel.VERBOSE, msg.toLog())

    /**
     * Send a [LogLevel.WARN] message to logger.
     *
     * @param msg a nullable object which can be anything.
     */
    fun w(msg: Any?) = log(LogLevel.WARN, msg.toLog())

    /**
     * Send a [LogLevel.DEBUG] message to logger.
     *
     * can be applied to any nullable object as an extension.
     */
    fun Any?.log() = d(this)

    private fun log(level: LogLevel, msg: Any?) {
        if (frameworkLoggers.isEmpty()) return
        val elements = Thread.currentThread().stackTrace
        val tag = elements[findIndex(elements)].className.substringAfterLast(".")
        frameworkLoggers.forEach {
            it.log(level, tag, msg.toString())
        }
    }

    private fun findIndex(elements: Array<StackTraceElement>): Int {
        var index = LOG_TAG_SEARCH_DEPTH
        while (index < elements.size) {
            val className = elements[index].className
            if (className != Logger::class.java.name &&
                elements[index].methodName.startsWith(SOURCE_CLASS_NAME_AFTER).not()
            ) {
                return index
            }
            index++
        }
        return 0
    }

    private fun Any?.toLog(): String = when (this) {
        is Throwable -> stackTraceToString()
        else -> toString()
    }
}
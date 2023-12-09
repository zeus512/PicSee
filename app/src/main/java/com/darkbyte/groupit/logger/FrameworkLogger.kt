package com.darkbyte.groupit.logger

/**
 * An interface to print actual log as per platform implementation.
 */
interface FrameworkLogger {
    /**
     * log the message with help of custom framework implementation which can be different per platform.
     */
    fun log(level: LogLevel, tag: String, msg: String)
}
package com.darkbyte.groupit.logger

/**
 * Priority constants for the logging method
 */
enum class LogLevel(val id: Int) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6),
}
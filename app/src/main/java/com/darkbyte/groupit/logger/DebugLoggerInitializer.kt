package com.darkbyte.groupit.logger

import android.content.Context
import android.util.Log
import androidx.startup.Initializer

class DebugLoggerInitializer : Initializer<Unit>, FrameworkLogger {
    override fun create(context: Context) = Logger.init(frameworkLogger = this)

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()

    override fun log(level: LogLevel, tag: String, msg: String) {
        Log.println(level.id, tag, msg)
    }
}
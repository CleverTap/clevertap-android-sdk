package com.clevertap.android.sdk

class TestLogger : ILogger {
    override fun debug(message: String?) {
        println("$message")
    }

    override fun debug(suffix: String?, message: String?) {
        println("$suffix - $message")
    }

    override fun debug(suffix: String?, message: String?, t: Throwable?) {
        println("$suffix - $message - ${t?.printStackTrace()}")
    }

    override fun debug(message: String?, t: Throwable?) {
        println("$message - ${t?.printStackTrace()}")
    }

    override fun info(message: String?) {
        println("$message")
    }

    override fun info(suffix: String?, message: String?) {
        println("$suffix - $message")
    }

    override fun info(suffix: String?, message: String?, t: Throwable?) {
        println("$suffix - $message - ${t?.printStackTrace()}")
    }

    override fun info(message: String?, t: Throwable?) {
        println("$message - ${t?.printStackTrace()}")
    }

    override fun verbose(message: String?) {
        println("$message")
    }

    override fun verbose(suffix: String?, message: String?) {
        println("$suffix - $message")
    }

    override fun verbose(suffix: String?, message: String?, t: Throwable?) {
        println("$suffix - $message - ${t?.printStackTrace()}")
    }

    override fun verbose(message: String?, t: Throwable?) {
        println("$message - ${t?.printStackTrace()}")
    }
}
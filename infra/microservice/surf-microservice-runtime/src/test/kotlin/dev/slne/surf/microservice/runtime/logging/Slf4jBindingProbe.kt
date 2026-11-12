package dev.slne.surf.microservice.runtime.logging

import org.slf4j.LoggerFactory

/**
 * Prints the class of the SLF4J logger factory bound in a fresh JVM; with [PIN_ARGUMENT] it pins the provider first.
 */
object Slf4jBindingProbe {
    const val PIN_ARGUMENT = "--pin"

    @JvmStatic
    fun main(args: Array<String>) {
        if (PIN_ARGUMENT in args) Slf4jProvider.selectLog4j()
        println(LoggerFactory.getILoggerFactory().javaClass.name)
    }
}

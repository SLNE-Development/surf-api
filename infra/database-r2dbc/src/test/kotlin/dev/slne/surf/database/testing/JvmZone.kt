package dev.slne.surf.database.testing

import kotlinx.coroutines.runBlocking
import java.util.TimeZone

/**
 * Runs [block] with the JVM default time zone set to [zone] and restores the previous zone afterwards.
 *
 * The default zone is global to the JVM, so callers must not run concurrently with other tests that read it.
 */
fun withJvmZone(zone: String, block: suspend () -> Unit) {
    val original = TimeZone.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone(zone))
    try {
        runBlocking { block() }
    } finally {
        TimeZone.setDefault(original)
    }
}

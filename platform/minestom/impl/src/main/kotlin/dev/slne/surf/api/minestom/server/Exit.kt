package dev.slne.surf.api.minestom.server

import dev.slne.surf.api.minestom.server.impl.SurfMinestomRuntime

/**
 * Stops the server, flushes the logs and terminates the JVM with [status].
 *
 * Stops the surf api and Minestom with [SurfMinestomServer.stop] when the api started, which also
 * runs the shutdown actions of an api that Minestom already shut down. The JVM shutdown hook of
 * the surf api flushes Log4j after it ran; when the api never started, e.g. after a failed start,
 * the logs are flushed here. Concurrent and later calls terminate the JVM with the status of the
 * first call.
 *
 * Blocks until the server is stopped; on a daemon thread, the termination runs on a non-daemon
 * thread, so that the JVM cannot end with status 0 first. Must not be called from a tick thread,
 * from within the start, from code the stop runs (a shutdown action, the `disable` of a feature,
 * the `onDisable` of a plugin, or code they wait for) or while the JVM shuts down.
 * [SurfMinestomServer.exit] can be called from all of these except the last, on a server
 * reference captured before the stop.
 *
 * @throws IllegalStateException when called from a tick thread, from within the start or the stop,
 * or while the JVM shuts down; the process keeps running
 */
fun exitFlushingLogs(status: Int): Nothing = SurfMinestomRuntime.exitProcess(status)

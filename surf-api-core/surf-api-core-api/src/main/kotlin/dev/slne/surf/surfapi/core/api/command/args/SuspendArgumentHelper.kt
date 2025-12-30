package dev.slne.surf.surfapi.core.api.command.args

import dev.jorel.commandapi.executors.CommandArguments
import kotlinx.coroutines.Deferred

/**
 * Awaits the value of a command argument that is represented as a [Deferred].
 *
 * If no argument with the given [nodeName] exists, this function returns `null`.
 * This is the nullable / optional variant of [awaiting].
 *
 * @param nodeName The name of the argument node.
 * @return The resolved argument value, or `null` if the argument is not present.
 */
suspend inline fun <T> CommandArguments.awaitingOrNull(nodeName: String): T? {
    val deferred = getUnchecked<Deferred<T>>(nodeName) ?: return null
    return deferred.await()
}

/**
 * Awaits the value of a required command argument that is represented as a [Deferred].
 *
 * If no argument with the given [nodeName] exists, an exception is thrown.
 * This is the non-nullable / required variant of [awaitingOrNull].
 *
 * @param nodeName The name of the argument node.
 * @return The resolved argument value.
 * @throws IllegalStateException If the argument is missing.
 */
suspend inline fun <T> CommandArguments.awaiting(nodeName: String): T {
    return awaitingOrNull(nodeName) ?: error("Argument $nodeName is missing")
}
package dev.slne.surf.surfapi.bukkit.api.command.util

import com.destroystokyo.paper.profile.PlayerProfile
import dev.jorel.commandapi.executors.CommandArguments
import kotlinx.coroutines.future.await
import java.util.*
import java.util.concurrent.CompletableFuture

suspend fun CommandArguments.awaitAsyncPlayerProfileOptional(
    nodeName: String,
): PlayerProfile? {
    val future = getUnchecked<CompletableFuture<List<PlayerProfile>>>(nodeName) ?: return null

    try {
        val profiles = future.await()
        return profiles.first()
    } catch (e: RuntimeException) {
        throw e.cause ?: e
    }
}

suspend fun CommandArguments.awaitAsyncPlayerProfile(nodeName: String): PlayerProfile =
    awaitAsyncPlayerProfileOptional(nodeName) ?: error("Argument '$nodeName' not found")

fun PlayerProfile.idOrThrow(): UUID = id ?: error("PlayerProfile does not provide a uuid")

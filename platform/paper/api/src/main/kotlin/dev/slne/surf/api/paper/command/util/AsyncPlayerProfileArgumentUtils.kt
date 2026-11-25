package dev.slne.surf.api.paper.command.util

import com.destroystokyo.paper.profile.PlayerProfile
import dev.jorel.commandapi.executors.CommandArguments
import kotlinx.coroutines.future.await
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

suspend fun CommandArguments.awaitAsyncPlayerProfile(nodeName: String): PlayerProfile {
    return awaitAsyncPlayerProfileOptional(nodeName) ?: error("Argument '$nodeName' not found")
}

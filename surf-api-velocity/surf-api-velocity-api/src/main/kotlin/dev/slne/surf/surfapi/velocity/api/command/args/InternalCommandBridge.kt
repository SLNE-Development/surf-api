package dev.slne.surf.surfapi.velocity.api.command.args

import com.velocitypowered.api.proxy.Player
import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import dev.slne.surf.surfapi.core.api.util.requiredService
import kotlinx.coroutines.Deferred
import java.util.*

@InternalSurfApi
interface InternalCommandBridge {
    fun getPlayer(name: String): Player?
    fun getPlayer(uuid: UUID): Player?
    fun getPlayers(): Collection<Player>

    suspend fun requestPlayerUuid(name: String): UUID?

    fun <T> async(block: suspend () -> T): Deferred<T>

    companion object : InternalCommandBridge by requiredService<InternalCommandBridge>()
}
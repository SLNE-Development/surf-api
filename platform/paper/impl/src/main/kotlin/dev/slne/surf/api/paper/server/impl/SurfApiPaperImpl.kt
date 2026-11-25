package dev.slne.surf.api.paper.server.impl

import com.google.common.io.ByteStreams
import dev.slne.surf.api.core.server.impl.SurfApiCoreImpl
import dev.slne.surf.api.paper.SurfApiPaper
import dev.slne.surf.api.paper.server.plugin
import dev.slne.surf.api.paper.server.time.TimeHandler
import dev.slne.surf.api.paper.time.SkipOperations
import dev.slne.surf.api.paper.time.TimeSkipResult
import io.papermc.paper.ServerBuildInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.springframework.stereotype.Service
import java.util.*

@Service
class SurfApiPaperImpl : SurfApiCoreImpl(), SurfApiPaper {

    private val buildInfo = ServerBuildInfo.buildInfo()
    override val isFolia: Boolean = buildInfo.isBrandCompatible(Key.key("papermc", "folia"))
    override val isCanvasMc: Boolean = buildInfo.isBrandCompatible(Key.key("canvasmc", "canvas"))

    override fun sendPlayerToServer(playerUuid: UUID, server: String) {
        val player = Bukkit.getPlayer(playerUuid)

        if (player != null) {
            val out = ByteStreams.newDataOutput()
            out.writeUTF("Connect")
            out.writeUTF(server)

            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
        }
    }

    override fun getPlayer(playerUuid: UUID): Player? = Bukkit.getPlayer(playerUuid)
    override fun isPlayer(audience: Audience): Boolean {
        return audience is Player
    }

    val dataFolder get() = plugin.dataPath

    override suspend fun skipTimeSmoothly(world: World, timeToAdd: Long): TimeSkipResult {
        return skipTimeSmoothly(world, timeToAdd, timeToAdd / TimeHandler.DEFAULT_SKIP_AMOUNT)
    }

    override suspend fun skipTimeSmoothly(
        world: World,
        timeToAdd: Long,
        duration: Long,
    ): TimeSkipResult = TimeHandler.skipTimeSmoothly(world, timeToAdd, duration)

    override suspend fun skipTimeSmoothly(
        world: World,
        skipOperation: SkipOperations.SkipOperation,
    ): TimeSkipResult {
        val timeToAdd = skipOperation.timeToAdd(world)

        return skipTimeSmoothly(world, timeToAdd, timeToAdd / TimeHandler.DEFAULT_SKIP_AMOUNT)
    }

    override suspend fun skipTimeSmoothly(timeToAdd: Long): Map<World, TimeSkipResult> {
        return coroutineScope {
            val worlds = Bukkit.getWorlds()
            worlds.associateWith {
                async {
                    skipTimeSmoothly(it, timeToAdd)
                }
            }.mapValues { (_, def) -> def.await() }
        }
    }

    override suspend fun skipTimeSmoothly(
        timeToAdd: Long,
        duration: Long,
    ): Map<World, TimeSkipResult> {
        return coroutineScope {
            val worlds = Bukkit.getWorlds()
            worlds.associateWith {
                async {
                    skipTimeSmoothly(it, timeToAdd, duration)
                }
            }.mapValues { (_, def) -> def.await() }
        }
    }

    override suspend fun skipTimeSmoothly(
        skipOperation: SkipOperations.SkipOperation,
    ): Map<World, TimeSkipResult> {
        return coroutineScope {
            val worlds = Bukkit.getWorlds()
            worlds.associateWith {
                async {
                    skipTimeSmoothly(it, skipOperation)
                }
            }.mapValues { (_, def) -> def.await() }
        }
    }
}

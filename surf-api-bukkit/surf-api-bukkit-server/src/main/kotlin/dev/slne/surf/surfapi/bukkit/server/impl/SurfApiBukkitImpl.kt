package dev.slne.surf.surfapi.bukkit.server.impl

import com.google.auto.service.AutoService
import com.google.common.io.ByteStreams
import dev.slne.surf.surfapi.bukkit.api.SurfApiBukkit
import dev.slne.surf.surfapi.bukkit.api.scoreboard.ObsoleteScoreboardApi
import dev.slne.surf.surfapi.bukkit.api.time.SkipOperations.SkipOperation
import dev.slne.surf.surfapi.bukkit.api.time.TimeSkipResult
import dev.slne.surf.surfapi.bukkit.server.hook.SurfBukkitHookManager
import dev.slne.surf.surfapi.bukkit.server.impl.scoreboard.SurfScoreboardBuilderImpl
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.bukkit.server.time.TimeHandler
import dev.slne.surf.surfapi.core.api.SurfApiCore
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.server.impl.SurfApiCoreImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.World
import java.util.*

@AutoService(SurfApiCore::class)
class SurfApiBukkitImpl : SurfApiCoreImpl(), SurfApiBukkit {

    init {
        checkInstantiationByServiceLoader()
    }

    fun onEnable() {
        SurfBukkitHookManager.onEnable()
    }

    override val isFolia: Boolean by lazy { runCatching { Class.forName("io.papermc.paper.threadedregions.RegionizedServer") }.isSuccess }
    override val isCanvasMc: Boolean by lazy { runCatching { Class.forName("io.canvasmc.canvas.event.EntityPortalAsyncEvent") }.isSuccess }

    @ObsoleteScoreboardApi
    override fun scoreboardLibrary() = plugin.getScoreboardLibrary()

    @ObsoleteScoreboardApi
    override fun createScoreboard(title: Component) = SurfScoreboardBuilderImpl(title)

    override fun sendPlayerToServer(playerUuid: UUID, server: String) {
        val player = Bukkit.getPlayer(playerUuid)

        if (player != null) {
            val out = ByteStreams.newDataOutput()
            out.writeUTF("Connect")
            out.writeUTF(server)

            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
        }
    }

    override fun getPlayer(playerUuid: UUID) = Bukkit.getPlayer(playerUuid)

    val dataFolder get() = plugin.dataPath

    override suspend fun skipTimeSmoothly(world: World, timeToAdd: Long) =
        skipTimeSmoothly(world, timeToAdd, timeToAdd / TimeHandler.DEFAULT_SKIP_AMOUNT)


    override suspend fun skipTimeSmoothly(
        world: World,
        timeToAdd: Long,
        duration: Long,
    ) = TimeHandler.skipTimeSmoothly(world, timeToAdd, duration)

    override suspend fun skipTimeSmoothly(
        world: World,
        skipOperation: SkipOperation,
    ): TimeSkipResult {
        val timeToAdd = skipOperation.timeToAdd(world)
        return skipTimeSmoothly(world, timeToAdd, timeToAdd / TimeHandler.DEFAULT_SKIP_AMOUNT)
    }

    override suspend fun skipTimeSmoothly(timeToAdd: Long) = coroutineScope {
        val worlds = Bukkit.getWorlds()
        worlds.associateWithTo(mutableObject2ObjectMapOf(worlds.size)) {
            async {
                skipTimeSmoothly(it, timeToAdd)
            }
        }.mapValuesTo(mutableObject2ObjectMapOf(worlds.size)) { (_, def) -> def.await() }
    }

    override suspend fun skipTimeSmoothly(
        timeToAdd: Long,
        duration: Long,
    ) = coroutineScope {
        val worlds = Bukkit.getWorlds()
        worlds.associateWithTo(mutableObject2ObjectMapOf(worlds.size)) {
            async {
                skipTimeSmoothly(it, timeToAdd, duration)
            }
        }.mapValuesTo(mutableObject2ObjectMapOf(worlds.size)) { (_, def) -> def.await() }
    }

    override suspend fun skipTimeSmoothly(
        skipOperation: SkipOperation,
    ) = coroutineScope {
        val worlds = Bukkit.getWorlds()
        worlds.associateWithTo(mutableObject2ObjectMapOf(worlds.size)) {
            async {
                skipTimeSmoothly(it, skipOperation)
            }
        }.mapValuesTo(mutableObject2ObjectMapOf(worlds.size)) { (_, def) -> def.await() }
    }
}

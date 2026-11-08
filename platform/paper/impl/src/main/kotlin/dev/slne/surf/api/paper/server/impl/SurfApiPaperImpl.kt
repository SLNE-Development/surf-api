package dev.slne.surf.api.paper.server.impl

import com.google.auto.service.AutoService
import com.google.common.io.ByteStreams
import dev.slne.surf.api.core.SurfApiCore
import dev.slne.surf.api.core.server.impl.SurfApiCoreImpl
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.paper.SurfApiPaper
import dev.slne.surf.api.paper.server.hook.SurfBukkitHookManager
import dev.slne.surf.api.paper.server.plugin
import dev.slne.surf.api.paper.server.time.TimeHandler
import dev.slne.surf.api.paper.time.SkipOperations
import dev.slne.surf.api.paper.time.TimeSkipResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*

@AutoService(SurfApiCore::class)
class SurfApiPaperImpl : SurfApiCoreImpl(), SurfApiPaper {

    init {
        checkInstantiationByServiceLoader()
    }

    fun onEnable() {
        SurfBukkitHookManager.onEnable()
    }

    override val isFolia: Boolean by lazy { runCatching { Class.forName("io.papermc.paper.threadedregions.RegionizedServer") }.isSuccess }
    override val isCanvasMc: Boolean by lazy { runCatching { Class.forName("io.canvasmc.canvas.event.EntityPortalAsyncEvent") }.isSuccess }

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
    override fun isPlayer(audience: Audience): Boolean {
        return audience is Player
    }

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
        skipOperation: SkipOperations.SkipOperation,
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
        skipOperation: SkipOperations.SkipOperation,
    ) = coroutineScope {
        val worlds = Bukkit.getWorlds()
        worlds.associateWithTo(mutableObject2ObjectMapOf(worlds.size)) {
            async {
                skipTimeSmoothly(it, skipOperation)
            }
        }.mapValuesTo(mutableObject2ObjectMapOf(worlds.size)) { (_, def) -> def.await() }
    }
}

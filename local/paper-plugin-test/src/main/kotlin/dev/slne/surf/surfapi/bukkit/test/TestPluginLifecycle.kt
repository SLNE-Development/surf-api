package dev.slne.surf.surfapi.bukkit.test

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.destroystokyo.paper.event.server.ServerTickStartEvent
import dev.jorel.commandapi.CommandAPI
import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.paper.event.listen
import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.SurfPaperPacketListenerApi
import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.surfapi.bukkit.test.command.SurfApiTestCommand
import dev.slne.surf.surfapi.bukkit.test.command.dialog.dialogTestCommand
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.TestInventoryView
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.testInventoryViewDsl
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.testPaginatedViewDsl
import dev.slne.surf.surfapi.bukkit.test.config.MyPluginConfig
import dev.slne.surf.surfapi.bukkit.test.config.randomiseModernTestConfig
import dev.slne.surf.surfapi.bukkit.test.listener.ChatListener
import kotlinx.coroutines.CoroutineScope
import net.minecraft.server.MinecraftServer
import org.bukkit.inventory.ItemType
import org.bukkit.plugin.Plugin
import org.springframework.stereotype.Service
import kotlin.concurrent.thread

/**
 * Sets up the test plugin's commands, views and listeners in the plugin's lifecycle phases.
 */
@Service
@OptIn(NmsUseWithCaution::class)
class TestPluginLifecycle(
    private val plugin: Plugin,
    private val packetListeners: SurfPaperPacketListenerApi,
    private val pluginScope: CoroutineScope,
    private val myPluginConfig: ConfigHandle<MyPluginConfig>,
) : SurfLifecycle {

    override suspend fun onLoad() {
        randomiseModernTestConfig()

        packetListeners.registerListeners(ChatListener())
        TestInventoryView.register()
        testInventoryViewDsl.register()
        testPaginatedViewDsl.register()
    }

    override suspend fun onEnable() {
        SurfApiTestCommand().register()
        dialogTestCommand(pluginScope)

        plugin.logger.info("Running release ${myPluginConfig.value.release}")

        Runtime.getRuntime().addShutdownHook(thread(start = false) { clearInventories() })

        listen<ServerTickStartEvent> {
            if (!MinecraftServer.getServer().isRunning) {
                print("Running action on shutdown in tick start event!")
                clearInventories()
            }
        }

        listen<ServerTickEndEvent> {
            if (!MinecraftServer.getServer().isRunning) {
                print("Running action on shutdown in tick end event!")
                clearInventories()
            }
        }
    }

    override suspend fun onDisable() {
        CommandAPI.unregister("surfapitest")
    }

    private fun clearInventories() {
        for (player in plugin.server.onlinePlayers) {
            player.scheduler.run(
                plugin,
                {
                    player.inventory.clear()
                    player.inventory.addItem(ItemType.DIAMOND.createItemStack(64))
                },
                null,
            )
        }
    }
}

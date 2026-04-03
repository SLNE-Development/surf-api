package dev.slne.surf.surfapi.bukkit.test

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.destroystokyo.paper.event.server.ServerTickStartEvent
import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.jorel.commandapi.CommandAPI
import dev.slne.surf.surfapi.bukkit.api.event.listen
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.register
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.packetListenerApi
import dev.slne.surf.surfapi.bukkit.test.command.SurfApiTestCommand
import dev.slne.surf.surfapi.bukkit.test.command.dialog.dialogTestCommand
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.TestInventoryView
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.testInventoryViewDsl
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection.Reflection
import dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig
import dev.slne.surf.surfapi.bukkit.test.config.MyPluginConfig
import dev.slne.surf.surfapi.bukkit.test.listener.ChatListener
import dev.slne.surf.surfapi.core.api.component.surfComponentApi
import net.minecraft.server.MinecraftServer
import org.bukkit.inventory.ItemType
import kotlin.concurrent.thread

@OptIn(NmsUseWithCaution::class)
class BukkitPluginMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        ModernTestConfig.init()
        ModernTestConfig.randomise()

        surfComponentApi.load(this)
        packetListenerApi.registerListeners(ChatListener())
        TestInventoryView.register()
        testInventoryViewDsl.register()
    }

    override suspend fun onEnableAsync() {
        SurfApiTestCommand().register()
        dialogTestCommand()
        Reflection::class.java.getClassLoader() // initialize Reflection

        MyPluginConfig.init()

        surfComponentApi.enable(this)

        fun runAction() {
            for (player in server.onlinePlayers) {
                player.scheduler.run(this@BukkitPluginMain, {
                    player.inventory.clear()
                    player.inventory.addItem(ItemType.DIAMOND.createItemStack(64))
                }, null)
            }
        }

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            runAction()
        })

        listen<ServerTickStartEvent> {
            if (!MinecraftServer.getServer().isRunning) {
                print("Running action on shutdown in tick start event!")
                runAction()
            }
        }

        listen<ServerTickEndEvent> {
            if (!MinecraftServer.getServer().isRunning) {
                print("Running action on shutdown in tick end event!")
                runAction()
            }
        }
    }

    override suspend fun onDisableAsync() {
        CommandAPI.unregister("surfapitest")
        surfComponentApi.disable(this)
    }

    companion object {
        val instance: BukkitPluginMain
            get() = getPlugin(BukkitPluginMain::class.java)
    }
}

val plugin get() = BukkitPluginMain.instance
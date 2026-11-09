package dev.slne.surf.surfapi.bukkit.test

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.destroystokyo.paper.event.server.ServerTickStartEvent
import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.jorel.commandapi.CommandAPI
import dev.slne.surf.api.paper.event.listen
import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.SurfPaperPacketListenerApi
import dev.slne.surf.api.paper.server.plugin as surfApi
import dev.slne.surf.api.paper.test.command.SurfApiTestCommand
import dev.slne.surf.api.paper.test.command.subcommands.reflection.Reflection
import dev.slne.surf.api.paper.test.listener.ChatListener
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.surfapi.bukkit.test.command.dialog.dialogTestCommand
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.TestInventoryView
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.testInventoryViewDsl
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.testPaginatedViewDsl
import dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig
import dev.slne.surf.surfapi.bukkit.test.config.MyPluginConfig
import net.minecraft.server.MinecraftServer
import org.bukkit.inventory.ItemType
import kotlin.concurrent.thread

@OptIn(NmsUseWithCaution::class, InternalSurfApi::class)
class PaperPluginMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        ModernTestConfig.init()
        ModernTestConfig.randomise()

        // The plugin's own beans live in a child of surf-api's root container, which is what
        // every plugin does for itself: the root holds surf-api's beans and only surf-api may start
        // it. `startChild` runs this plugin's `load` and then its `enable` phase before it returns,
        // so both have happened by the time the server reaches `onEnableAsync`.
        surfApi.containers.startChild(this)

        SurfPaperPacketListenerApi.INSTANCE.registerListeners(ChatListener())
        TestInventoryView.register()
        testInventoryViewDsl.register()
        testPaginatedViewDsl.register()

//        SurfConfigApi.createSpongeYmlConfig(TestConfig2::class.java, dataPath, "test-config-2.yml")
    }

    override suspend fun onEnableAsync() {
        SurfApiTestCommand().register()
        dialogTestCommand()
        Reflection::class.java.classLoader // initialize Reflection

        MyPluginConfig.init()

        fun runAction() {
            for (player in server.onlinePlayers) {
                player.scheduler.run(this@PaperPluginMain, {
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

        // Disables this plugin's beans in reverse order and forgets the container. surf-api's own
        // beans are left running: they belong to the root, which surf-api stops after every plugin.
        surfApi.containers.stop(this)
    }

    companion object {
        val instance: PaperPluginMain
            get() = getPlugin(PaperPluginMain::class.java)
    }
}

val plugin get() = PaperPluginMain.instance

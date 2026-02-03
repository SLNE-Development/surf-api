package dev.slne.surf.surfapi.bukkit.test

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.jorel.commandapi.CommandAPI
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.register
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.packetListenerApi
import dev.slne.surf.surfapi.bukkit.server.gui.view.GuiViewListener
import dev.slne.surf.surfapi.bukkit.test.command.SurfApiTestCommand
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.TestInventoryView
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection.Reflection
import dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig
import dev.slne.surf.surfapi.bukkit.test.listener.ChatListener

@OptIn(NmsUseWithCaution::class)
class BukkitPluginMain : SuspendingJavaPlugin() {
    override fun onLoad() {
        ModernTestConfig.init()
        ModernTestConfig.randomise()

        packetListenerApi.registerListeners(ChatListener())
        TestInventoryView.register()
        
        // Register new GUI framework listener
        server.pluginManager.registerEvents(GuiViewListener, this)
    }

    override fun onEnable() {
        SurfApiTestCommand().register()
        Reflection::class.java.getClassLoader() // initialize Reflection
    }

    override fun onDisable() {
        CommandAPI.unregister("surfapitest")
    }

    companion object {
        val instance: BukkitPluginMain
            get() = getPlugin(BukkitPluginMain::class.java)
    }
}

val plugin get() = BukkitPluginMain.instance
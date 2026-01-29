package dev.slne.surf.surfapi.bukkit.test

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.jorel.commandapi.CommandAPI
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.register
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.packetListenerApi
import dev.slne.surf.surfapi.bukkit.test.command.SurfApiTestCommand
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.TestInventoryView
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection.Reflection
import dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig
import dev.slne.surf.surfapi.bukkit.test.listener.ChatListener
import dev.slne.surf.surfapi.core.api.component.surfComponentApi

@OptIn(NmsUseWithCaution::class)
class BukkitPluginMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        ModernTestConfig.init()
        ModernTestConfig.randomise()

        surfComponentApi.load(this)
        packetListenerApi.registerListeners(ChatListener())
        TestInventoryView.register()
    }

    override suspend fun onEnableAsync() {
        SurfApiTestCommand().register()
        Reflection::class.java.getClassLoader() // initialize Reflection

        surfComponentApi.enable(this)
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
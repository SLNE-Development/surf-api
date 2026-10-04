package dev.slne.surf.api.minestom.server.plugins

import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import net.minestom.server.event.player.PlayerSpawnEvent

/** A plugin on the test class path, found through its service entry. */
class TestPlugin : SurfMinestomPlugin() {
    override suspend fun onLoad() {
        calls += "load"
        eventNode.addListener(PlayerSpawnEvent::class.java) { }
        CommandAPICommand("test-plugin-command").anyExecutor { _, _ -> }.register()
    }

    override suspend fun onEnable() {
        calls += "enable"
    }

    override suspend fun onDisable() {
        calls += "disable"
    }

    companion object {
        val calls = mutableListOf<String>()
        var instance: TestPlugin? = null
    }

    init {
        instance = this
    }
}

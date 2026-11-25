package dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection

import dev.slne.surf.api.reflection.ReflectedMethod
import dev.slne.surf.api.reflection.ReflectedStatic
import dev.slne.surf.api.reflection.ReflectionProxy
import dev.slne.surf.api.reflection.reflectionProxy

@ReflectionProxy(targetName = "org.spigotmc.RestartCommand")
interface SpigotRestartCommandProxy {

    @ReflectedStatic
    @ReflectedMethod
    fun restart()

    companion object {
        val instance = reflectionProxy<SpigotRestartCommandProxy>()
    }
}

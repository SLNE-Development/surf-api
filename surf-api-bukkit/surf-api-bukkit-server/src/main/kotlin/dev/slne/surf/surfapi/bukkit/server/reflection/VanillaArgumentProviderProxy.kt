package dev.slne.surf.surfapi.bukkit.server.reflection

import dev.slne.surf.surfapi.core.api.reflection.Name
import dev.slne.surf.surfapi.core.api.reflection.Static
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy

@SurfProxy(qualifiedName = "io.papermc.paper.command.brigadier.argument.VanillaArgumentProvider")
interface VanillaArgumentProviderProxy {

    @Static
    @Name("provider")
    fun provider(): Any
}
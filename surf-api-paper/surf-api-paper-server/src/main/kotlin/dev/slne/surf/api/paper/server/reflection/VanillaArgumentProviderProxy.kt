package dev.slne.surf.api.paper.server.reflection

import dev.slne.surf.api.core.reflection.Name
import dev.slne.surf.api.core.reflection.Static
import dev.slne.surf.api.core.reflection.SurfProxy


@SurfProxy(qualifiedName = "io.papermc.paper.command.brigadier.argument.VanillaArgumentProvider")
interface VanillaArgumentProviderProxy {

    @Static
    @Name("provider")
    fun provider(): Any
}
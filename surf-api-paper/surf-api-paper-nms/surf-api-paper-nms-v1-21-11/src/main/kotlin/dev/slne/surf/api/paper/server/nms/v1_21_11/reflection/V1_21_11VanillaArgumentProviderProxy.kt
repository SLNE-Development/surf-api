package dev.slne.surf.api.paper.server.nms.v1_21_11.reflection

import dev.slne.surf.api.core.reflection.Name
import dev.slne.surf.api.core.reflection.Static
import dev.slne.surf.api.core.reflection.SurfProxy


@SurfProxy(qualifiedName = "io.papermc.paper.command.brigadier.argument.VanillaArgumentProvider")
interface V1_21_11VanillaArgumentProviderProxy {

    @Static
    @Name("provider")
    fun provider(): Any
}

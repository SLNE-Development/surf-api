package dev.slne.surf.surfapi.bukkit.server.reflection

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.surfapi.core.api.reflection.Name
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy
import io.papermc.paper.command.brigadier.argument.VanillaArgumentProviderImpl

@SurfProxy(VanillaArgumentProviderImpl::class)
interface VanillaArgumentProviderImplProxy {

    @Name("wrap")
    @Throws(CommandSyntaxException::class)
    fun wrap(instance: Any, base: Any, converter: Any): ArgumentType<*>
}
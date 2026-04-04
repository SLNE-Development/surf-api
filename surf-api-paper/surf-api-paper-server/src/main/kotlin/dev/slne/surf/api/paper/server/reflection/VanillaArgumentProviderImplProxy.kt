package dev.slne.surf.api.paper.server.reflection

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.core.reflection.Name
import dev.slne.surf.api.core.reflection.SurfProxy
import io.papermc.paper.command.brigadier.argument.VanillaArgumentProviderImpl

@SurfProxy(VanillaArgumentProviderImpl::class)
interface VanillaArgumentProviderImplProxy {

    @Name("wrap")
    @Throws(CommandSyntaxException::class)
    fun wrap(instance: Any, base: Any, converter: Any): ArgumentType<*>
}
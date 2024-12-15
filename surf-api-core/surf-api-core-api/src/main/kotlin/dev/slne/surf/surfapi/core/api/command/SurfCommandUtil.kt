package dev.slne.surf.surfapi.core.api.command

import dev.slne.surf.surfapi.core.api.command.builder.CommandExceptionBuilder
import dev.slne.surf.surfapi.core.api.command.exception.WrapperCommandExceptionComponent
import net.kyori.adventure.text.ComponentLike
import org.jetbrains.annotations.ApiStatus

@ApiStatus.NonExtendable
object SurfCommandUtil {
    @JvmStatic
    fun createException(message: ComponentLike) =
        WrapperCommandExceptionComponent(message.asComponent())

    @JvmStatic
    fun failWithMessage(message: ComponentLike) {
        throw createException(message)
    }

    @JvmStatic
    fun failWithBuilder(builder: CommandExceptionBuilder) {
        throw createException(builder.build())
    }
}

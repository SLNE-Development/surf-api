package dev.slne.surf.api.core.messages

import com.mojang.brigadier.Message
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.jetbrains.annotations.ApiStatus
import java.util.*

class ComponentMessage(private val message: ComponentLike) : Message, ComponentLike {
    override fun getString(): String {
        return (COMPONENT_MESSAGE_PREFIX + GsonComponentSerializer.gson()
            .serialize(message.asComponent().compact()))
    }

    override fun asComponent(): Component {
        return message.asComponent()
    }

    companion object {
        @ApiStatus.Internal
        val COMPONENT_MESSAGE_PREFIX: String = "[ComponentMessage " + UUID.randomUUID() + "] "
    }
}

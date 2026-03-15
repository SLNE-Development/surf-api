package dev.slne.surf.microservice.test.api

import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.serializer.adventure.key.SerializableKey
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.ComponentLike

@Serializable
data class Message(
    val key: SerializableKey,
    val content: String
) : ComponentLike {
    private val displayName = buildText {
        variableKey(key.asString() + ": ")
        variableValue(content)
    }

    override fun asComponent() = displayName
}
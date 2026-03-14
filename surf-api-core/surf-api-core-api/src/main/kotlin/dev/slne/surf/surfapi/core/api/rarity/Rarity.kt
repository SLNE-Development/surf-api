package dev.slne.surf.surfapi.core.api.rarity

import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.TextColor

/**
 * Represents the rarity of an item or entity within the system.
 *
 * Each rarity level is associated with a display name and a color,
 * allowing visually distinct representations for different levels.
 *
 * @property displayName The visual display name of the rarity level.
 * @property color       The textual color associated with the rarity level.
 */
enum class Rarity(
    displayName: SurfComponentBuilder.() -> Unit,
    val color: TextColor
) : ComponentLike {
    COMMON(
        displayName = { spacer("Gewöhnlich") },
        color = TextColor.color(0xAAAAAA)
    ),
    UNCOMMON(
        displayName = { spacer("Ungewöhnlich") },
        color = TextColor.color(0x55FF55)
    ),
    RARE(
        displayName = { success("Selten") },
        color = TextColor.color(0x55FFFF)
    ),
    EPIC(
        displayName = { info("Episch") },
        color = TextColor.color(0xFF55FF)
    ),
    LEGENDARY(
        displayName = { warning("Legendär") },
        color = TextColor.color(0xFFAA00)
    ),
    MYTHIC(
        displayName = { error("Mythisch") },
        color = TextColor.color(0xAA00AA)
    );

    val displayName = buildText(displayName)
    override fun asComponent() = displayName
}
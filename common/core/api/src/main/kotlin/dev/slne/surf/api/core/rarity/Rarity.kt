package dev.slne.surf.api.core.rarity

import dev.slne.surf.api.core.messages.adventure.text
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
    displayName: String,
    val color: TextColor
) : ComponentLike {
    COMMON(
        displayName = "Gewöhnlich",
        color = TextColor.color(0xAAAAAA),
    ),
    UNCOMMON(
        displayName = "Ungewöhnlich",
        color = TextColor.color(0x55FF55)
    ),
    RARE(
        displayName = "Selten",
        color = TextColor.color(0x55FFFF)
    ),
    EPIC(
        displayName = "Episch",
        color = TextColor.color(0xFF55FF)
    ),
    LEGENDARY(
        displayName = "Legendär",
        color = TextColor.color(0xFFAA00)
    ),
    MYTHIC(
        displayName = "Mythisch",
        color = TextColor.color(0xAA00AA)
    );

    val displayName = text(displayName, color)
    override fun asComponent() = displayName
}
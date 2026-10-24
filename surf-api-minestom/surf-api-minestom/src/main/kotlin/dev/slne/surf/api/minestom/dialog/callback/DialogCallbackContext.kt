package dev.slne.surf.api.minestom.dialog.callback

import net.kyori.adventure.nbt.BinaryTag
import net.minestom.server.entity.Player

/**
 * What the client sent along when it ran a dialog callback.
 *
 * @property player the player who pressed the button
 * @property payload the input values the dialog collected, or `null` for a dialog without inputs
 */
data class DialogCallbackContext(
    val player: Player,
    val payload: BinaryTag?,
)

package dev.slne.surf.api.minestom.example

import dev.slne.surf.api.minestom.npc.mannequinNpc
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

/**
 * NPCs spawned through the NPC feature of the surf api.
 */
internal object ExampleNpcs {

    fun spawn(instance: Instance) {
        mannequinNpc("guide", instance, Pos(3.5, 41.0, 3.5, 135f, 0f)) {
            displayName = Component.text("Guide", NamedTextColor.GREEN)
            setEquipment(EquipmentSlot.MAIN_HAND, ItemStack.of(Material.COMPASS))
            onInteract { interaction ->
                interaction.player.sendMessage(
                    Component.text("Try /example highlight, /shout or /gamemode creative!")
                )
            }
        }
    }
}

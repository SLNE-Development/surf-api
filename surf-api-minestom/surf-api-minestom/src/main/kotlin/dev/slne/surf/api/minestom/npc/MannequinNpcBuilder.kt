package dev.slne.surf.api.minestom.npc

import codes.bed.minestom.npc.api.NpcInteraction
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.network.player.ResolvableProfile
import java.util.*

/**
 * Builds and spawns a [MannequinNpc].
 *
 * ```
 * val npc = mannequinNpc("shop", instance, Pos(0.5, 100.0, 0.5)) {
 *     displayName = Component.text("Shop")
 *     skin = ResolvableProfile(PlayerSkin(textures, signature))
 *     scale = 1.5
 *     onInteract { interaction -> interaction.player.sendMessage("Hello!") }
 * }
 * ```
 */
class MannequinNpcBuilder(private val name: String) {

    /**
     * The hologram text shown above the NPC.
     */
    var displayName: Component = Component.empty()

    /**
     * The profile the mannequin renders, or `null` for the default profile.
     */
    var profile: ResolvableProfile? = null

    /**
     * The size of the mannequin, where `1.0` is player-sized.
     */
    var scale: Double = 1.0

    /**
     * Where the hologram sits relative to the NPC's feet. Defaults to just above the
     * mannequin's head, following [scale].
     */
    var displayNameOffset: Vec? = null

    /**
     * The text the client renders below the mannequin. Empty hides the client's default
     * mannequin label.
     */
    var description: Component = Component.empty()

    private val interactHandlers = mutableListOf<(NpcInteraction) -> Unit>()

    private val equipments = EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot::class.java)

    /**
     * Runs [handler] whenever a player clicks the NPC.
     */
    fun onInteract(handler: (NpcInteraction) -> Unit) {
        interactHandlers += handler
    }

    fun setEquipment(slot: EquipmentSlot, item: ItemStack) {
        equipments[slot] = item
    }

    @PublishedApi
    internal fun spawn(instance: Instance, position: Pos): MannequinNpc {
        val npc = MannequinNpc(
            name = name,
            hologramText = displayName,
            profile = profile,
            scale = scale,
            hologramOffset = displayNameOffset ?: Vec(0.0, HOLOGRAM_HEIGHT * scale, 0.0),
            description = description,

            // Equipments
            mainHandItem = equipments[EquipmentSlot.MAIN_HAND] ?: ItemStack.AIR,
            offHandItem = equipments[EquipmentSlot.OFF_HAND] ?: ItemStack.AIR,
            helmet = equipments[EquipmentSlot.HELMET] ?: ItemStack.AIR,
            chestplate = equipments[EquipmentSlot.CHESTPLATE] ?: ItemStack.AIR,
            leggings = equipments[EquipmentSlot.LEGGINGS] ?: ItemStack.AIR,
            boots = equipments[EquipmentSlot.BOOTS] ?: ItemStack.AIR,
            bodyEquipment = equipments[EquipmentSlot.BODY] ?: ItemStack.AIR,
            saddleEquipment = equipments[EquipmentSlot.SADDLE] ?: ItemStack.AIR,
        )

        interactHandlers.forEach { handler ->
            npc.onInteract { interaction -> handler(interaction) }
        }

        npc.spawn()
        npc.setInstance(instance, position)

        return npc
    }

    private companion object {
        const val HOLOGRAM_HEIGHT = 2.1
    }
}

/**
 * Creates a [MannequinNpc] named [name], configures it with [block] and spawns it at
 * [position] in [instance].
 */
inline fun mannequinNpc(
    name: String,
    instance: Instance,
    position: Pos,
    block: MannequinNpcBuilder.() -> Unit,
): MannequinNpc = MannequinNpcBuilder(name).apply(block).spawn(instance, position)

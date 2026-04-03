package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.player

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player.SurfBukkitNmsPlayerToastPackets
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player.toast.Toast
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.PacketOperationImpl
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.advancements.AdvancementRequirements
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket
import net.minecraft.resources.Identifier
import java.util.*


@NmsUseWithCaution
@AutoService(SurfBukkitNmsPlayerToastPackets::class)
class SurfBukkitNmsPlayerToastPacketsImpl : SurfBukkitNmsPlayerToastPackets {
    override fun showToast(toast: Toast) = PacketOperationImpl.complex { _, packets ->
        val id = Identifier.fromNamespaceAndPath("surfapi", "toast_${UUID.randomUUID()}")

        packets.add(showPacket(id, toast))
        packets.add(hidePacket(id))

        packets
    }

    private fun showPacket(id: Identifier, toast: Toast) =
        ClientboundUpdateAdvancementsPacket(
            false, listOf(createAdvancement(id, toast)), emptySet(), mapOf(
                id to AdvancementProgress().apply {
                    update(requirements)
                    grantProgress(CRITERION_ID)
                }), true
        )

    private fun hidePacket(id: Identifier) = ClientboundUpdateAdvancementsPacket(
        false, emptyList(), setOf(id), emptyMap(), false
    )

    private fun createAdvancement(id: Identifier, toast: Toast) =
        Advancement.Builder.recipeAdvancement()
            .display(
                toast.icon.toNms().item,
                toast.title.toNms(),
                Component.empty(),
                null,
                toast.frame.toNms(),
                true,
                false,
                false
            )
            .requirements(requirements)
            .build(id)

    companion object {
        private const val CRITERION_ID = "surfapi_toast"
        private val requirements by lazy { AdvancementRequirements.allOf(listOf(CRITERION_ID)) }
    }
}
package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.player

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerToastPackets
import dev.slne.surf.api.paper.nms.bridges.packets.player.toast.Toast
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.advancements.AdvancementRequirements
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket
import net.minecraft.resources.ResourceLocation
import java.util.*


@NmsUseWithCaution
class V1_21_11SurfPaperNmsPlayerToastPacketsImpl : SurfPaperNmsPlayerToastPackets {
    override fun showToast(toast: Toast) = V1_21_11PacketOperationImpl.complex { _, packets ->
        val id = ResourceLocation.fromNamespaceAndPath("surfapi", "toast_${UUID.randomUUID()}")

        packets.add(showPacket(id, toast))
        packets.add(hidePacket(id))

        packets
    }

    private fun showPacket(id: ResourceLocation, toast: Toast) =
        ClientboundUpdateAdvancementsPacket(
            false, listOf(createAdvancement(id, toast)), emptySet(), mapOf(
                id to AdvancementProgress().apply {
                    update(requirements)
                    grantProgress(CRITERION_ID)
                }), true
        )

    private fun hidePacket(id: ResourceLocation) = ClientboundUpdateAdvancementsPacket(
        false, emptyList(), setOf(id), emptyMap(), false
    )

    private fun createAdvancement(id: ResourceLocation, toast: Toast) =
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

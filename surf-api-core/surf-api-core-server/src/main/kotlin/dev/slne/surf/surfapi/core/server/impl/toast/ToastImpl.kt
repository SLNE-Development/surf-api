package dev.slne.surf.surfapi.core.server.impl.toast

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.advancements.*
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemType
import com.github.retrooper.packetevents.resources.ResourceLocation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAdvancements
import dev.slne.surf.surfapi.core.api.surfCoreApi
import dev.slne.surf.surfapi.core.api.toast.Toast
import dev.slne.surf.surfapi.core.api.toast.ToastStyle
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import net.kyori.adventure.text.Component
import java.util.*

private const val CRITERION_ID = "surfapi_toast"

class ToastImpl(
    override val icon: ItemType,
    override val text: Component,
    override val style: ToastStyle
) : Toast {
    override fun send(player: UUID) {
        val user =
            PacketEvents.getAPI().playerManager.getUser(surfCoreApi.getPlayer(player) ?: return)
                ?: return
        val resourceLocation = ResourceLocation.minecraft("surfapi_toast_${UUID.randomUUID()}")

        user.sendPacket(buildShowPacket(resourceLocation, this))
        user.sendPacket(buildHidePacket(resourceLocation))
    }
}

private fun ToastStyle.toType() = when (this) {
    ToastStyle.TASK -> AdvancementType.TASK
    ToastStyle.GOAL -> AdvancementType.GOAL
    ToastStyle.CHALLENGE -> AdvancementType.CHALLENGE
}

private fun createFakeAdvancement(toast: Toast): Advancement {
    val criterion: List<String> = Collections.singletonList(CRITERION_ID)
    return Advancement(
        null,
        AdvancementDisplay(
            toast.text,
            Component.empty(),
            ItemStack.builder().type(toast.icon).build(),
            toast.style.toType(),
            null,
            true,
            false,
            0.0f,
            0.0f
        ),
        criterion,
        Collections.singletonList(criterion),
        false
    )
}

private fun buildShowPacket(resourceLocation: ResourceLocation, toast: Toast) =
    WrapperPlayServerUpdateAdvancements(
        false,
        Collections.singletonList(
            AdvancementHolder(
                resourceLocation,
                createFakeAdvancement(toast)
            )
        ),
        Collections.emptySet(),
        Collections.singletonMap(
            resourceLocation, AdvancementProgress(
                mutableObject2ObjectMapOf(
                    CRITERION_ID to AdvancementProgress.CriterionProgress(System.currentTimeMillis())
                )
            )
        ),
        true
    )

private fun buildHidePacket(resourceLocation: ResourceLocation) =
    WrapperPlayServerUpdateAdvancements(
        false,
        Collections.emptyList(),
        Collections.singleton(resourceLocation),
        Collections.emptyMap(),
        false
    )

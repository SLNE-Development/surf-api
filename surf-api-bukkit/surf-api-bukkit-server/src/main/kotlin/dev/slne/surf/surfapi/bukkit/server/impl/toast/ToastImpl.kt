package dev.slne.surf.surfapi.bukkit.server.impl.toast

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.advancements.*
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.resources.ResourceLocation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAdvancements
import dev.slne.surf.surfapi.bukkit.api.toast.Toast
import dev.slne.surf.surfapi.bukkit.api.toast.ToastStyle
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*

class ToastImpl(
    override val icon: Material,
    override val text: Component,
    override val style: ToastStyle
) : Toast {
    override fun send(player: Player) {
        val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
        val stack =
            ItemStack.builder().type(SpigotConversionUtil.fromBukkitItemMaterial(icon)).build()
        val criterionName = "surfapi_toast"
        val criterion: List<String> = Collections.singletonList(criterionName)
        val resourceLocation = ResourceLocation.minecraft("surfapi_toast_${UUID.randomUUID()}")
        val advancement = Advancement(
            null,
            AdvancementDisplay(
                text,
                buildText {
                    error("test")
                },
                stack,
                style.toType(),
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

        val showPacket = WrapperPlayServerUpdateAdvancements(
            false,
            Collections.singletonList(
                AdvancementHolder(
                    resourceLocation,
                    advancement
                )
            ),
            Collections.emptySet(),
            Collections.singletonMap(
                resourceLocation, AdvancementProgress(
                    mutableObject2ObjectMapOf(
                        criterionName to AdvancementProgress.CriterionProgress(System.currentTimeMillis())
                    )
                )
            ),
            true
        )

        val hidePacket = WrapperPlayServerUpdateAdvancements(
            false,
            Collections.emptyList(),
            Collections.singleton(resourceLocation),
            Collections.emptyMap(),
            false
        )

        user.sendPacket(showPacket)
        user.sendPacket(hidePacket)
    }
}

private fun ToastStyle.toType() = when (this) {
    ToastStyle.TASK -> AdvancementType.TASK
    ToastStyle.GOAL -> AdvancementType.GOAL
    ToastStyle.CHALLENGE -> AdvancementType.CHALLENGE
}
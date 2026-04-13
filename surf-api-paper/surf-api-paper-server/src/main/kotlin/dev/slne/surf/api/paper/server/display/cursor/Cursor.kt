package dev.slne.surf.api.paper.server.display.cursor

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemEquippable
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.protocol.sound.Sounds
import com.github.retrooper.packetevents.resources.ResourceLocation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import dev.slne.surf.api.paper.display.cursor.CursorStyle
import dev.slne.surf.api.paper.server.display.user.DisplayUser

class Cursor(
    private val horseEntityId: Int,
    private val user: DisplayUser
) {
    var currentStyle: CursorStyle = CursorStyle.DEFAULT
        private set

    fun setCursor(style: CursorStyle) {
        if (style == currentStyle) return
        currentStyle = style
        sendCursorUpdate()
    }

    fun reset() {
        setCursor(CursorStyle.DEFAULT)
    }

    fun sendCursorUpdate() {
        val equipment = createHorseEquipment(currentStyle.texturePath, horseEntityId)
        user.sendPacket(equipment)
    }

    companion object {
        private const val NAMESPACE = "surf-display"

        fun createHorseEquipment(texturePath: String, entityId: Int): WrapperPlayServerEntityEquipment {
            return WrapperPlayServerEntityEquipment(
                entityId,
                listOf(
                    Equipment(
                        EquipmentSlot.SADDLE,
                        ItemStack.builder().type(ItemTypes.SADDLE).build()
                    ),
                    Equipment(
                        EquipmentSlot.BODY,
                        ItemStack.builder()
                            .type(ItemTypes.COPPER_HORSE_ARMOR)
                            .component(
                                ComponentTypes.EQUIPPABLE,
                                ItemEquippable(
                                    EquipmentSlot.BODY,
                                    Sounds.ITEM_ARMOR_EQUIP_GENERIC,
                                    ResourceLocation(NAMESPACE, texturePath),
                                    null,
                                    null,
                                    false,
                                    false,
                                    false
                                )
                            )
                            .build()
                    )
                )
            )
        }
    }
}

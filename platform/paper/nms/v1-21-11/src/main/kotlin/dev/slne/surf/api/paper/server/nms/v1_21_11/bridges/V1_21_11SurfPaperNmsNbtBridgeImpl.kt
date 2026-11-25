package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsNbtBridge
import dev.slne.surf.api.paper.nms.common.NmsComponent
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toBukkit
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.TypedEntityData
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
@NmsComponent
class V1_21_11SurfPaperNmsNbtBridgeImpl : SurfPaperNmsNbtBridge {
    override fun makeItemStackEntityInvisible(
        itemStack: ItemStack,
        invisibleEntityType: EntityType,
    ): ItemStack {
        val nmsStack = itemStack.toNms()

        val nbt = CompoundTag()
        nbt.putBoolean("Invisible", true)
        nbt.putString("id", invisibleEntityType.key.asString())

        val entityData = TypedEntityData.decodeEntity(nbt)

        val patch = DataComponentPatch.builder()
            .set(DataComponents.ENTITY_DATA, entityData)
            .build()

        nmsStack.applyComponents(patch)

        return nmsStack.toBukkit()
    }
}

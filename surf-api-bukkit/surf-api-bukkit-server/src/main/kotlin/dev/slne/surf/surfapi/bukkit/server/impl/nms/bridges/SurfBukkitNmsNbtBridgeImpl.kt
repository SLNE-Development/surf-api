package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsNbtBridge
import dev.slne.surf.surfapi.bukkit.server.nms.toBukkit
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.core.api.util.logger
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack

@AutoService(SurfBukkitNmsNbtBridge::class)
@NmsUseWithCaution
class SurfBukkitNmsNbtBridgeImpl : SurfBukkitNmsNbtBridge {

    private val log = logger()

    override fun makeItemStackEntityInvisible(
        itemStack: ItemStack,
        entityType: EntityType
    ): ItemStack {
        val nmsStack = itemStack.toNms()

        val nbt = CompoundTag()
        nbt.putBoolean("Invisible", true)
        nbt.putString("id", entityType.getKey().asString())

        val patch = DataComponentPatch.builder()
            .set(DataComponents.ENTITY_DATA, CustomData.of(nbt))
            .build()

        nmsStack.applyComponents(patch)

        return nmsStack.toBukkit()
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun getNbtString(
        itemStack: ItemStack,
        key: String
    ): String {
        log.atWarning()
            .atMostEvery(30, java.util.concurrent.TimeUnit.SECONDS)
            .log(
                ("Using deprecated method getNbtString(ItemStack, String) in SurfBukkitNmsNbtBridgeImpl."
                        + " ItemStacks now use DataComponents and nbt keys are not used. Please update your"
                        + " code to use DataComponents instead.")
            )

        return itemStack.toNms().components
            .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .unsafe.getString(key)
    }
}

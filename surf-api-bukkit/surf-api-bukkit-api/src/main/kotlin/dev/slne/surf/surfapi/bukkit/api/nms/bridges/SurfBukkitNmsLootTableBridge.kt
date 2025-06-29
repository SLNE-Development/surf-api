package dev.slne.surf.surfapi.bukkit.api.nms.bridges

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.damage.DamageSource
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
interface SurfBukkitNmsLootTableBridge {

    fun getDifferentLootTable(
        entity: LivingEntity,
        damageSource: DamageSource,
        replacement: EntityType,
        causedByPlayer: Boolean,
    ): Collection<ItemStack>

    companion object {
        val instance = requiredService<SurfBukkitNmsLootTableBridge>()
    }
}

@NmsUseWithCaution
val nmsLootTableBridge get() = SurfBukkitNmsLootTableBridge.instance
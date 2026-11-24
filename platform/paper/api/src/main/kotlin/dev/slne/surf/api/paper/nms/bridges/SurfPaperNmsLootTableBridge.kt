package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.spring.SurfApiBean
import org.bukkit.damage.DamageSource
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
@SurfApiBean
interface SurfPaperNmsLootTableBridge {

    fun getDifferentLootTable(
        entity: LivingEntity,
        damageSource: DamageSource,
        replacement: EntityType,
        causedByPlayer: Boolean,
    ): Collection<ItemStack>

    fun rollLootTable(
        entity: LivingEntity,
        damageSource: DamageSource,
        causedByPlayer: Boolean,
    ): Collection<ItemStack>
}

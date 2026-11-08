package dev.slne.surf.api.paper.server.nms.v26_2.bridges

import dev.slne.surf.api.core.util.emptyObjectList
import dev.slne.surf.api.core.util.mutableObjectListOf
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsLootTableBridge
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toBukkit
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNms
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import org.bukkit.damage.DamageSource
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import kotlin.jvm.optionals.getOrNull

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2SurfPaperNmsLootTableBridgeImpl : SurfPaperNmsLootTableBridge {
    override fun getDifferentLootTable(
        entity: LivingEntity,
        damageSource: DamageSource,
        replacement: EntityType,
        causedByPlayer: Boolean,
    ): Collection<ItemStack> {
        val lootTableKey =
            replacement.toNms().defaultLootTable.getOrNull() ?: return emptyObjectList()
        val lootTable =
            MinecraftServer.getServer().reloadableRegistries().getLootTable(lootTableKey)
        val nmsEntity = entity.toNms()
        val nmsDamageSource = damageSource.toNms()

        val lootParamsBuilder = LootParams.Builder(nmsEntity.level() as ServerLevel)
            .withParameter(LootContextParams.THIS_ENTITY, nmsEntity)
            .withParameter(LootContextParams.ORIGIN, nmsEntity.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, nmsDamageSource)
            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, nmsDamageSource.entity)
            .withOptionalParameter(
                LootContextParams.DIRECT_ATTACKING_ENTITY,
                nmsDamageSource.directEntity
            )

        val lastHurtByPlayer = nmsEntity.getLastHurtByPlayer()
        if (causedByPlayer && lastHurtByPlayer != null) {
            lootParamsBuilder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, lastHurtByPlayer)
                .withLuck(lastHurtByPlayer.luck)
        }

        val lootParams = lootParamsBuilder.create(LootContextParamSets.ENTITY)
        return lootTable.getRandomItems(lootParams, nmsEntity.lootTableSeed)
            .mapTo(mutableObjectListOf()) { it.toBukkit() }
    }

    override fun rollLootTable(
        entity: LivingEntity,
        damageSource: DamageSource,
        causedByPlayer: Boolean
    ): Collection<ItemStack> {
        val nmsEntity = entity.toNms()
        val lootTableKey = nmsEntity.lootTable.getOrNull() ?: return emptyObjectList()
        val lootTable = MinecraftServer.getServer().reloadableRegistries().getLootTable(lootTableKey)
        val nmsDamageSource = damageSource.toNms()

        val paramBuilder = LootParams.Builder(nmsEntity.level() as ServerLevel)
            .withParameter(LootContextParams.THIS_ENTITY, nmsEntity)
            .withParameter(LootContextParams.ORIGIN, nmsEntity.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, nmsDamageSource)
            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, nmsDamageSource.entity)
            .withOptionalParameter(
                LootContextParams.DIRECT_ATTACKING_ENTITY,
                nmsDamageSource.directEntity
            )

        val lastHurtByPlayer = nmsEntity.getLastHurtByPlayer()
        if (causedByPlayer && lastHurtByPlayer != null) {
            paramBuilder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, lastHurtByPlayer)
                .withLuck(lastHurtByPlayer.luck)
        }

        val lootParams = paramBuilder.create(LootContextParamSets.ENTITY)

        return lootTable.getRandomItems(lootParams, nmsEntity.lootTableSeed)
            .mapTo(mutableObjectListOf()) { it.toBukkit() }
    }
}

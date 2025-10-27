package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsBiomeBridge
import dev.slne.surf.surfapi.bukkit.server.nms.RegistryLock
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import net.minecraft.core.Holder
import net.minecraft.core.RegistrationInfo
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.Music
import net.minecraft.util.random.Weighted
import net.minecraft.util.random.WeightedList
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.biome.*
import net.minecraft.world.level.biome.Biome.TemperatureModifier
import org.bukkit.Registry
import org.bukkit.block.Biome
import org.bukkit.craftbukkit.CraftParticle
import org.bukkit.craftbukkit.CraftSound
import org.bukkit.craftbukkit.block.CraftBiome
import org.spigotmc.AsyncCatcher
import kotlin.jvm.optionals.getOrNull

@NmsUseWithCaution
@AutoService(SurfBukkitNmsBiomeBridge::class)
class SurfBukkitNmsBiomeBridgeImpl : SurfBukkitNmsBiomeBridge {
    override fun registry(): Registry<Biome> {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
    }

    override fun unregister(biome: Biome) {
        AsyncCatcher.catchOp("biome unregistration")
        require(biome is CraftBiome) { "Biome must be a CraftBiome" }

        RegistryLock.unlockRegistry(Registries.BIOME) { registry ->
            val proxy = Reflection.MAPPED_REGISTRY_PROXY
            val key = registry.getResourceKey(biome.handle).getOrNull() ?: error("Failed to get key for biome")

            val byKey = proxy.getByKey(registry)
            val ref = byKey.remove(key) ?: error("Failed to remove biome by key")
            val value = ref.value()

            proxy.getByLocation(registry).remove(key.location())
            proxy.getByValue(registry).remove(value)
            proxy.getRegistrationInfos(registry).remove(key)

            val byId = proxy.getById(registry)
            val idx = byId.indexOf(ref)

            if (idx >= 0) {
                byId.removeAt(idx)
                val toId = proxy.getToId(registry)
                toId.removeInt(value)
                var i = idx
                while (i < byId.size) {
                    val r = byId[i]
                    toId.put(r.value(), i)
                    i++
                }
            }

            val frozenTags = proxy.getFrozenTags(registry)
            frozenTags.forEach { (_, named) ->
                val filtered = ArrayList<Holder<net.minecraft.world.level.biome.Biome>>()
                for (h in named) if (h !== ref) filtered.add(h)
                Reflection.HOLDER_SET_PROXY_NAMED.bind(named, filtered)
            }

            proxy.refreshTagsInHolders(registry)
        }
    }


    override fun register(key: Key, data: SurfBukkitNmsBiomeBridge.BiomeData): Biome {
        AsyncCatcher.catchOp("biome registration")

        val biomeKey = ResourceKey.create(Registries.BIOME, PaperAdventure.asVanilla(key))

        val fromBiom = data.biomeGenerationSettings
        require(fromBiom is CraftBiome)
        fromBiom.handle.generationSettings

        val biome = net.minecraft.world.level.biome.Biome.BiomeBuilder()
            .hasPrecipitation(data.hasPrecipitation)
            .temperature(data.temperature)
            .downfall(data.downfall)
            .temperatureAdjustment(TemperatureModifier.valueOf(data.temperatureModifier.name))
            .specialEffects(BiomeSpecialEffects.Builder().apply {
                val specialEffects = data.specialEffects

                specialEffects.fogColor?.let { fogColor(it.value()) }
                specialEffects.waterColor?.let { waterColor(it.value()) }
                specialEffects.waterFogColor?.let { waterFogColor(it.value()) }
                specialEffects.skyColor?.let { skyColor(it.value()) }
                specialEffects.foliageColorOverride?.let { foliageColorOverride(it.value()) }
                specialEffects.dryFoliageColorOverride?.let { dryFoliageColorOverride(it.value()) }
                specialEffects.grassColorOverride?.let { grassColorOverride(it.value()) }
                grassColorModifier(BiomeSpecialEffects.GrassColorModifier.valueOf(specialEffects.grassColorModifier.name))
                specialEffects.ambientParticle?.let { ambientParticle ->
                    ambientParticle(
                        AmbientParticleSettings(
                            CraftParticle.createParticleParam(ambientParticle.particle, ambientParticle.data),
                            ambientParticle.probability
                        )
                    )
                }
                specialEffects.ambientLoopSoundEvent?.let { ambientLoopSound(CraftSound.bukkitToMinecraftHolder(it)) }
                specialEffects.ambientMoodSettings?.let { moodSettings ->
                    ambientMoodSound(
                        AmbientMoodSettings(
                            CraftSound.bukkitToMinecraftHolder(moodSettings.soundEvent),
                            moodSettings.tickDelay,
                            moodSettings.blockSearchExtent,
                            moodSettings.soundPositionOffset
                        )
                    )
                }
                specialEffects.ambientAdditionsSettings?.let { additionsSettings ->
                    ambientAdditionsSound(
                        AmbientAdditionsSettings(
                            CraftSound.bukkitToMinecraftHolder(additionsSettings.sound),
                            additionsSettings.tickChance
                        )
                    )
                }
                specialEffects.backgroundMusic?.let { musics ->
                    backgroundMusic(
                        WeightedList.of(
                            musics.map { (music, chance) ->
                                Weighted(
                                    Music(
                                        CraftSound.bukkitToMinecraftHolder(music.sound),
                                        music.minDelay,
                                        music.maxDelay,
                                        music.replaceCurrentMusic
                                    ),
                                    chance
                                )
                            }
                        )
                    )
                }
                backgroundMusicVolume(specialEffects.backgroundMusicVolume)
            }.build())
            .mobSpawnSettings(MobSpawnSettings.Builder().apply {
                val mobSpawnSettings = data.mobSpawnSettings
                for ((category, spawnDatas) in mobSpawnSettings.spawners) {
                    for ((data, chance) in spawnDatas) {
                        addSpawn(
                            MobCategory.valueOf(category.name),
                            chance,
                            MobSpawnSettings.SpawnerData(
                                data.type.toNms(),
                                data.minCount,
                                data.maxCount
                            )
                        )
                    }
                }
                for ((type, cost) in mobSpawnSettings.mobSpawnCosts) {
                    addMobCharge(
                        type.toNms(),
                        cost.charge,
                        cost.energyBudget
                    )
                }
                creatureGenerationProbability(mobSpawnSettings.creatureGenerationProbability)
            }.build())
            .generationSettings(
                (data.biomeGenerationSettings as CraftBiome).handle.generationSettings
            )
            .build()

        RegistryLock.unlockRegistry(Registries.BIOME) { registry ->
            registry.register(
                biomeKey,
                biome,
                RegistrationInfo.BUILT_IN
            )
        }

        return CraftBiome.minecraftToBukkit(biome)
    }
}
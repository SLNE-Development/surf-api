package dev.slne.surf.surfapi.bukkit.api.nms.bridges

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.requiredService
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Particle
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.block.Biome
import org.bukkit.entity.EntityType

@NmsUseWithCaution
interface SurfBukkitNmsBiomeBridge {
    fun registry(): Registry<Biome>
    fun unregister(biome: Biome)
    fun register(key: Key, data: BiomeData): Biome

    companion object {
        val instance = requiredService<SurfBukkitNmsBiomeBridge>()
    }

    data class BiomeData(
        val temperature: Float,
        val temperatureModifier: TemperatureModifier,
        val downfall: Float,
        val specialEffects: BiomeSpecialEffectsData,
        val mobSpawnSettings: MobSpawnSettingsData,
        val biomeGenerationSettings: Biome,
        val hasPrecipitation: Boolean = true
    ) {

        data class BiomeSpecialEffectsData(
            val fogColor: TextColor? = null,
            val waterColor: TextColor? = null,
            val waterFogColor: TextColor? = null,
            val skyColor: TextColor? = null,
            val foliageColorOverride: TextColor? = null,
            val dryFoliageColorOverride: TextColor? = null,
            val grassColorOverride: TextColor? = null,
            val grassColorModifier: GrassColorModifier = GrassColorModifier.NONE,
            val ambientParticle: AmbientParticleSettingsData? = null,
            val ambientLoopSoundEvent: Sound? = null,
            val ambientMoodSettings: AmbientMoodSettingsData? = null,
            val ambientAdditionsSettings: AmbientAdditionsSettingsData? = null,
            val backgroundMusic: Object2IntMap<Music>? = null,
            val backgroundMusicVolume: Float = 1.0f,
        ) {
            enum class GrassColorModifier { NONE, DARK_FOREST, SWAMP }

            /**
             * @see Particle.DustOptions
             * @see Particle.Spell
             */
            data class AmbientParticleSettingsData(val particle: Particle, val data: Any? = null, val probability: Float)

            data class AmbientMoodSettingsData(
                val soundEvent: Sound,
                val tickDelay: Int,
                val blockSearchExtent: Int,
                val soundPositionOffset: Double
            )

            data class AmbientAdditionsSettingsData(val sound: Sound, val tickChance: Double)

            data class Music(val sound: Sound, val minDelay: Int, val maxDelay: Int, val replaceCurrentMusic: Boolean)
        }

        data class MobSpawnSettingsData(
            val spawners: Object2ObjectMap<MobCategory, Object2IntMap<SpawnerData>>,
            val mobSpawnCosts: Object2ObjectMap<EntityType, MobSpawnCost>,
            val creatureGenerationProbability: Float = 0.1f
        ) {
            enum class MobCategory { MONSTER, CREATURE, AMBIENT, AXOLOTLS, UNDERGROUND_WATER_CREATURE, WATER_CREATURE, WATER_AMBIENT, MISC }
            data class SpawnerData(val type: EntityType, val minCount: Int, val maxCount: Int)
            data class MobSpawnCost(val energyBudget: Double, val charge: Double)
        }

        enum class TemperatureModifier { NONE, FROZEN }
    }

}

@NmsUseWithCaution
val nmsBiomeBridge get() = SurfBukkitNmsBiomeBridge.instance
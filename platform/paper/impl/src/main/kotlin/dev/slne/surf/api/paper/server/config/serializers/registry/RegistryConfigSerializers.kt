package dev.slne.surf.api.paper.server.config.serializers.registry

import dev.slne.surf.api.config.serializer.DefaultConfigSerializerSource
import dev.slne.surf.api.paper.server.PaperInstance
import io.leangen.geantyref.TypeToken
import io.papermc.paper.datacomponent.DataComponentType
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.entity.poi.PoiType
import io.papermc.paper.registry.RegistryKey
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.Biome
import org.bukkit.block.BlockType
import org.bukkit.block.banner.PatternType
import org.bukkit.damage.DamageType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.generator.structure.Structure
import org.bukkit.generator.structure.StructureType
import org.bukkit.inventory.ItemType
import org.bukkit.inventory.MenuType
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.inventory.meta.trim.TrimPattern
import org.bukkit.map.MapCursor
import org.bukkit.potion.PotionEffectType
import org.spongepowered.configurate.serialize.TypeSerializer
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * Supplies a config serializer for every Bukkit registry type, as root defaults.
 *
 * Nothing is supplied while surf-api bootstraps on Paper. A type or registry key missing on the
 * running server version is skipped.
 */
@Component
internal class RegistryConfigSerializers : DefaultConfigSerializerSource {

    override fun serializers(): List<TypeSerializer<*>> {
        if (PaperInstance.isBootstrapping()) return emptyList()

        return buildList {
            //@formatter:off
            addSafely({ GameEvent::class }, { RegistryKey.GAME_EVENT })
            addSafely({ StructureType::class }, { RegistryKey.STRUCTURE_TYPE })
            addSafely({ PotionEffectType::class }, { RegistryKey.MOB_EFFECT })
            addSafely({ BlockType::class }, { RegistryKey.BLOCK })
            addSafely({ ItemType::class }, { RegistryKey.ITEM })
            addSafely({ Villager.Profession::class }, { RegistryKey.VILLAGER_PROFESSION })
            addSafely({ PoiType::class }, { RegistryKey.POINT_OF_INTEREST_TYPE })
            addSafely({ Villager.Type::class }, { RegistryKey.VILLAGER_TYPE })
            addSafely({ MapCursor.Type::class }, { RegistryKey.MAP_DECORATION_TYPE })
            addSafely({ MenuType::class }, { RegistryKey.MENU })
            addSafely({ Attribute::class }, { RegistryKey.ATTRIBUTE })
            addSafely({ Fluid::class }, { RegistryKey.FLUID })
            addSafely({ Sound::class }, { RegistryKey.SOUND_EVENT })
            addSafely({ DataComponentType::class }, { RegistryKey.DATA_COMPONENT_TYPE })
            addSafely({ Biome::class }, { RegistryKey.BIOME })
            addSafely({ Structure::class }, { RegistryKey.STRUCTURE })
            addSafely({ TrimMaterial::class }, { RegistryKey.TRIM_MATERIAL })
            addSafely({ TrimPattern::class }, { RegistryKey.TRIM_PATTERN })
            addSafely({ DamageType::class }, { RegistryKey.DAMAGE_TYPE })
            addSafely({ Wolf.Variant::class }, { RegistryKey.WOLF_VARIANT })
            addSafely({ Wolf.SoundVariant::class }, { RegistryKey.WOLF_SOUND_VARIANT })
            addSafely({ Enchantment::class }, { RegistryKey.ENCHANTMENT })
            addSafely({ JukeboxSong::class }, { RegistryKey.JUKEBOX_SONG })
            addSafely({ PatternType::class }, { RegistryKey.BANNER_PATTERN })
            addSafely({ Art::class }, { RegistryKey.PAINTING_VARIANT })
            addSafely({ MusicInstrument::class }, { RegistryKey.INSTRUMENT })
            addSafely({ Cat.Type::class }, { RegistryKey.CAT_VARIANT })
            addSafely({ Cat.SoundVariant::class }, { RegistryKey.CAT_SOUND_VARIANT })
            addSafely({ Frog.Variant::class }, { RegistryKey.FROG_VARIANT })
            addSafely({ Chicken.Variant::class }, { RegistryKey.CHICKEN_VARIANT })
            addSafely({ Chicken.SoundVariant::class }, { RegistryKey.CHICKEN_SOUND_VARIANT })
            addSafely({ Cow.Variant::class }, { RegistryKey.COW_VARIANT })
            addSafely({ Cow.SoundVariant::class }, { RegistryKey.COW_SOUND_VARIANT })
            addSafely({ Pig.Variant::class }, { RegistryKey.PIG_VARIANT })
            addSafely({ Pig.SoundVariant::class }, { RegistryKey.PIG_SOUND_VARIANT })
            addSafely({ ZombieNautilus.Variant::class }, { RegistryKey.ZOMBIE_NAUTILUS_VARIANT })
            addSafely({ Dialog::class }, { RegistryKey.DIALOG })
            //@formatter:on
            add(
                RegistryValueSerializer(
                    object : TypeToken<GameRule<*>>() {},
                    RegistryKey.GAME_RULE,
                    true,
                ),
            )
        }
    }

    private fun <T : Keyed> MutableList<TypeSerializer<*>>.addSafely(
        type: () -> KClass<T>,
        registryKey: () -> RegistryKey<T>,
    ) {
        try {
            add(RegistryValueSerializer(type().java, registryKey(), true))
        } catch (_: Throwable) {
        }
    }
}

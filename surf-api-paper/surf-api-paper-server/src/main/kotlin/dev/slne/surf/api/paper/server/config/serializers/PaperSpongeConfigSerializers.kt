package dev.slne.surf.api.paper.server.config.serializers

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.config.serializer.SpongeConfigSerializers
import dev.slne.surf.api.paper.server.PaperInstance
import dev.slne.surf.api.paper.server.config.serializers.registry.RegistryValueSerializer
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
import org.bukkit.configuration.serialization.ConfigurationSerializable
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
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import kotlin.reflect.KClass

@Suppress("UnstableApiUsage")
@AutoService(SpongeConfigSerializers::class)
class PaperSpongeConfigSerializers : SpongeConfigSerializers() {

    override fun registerDefaults(builder: TypeSerializerCollection.Builder) {
        super.registerDefaults(builder)

        builder.register(NamespacedKeySerializer)
        builder.register(MaterialSerializer)

        if (!PaperInstance.isBootstrapping()) {
            builder.register(ItemStackSerializer)

            //region Registry serializer
            //@formatter:off
            builder.registerSafeRegistry({ GameEvent::class }, { RegistryKey.GAME_EVENT })
            builder.registerSafeRegistry({ StructureType::class }, { RegistryKey.STRUCTURE_TYPE })
            builder.registerSafeRegistry({ PotionEffectType::class }, { RegistryKey.MOB_EFFECT })
            builder.registerSafeRegistry({ BlockType::class }, { RegistryKey.BLOCK })
            builder.registerSafeRegistry({ ItemType::class }, { RegistryKey.ITEM })
            builder.registerSafeRegistry({ Villager.Profession::class }, { RegistryKey.VILLAGER_PROFESSION })
            builder.registerSafeRegistry({ PoiType::class }, { RegistryKey.POINT_OF_INTEREST_TYPE })
            builder.registerSafeRegistry({ Villager.Type::class }, { RegistryKey.VILLAGER_TYPE })
            builder.registerSafeRegistry({ MapCursor.Type::class }, { RegistryKey.MAP_DECORATION_TYPE })
            builder.registerSafeRegistry({ MenuType::class }, { RegistryKey.MENU })
            builder.registerSafeRegistry({ Attribute::class }, { RegistryKey.ATTRIBUTE })
            builder.registerSafeRegistry({ Fluid::class }, { RegistryKey.FLUID })
            builder.registerSafeRegistry({ Sound::class }, { RegistryKey.SOUND_EVENT })
            builder.registerSafeRegistry({ DataComponentType::class }, { RegistryKey.DATA_COMPONENT_TYPE })
            builder.registerSafeRegistry({ Biome::class }, { RegistryKey.BIOME })
            builder.registerSafeRegistry({ Structure::class }, { RegistryKey.STRUCTURE })
            builder.registerSafeRegistry({ TrimMaterial::class }, { RegistryKey.TRIM_MATERIAL })
            builder.registerSafeRegistry({ TrimPattern::class }, { RegistryKey.TRIM_PATTERN })
            builder.registerSafeRegistry({ DamageType::class }, { RegistryKey.DAMAGE_TYPE })
            builder.registerSafeRegistry({ Wolf.Variant::class }, { RegistryKey.WOLF_VARIANT })
            builder.registerSafeRegistry({ Wolf.SoundVariant::class }, { RegistryKey.WOLF_SOUND_VARIANT })
            builder.registerSafeRegistry({ Enchantment::class }, { RegistryKey.ENCHANTMENT })
            builder.registerSafeRegistry({ JukeboxSong::class }, { RegistryKey.JUKEBOX_SONG })
            builder.registerSafeRegistry({ PatternType::class }, { RegistryKey.BANNER_PATTERN })
            builder.registerSafeRegistry({ Art::class }, { RegistryKey.PAINTING_VARIANT })
            builder.registerSafeRegistry({ MusicInstrument::class }, { RegistryKey.INSTRUMENT })
            builder.registerSafeRegistry({ Cat.Type::class }, { RegistryKey.CAT_VARIANT })
            builder.registerSafeRegistry({ Cat.SoundVariant::class }, { RegistryKey.CAT_SOUND_VARIANT })
            builder.registerSafeRegistry({ Frog.Variant::class }, { RegistryKey.FROG_VARIANT })
            builder.registerSafeRegistry({ Chicken.Variant::class }, { RegistryKey.CHICKEN_VARIANT })
            builder.registerSafeRegistry({ Chicken.SoundVariant::class }, { RegistryKey.CHICKEN_SOUND_VARIANT })
            builder.registerSafeRegistry({ Cow.Variant::class }, { RegistryKey.COW_VARIANT })
            builder.registerSafeRegistry({ Cow.SoundVariant::class }, { RegistryKey.COW_SOUND_VARIANT })
            builder.registerSafeRegistry({ Pig.Variant::class }, { RegistryKey.PIG_VARIANT })
            builder.registerSafeRegistry({ Pig.SoundVariant::class }, { RegistryKey.PIG_SOUND_VARIANT })
            builder.registerSafeRegistry({ ZombieNautilus.Variant::class }, { RegistryKey.ZOMBIE_NAUTILUS_VARIANT })
            builder.registerSafeRegistry({ Dialog::class }, { RegistryKey.DIALOG })
            builder.register(RegistryValueSerializer(object : TypeToken<GameRule<*>>() {}, RegistryKey.GAME_RULE, true))
            //@formatter:on
            //endregion
        }

        // register last to let other serializer override this
        builder.register(
            { type -> type is Class<*> && ConfigurationSerializable::class.java.isAssignableFrom(type) },
            BukkitConfigurationSerializableSerializer
        )
    }

    private fun <T : Keyed> TypeSerializerCollection.Builder.registerSafeRegistry(
        type: () -> KClass<T>,
        registryKey: () -> RegistryKey<T>,
        omitMinecraftNamespace: Boolean = true
    ) {
        try {
            register(RegistryValueSerializer(type().java, registryKey(), omitMinecraftNamespace))
        } catch (_: Throwable) {
        }
    }
}
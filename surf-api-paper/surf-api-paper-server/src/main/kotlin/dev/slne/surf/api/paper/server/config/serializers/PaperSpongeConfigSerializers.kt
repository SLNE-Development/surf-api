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
            builder.register(RegistryValueSerializer(GameEvent::class.java, RegistryKey.GAME_EVENT, true))
            builder.register(RegistryValueSerializer(StructureType::class.java, RegistryKey.STRUCTURE_TYPE, true))
            builder.register(RegistryValueSerializer(PotionEffectType::class.java, RegistryKey.MOB_EFFECT, true))
            builder.register(RegistryValueSerializer(BlockType::class.java, RegistryKey.BLOCK, true))
            builder.register(RegistryValueSerializer(ItemType::class.java, RegistryKey.ITEM, true))
            builder.register(RegistryValueSerializer(Villager.Profession::class.java, RegistryKey.VILLAGER_PROFESSION, true))
            builder.register(RegistryValueSerializer(PoiType::class.java, RegistryKey.POINT_OF_INTEREST_TYPE, true))
            builder.register(RegistryValueSerializer(Villager.Type::class.java, RegistryKey.VILLAGER_TYPE, true))
            builder.register(RegistryValueSerializer(MapCursor.Type::class.java, RegistryKey.MAP_DECORATION_TYPE, true))
            builder.register(RegistryValueSerializer(MenuType::class.java, RegistryKey.MENU, true))
            builder.register(RegistryValueSerializer(Attribute::class.java, RegistryKey.ATTRIBUTE, true))
            builder.register(RegistryValueSerializer(Fluid::class.java, RegistryKey.FLUID, true))
            builder.register(RegistryValueSerializer(Sound::class.java, RegistryKey.SOUND_EVENT, true))
            builder.register(RegistryValueSerializer(DataComponentType::class.java, RegistryKey.DATA_COMPONENT_TYPE, true))
            builder.register(RegistryValueSerializer(object : TypeToken<GameRule<*>>() {}, RegistryKey.GAME_RULE, true))
            builder.register(RegistryValueSerializer(Biome::class.java, RegistryKey.BIOME, true))
            builder.register(RegistryValueSerializer(Structure::class.java, RegistryKey.STRUCTURE, true))
            builder.register(RegistryValueSerializer(TrimMaterial::class.java, RegistryKey.TRIM_MATERIAL, true))
            builder.register(RegistryValueSerializer(TrimPattern::class.java, RegistryKey.TRIM_PATTERN, true))
            builder.register(RegistryValueSerializer(DamageType::class.java, RegistryKey.DAMAGE_TYPE, true))
            builder.register(RegistryValueSerializer(Wolf.Variant::class.java, RegistryKey.WOLF_VARIANT, true))
            builder.register(RegistryValueSerializer(Wolf.SoundVariant::class.java, RegistryKey.WOLF_SOUND_VARIANT, true))
            builder.register(RegistryValueSerializer(Enchantment::class.java, RegistryKey.ENCHANTMENT, true))
            builder.register(RegistryValueSerializer(JukeboxSong::class.java, RegistryKey.JUKEBOX_SONG, true))
            builder.register(RegistryValueSerializer(PatternType::class.java, RegistryKey.BANNER_PATTERN, true))
            builder.register(RegistryValueSerializer(Art::class.java, RegistryKey.PAINTING_VARIANT, true))
            builder.register(RegistryValueSerializer(MusicInstrument::class.java, RegistryKey.INSTRUMENT, true))
            builder.register(RegistryValueSerializer(Cat.Type::class.java, RegistryKey.CAT_VARIANT, true))
            builder.register(RegistryValueSerializer(Cat.SoundVariant::class.java, RegistryKey.CAT_SOUND_VARIANT, true))
            builder.register(RegistryValueSerializer(Frog.Variant::class.java, RegistryKey.FROG_VARIANT, true))
            builder.register(RegistryValueSerializer(Chicken.Variant::class.java, RegistryKey.CHICKEN_VARIANT, true))
            builder.register(RegistryValueSerializer(Chicken.SoundVariant::class.java, RegistryKey.CHICKEN_SOUND_VARIANT, true))
            builder.register(RegistryValueSerializer(Cow.Variant::class.java, RegistryKey.COW_VARIANT, true))
            builder.register(RegistryValueSerializer(Cow.SoundVariant::class.java, RegistryKey.COW_SOUND_VARIANT, true))
            builder.register(RegistryValueSerializer(Pig.Variant::class.java, RegistryKey.PIG_VARIANT, true))
            builder.register(RegistryValueSerializer(Pig.SoundVariant::class.java, RegistryKey.PIG_SOUND_VARIANT, true))
            builder.register(RegistryValueSerializer(ZombieNautilus.Variant::class.java, RegistryKey.ZOMBIE_NAUTILUS_VARIANT, true))
            builder.register(RegistryValueSerializer(Dialog::class.java, RegistryKey.DIALOG, true))
            //@formatter:on
            //endregion
        }

        // register last to let other serializer override this
        builder.register(
            { type -> type is Class<*> && ConfigurationSerializable::class.java.isAssignableFrom(type) },
            BukkitConfigurationSerializableSerializer
        )
    }
}
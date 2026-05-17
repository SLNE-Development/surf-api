package dev.slne.surf.surfapi.bukkit.test.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.api.core.config.constraints.*
import dev.slne.surf.api.core.config.serializer.collection.map.ThrowExceptions
import dev.slne.surf.api.core.config.serializer.collection.map.WriteKeyBack
import dev.slne.surf.api.core.config.type.BooleanOrDefault
import dev.slne.surf.api.core.config.type.ConfigDuration
import dev.slne.surf.api.core.config.type.DurationOrDisabled
import dev.slne.surf.api.core.config.type.number.BelowZeroToEmpty
import dev.slne.surf.api.core.config.type.number.DoubleOr
import dev.slne.surf.api.core.config.type.number.IntOr
import dev.slne.surf.surfapi.bukkit.test.plugin
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.GameRule
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ConfigSerializable
data class ModernSerializerTestConfig(
    // scalar serializers
    val component: Component = Component.text("Hello <world>"),
    val key: Key = Key.key("minecraft:stone"),
    val duration: ConfigDuration = ConfigDuration(30.seconds),
    val uuid: UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
    val regex: Regex = "^[a-z0-9_-]+$".toRegex(),
    val pattern: Pattern = Pattern.compile("^[A-Z_]+$"),
    val uri: URI = URI("https://example.com/api"),
    val url: URL = URI("https://example.com").toURL(),
    val path: Path = Path.of("plugins/Surf/test.yml"),
    val file: File = File("plugins/Surf/test.yml"),
    val textColor: TextColor = TextColor.color(0x55FFFF),

    // wrapper types
    val booleanOrDefault: BooleanOrDefault = BooleanOrDefault.USE_DEFAULT,
    val durationOrDisabled: DurationOrDisabled = DurationOrDisabled.DISABLED,
    val intOrDefault: IntOr.Default = IntOr.Default.USE_DEFAULT,
    val intOrDisabled: IntOr.Disabled = IntOr.Disabled.DISABLED,
    val doubleOrDefault: DoubleOr.Default = DoubleOr.Default.USE_DEFAULT,
    val doubleOrDisabled: DoubleOr.Disabled = DoubleOr.Disabled.DISABLED,

    @BelowZeroToEmpty
    val belowZeroIntDefault: IntOr.Default = IntOr.Default.USE_DEFAULT,

    @BelowZeroToEmpty
    val belowZeroDoubleDisabled: DoubleOr.Disabled = DoubleOr.Disabled.DISABLED,

    // numeric constraints
    @PositiveNumber
    val positiveNumber: Int = 1,

    @NegativeNumber
    val negativeNumber: Int = -1,

    @MinNumber(10.0)
    val minNumber: Int = 10,

    @MaxNumber(100.0)
    val maxNumber: Int = 100,

    @Range(min = 0.0, max = 1.0)
    val chance: Double = 0.5,

    @NotBlank
    val notBlankString: String = "hello",

    @Trimmed
    val trimmedString: String = "hello",

    @MinLength(3)
    val minLengthString: String = "abc",

    @MaxLength(16)
    val maxLengthString: String = "short-text",

    @StartsWith("surf:")
    val startsWithString: String = "surf:test",

    @EndsWith(".yml")
    val endsWithString: String = "config.yml",

    @Contains("surf")
    val containsString: String = "hello-surf-api",

    // collection constraints
    @NotEmpty
    val notEmptyList: List<String> = listOf("one"),

    @MinSize(1)
    val minSizeList: List<String> = listOf("one"),

    @MaxSize(3)
    val maxSizeList: List<String> = listOf("one", "two"),

    @NoDuplicates
    val noDuplicatesList: List<String> = listOf("one", "two"),

    // duration constraints
    @MinDuration(seconds = 5)
    val minDuration: ConfigDuration = ConfigDuration(10.seconds),

    @MaxDuration(seconds = 300)
    val maxDuration: ConfigDuration = ConfigDuration(5.minutes),

    // key / enum-ish constraints
    @Namespace("minecraft")
    val namespacedKey: Key = Key.key("minecraft:dirt"),

    @DisallowValues("DISABLED", "NONE")
    val mode: TestMode = TestMode.ENABLED,

    // path constraints
    @ExistingFile
    val existingFile: Path = Path.of("plugins/SurfPaperPluginTest/test.yml"),

    @Directory
    val directory: Path = Path.of("plugins/Surf"),

    @WritablePath
    val writablePath: Path = Path.of("plugins/SurfPaperPluginTest/test.yml"),

    // map serializers
    val normalMap: Map<String, Int> = mapOf(
        "one" to 1,
        "two" to 2,
    ),

    val strictMap: @ThrowExceptions Map<String, Int> = mapOf(
        "strict" to 1,
    ),

    val keyWriteBackMap: Map<@WriteKeyBack Key, Int> = mapOf(
        Key.key("minecraft:stone") to 1,
    ),

    // fastutil map serializer
    val fastutilMap: Int2ObjectMap<String> = Int2ObjectOpenHashMap<String>().apply {
        put(1, "one")
        put(2, "two")
    },

    val fastUtilInt2Int: Int2IntMap = Int2IntOpenHashMap().apply {
        put(1, 1)
        put(2, 2)
    },

    // paper
    val stack: ItemStack = ItemType.BOOK.createItemStack(),

    val location: Location = Location(plugin.server.worlds.first(), 0.0, 0.0, 0.0),

    val itemType: ItemType = ItemType.STONE,
    val material: Material = Material.STONE,
    val selectedGameRules: List<GameRule<*>> = listOf(GameRules.ADVANCE_TIME, GameRules.MAX_BLOCK_MODIFICATIONS),
) {
    companion object : SpongeYmlConfigClass<ModernSerializerTestConfig>(
        ModernSerializerTestConfig::class.java,
        plugin.dataPath,
        "modern-serializer-test-config.yml"
    )
}

enum class TestMode {
    ENABLED,
    DISABLED,
    NONE
}
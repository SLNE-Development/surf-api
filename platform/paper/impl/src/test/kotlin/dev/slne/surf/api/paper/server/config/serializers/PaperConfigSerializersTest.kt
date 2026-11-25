package dev.slne.surf.api.paper.server.config.serializers

import dev.slne.surf.api.config.ConfigHandles
import dev.slne.surf.api.config.serializer.ConfigSerializerCollections
import dev.slne.surf.api.spring.SurfContexts
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Checks that Paper's config serializers join the root defaults as beans, without a server.
 */
class PaperConfigSerializersTest {

    @TempDir
    lateinit var folder: Path

    private val root = AnnotationConfigApplicationContext().apply {
        scan(
            "dev.slne.surf.api.config.serializer",
            "dev.slne.surf.api.config.type",
            "dev.slne.surf.api.config.impl.serializer",
            "dev.slne.surf.api.paper.server.config.serializers",
        )
        refresh()
        SurfContexts.install(this)
    }

    @AfterEach
    fun close() {
        SurfContexts.uninstall()
        root.close()
    }

    @Test
    fun `a specific Paper serializer wins over the root serializer of its supertype`() {
        val serializers = root.getBean<ConfigSerializerCollections>().withRootDefaults(emptyList())

        assertIs<ItemStackSerializer>(serializers.get(ItemStack::class.java))
        assertIs<NamespacedKeySerializer>(serializers.get(NamespacedKey::class.java))
        assertIs<BukkitConfigurationSerializableSerializer>(serializers.get(Location::class.java))
    }

    @Test
    fun `a location and a namespaced key survive a round trip through a file`() {
        val file = folder.resolve("places.yml")

        ConfigHandles.open<Places>(file).close()
        val reloaded = ConfigHandles.open<Places>(file).use { handle -> handle.reload() }

        assertContains(file.readText(), "surf:spawn")
        assertEquals(Places(), reloaded)
    }

    @ConfigSerializable
    data class Places(
        val spawn: Location = Location(null, 1.5, 64.0, -3.0, 90f, 0f),
        val key: NamespacedKey = NamespacedKey("surf", "spawn"),
    )
}

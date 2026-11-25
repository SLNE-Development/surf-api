package dev.slne.surf.api.config.impl

import dev.slne.surf.api.config.ConfigFormat
import dev.slne.surf.api.config.ConfigHandles
import dev.slne.surf.api.config.migration.ConfigMigration
import dev.slne.surf.api.config.migration.ConfigMigrations
import dev.slne.surf.api.config.migration.configMigrations
import dev.slne.surf.api.config.serializer.SpongeConfigSerializers
import dev.slne.surf.api.spring.SurfContexts
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.*

class ConfigHandlesTest {

    @TempDir
    lateinit var folder: Path

    private val root = AnnotationConfigApplicationContext().apply {
        beanFactory.registerSingleton("configSerializers", object : SpongeConfigSerializers() {})
        refresh()
        SurfContexts.install(this)
    }

    @AfterEach
    fun close() {
        SurfContexts.uninstall()
        root.close()
    }

    @Test
    fun `open writes the defaults to a file in a missing folder`() {
        val file = folder.resolve("nested/settings.yml")

        ConfigHandles.open<Settings>(file).use { handle ->
            assertEquals(Settings(), handle.value)
        }

        assertContains(file.readText(), "name: default")
    }

    @Test
    fun `open takes JSON from the json extension`() {
        val file = folder.resolve("inferred.json")

        ConfigHandles.open<Settings>(file).close()

        assertContains(file.readText(), "\"name\"")
    }

    @Test
    fun `open falls back to YAML for an unknown extension`() {
        val file = folder.resolve("settings.conf")

        ConfigHandles.open<Settings>(file).close()

        assertContains(file.readText(), "name: default")
    }

    @Test
    fun `an explicit format wins over the extension`() {
        val file = folder.resolve("forced.yml")

        ConfigHandles.open<Settings>(file, ConfigFormat.JSON).close()

        assertContains(file.readText(), "\"name\"")
    }

    @Test
    fun `open writes JSON when asked to`() {
        val file = folder.resolve("settings.json")

        ConfigHandles.open<Settings>(file, ConfigFormat.JSON).close()

        assertContains(file.readText(), "\"name\"")
    }

    @Test
    fun `sparse migrations run from the stored version on, under a custom version key`() {
        val file = folder.resolve("settings.yml")
        file.writeText("meta:\n  revision: 2\ntitle: kept\nsize: 3\n")
        val migrations = configMigrations<Settings> {
            versionKey("meta", "revision")
            version(1) { node -> node.node("size").set(100) }
            version(4) { node ->
                val title = node.node("title")
                node.node("name").set(title.raw())
                title.raw(null)
            }
            version(9) { node -> node.node("size").set(node.node("size").getInt() * 2) }
        }

        ConfigHandles.open(file, migrations = migrations).use { handle ->
            assertEquals(Settings(name = "kept", size = 6), handle.value)
        }

        assertContains(file.readText(), "revision: 9")
    }

    @Test
    fun `a version registered twice is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            configMigrations<Settings> {
                version(1, ConfigMigration { })
                version(1, ConfigMigration { })
            }
        }
    }

    @Test
    fun `a negative version is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            configMigrations<Settings> { version(-1, ConfigMigration { }) }
        }
    }

    @Test
    fun `an empty or blank version key is rejected`() {
        val builder = ConfigMigrations.builder<Settings>()

        assertFailsWith<IllegalArgumentException> { builder.versionKey() }
        assertFailsWith<IllegalArgumentException> { builder.versionKey("meta", " ") }
    }

    @Test
    fun `the format follows the extension, ignoring case`() {
        assertEquals(ConfigFormat.YAML, ConfigFormat.ofPath(Path("A.YML")))
        assertEquals(ConfigFormat.YAML, ConfigFormat.ofPath(Path("a.yaml")))
        assertEquals(ConfigFormat.JSON, ConfigFormat.ofPath(Path("a.json")))
        assertNull(ConfigFormat.ofPath(Path("a.txt")))
    }

    @Test
    fun `reload installs the file's value without rewriting an unmigrated file`() {
        val file = folder.resolve("settings.yml")
        ConfigHandles.open<Settings>(file).use { handle ->
            file.writeText("# hand-written\nname: edited\nsize: 2\nunknown-key: 1\n")
            val written = file.readBytes()

            assertEquals(Settings(name = "edited", size = 2), handle.reload())
            assertEquals(Settings(name = "edited", size = 2), handle.value)
            assertContentEquals(written, file.readBytes())
        }
    }

    @Test
    fun `reload keeps the current value and the file when the file was emptied`() {
        val file = folder.resolve("settings.yml")
        ConfigHandles.open<Settings>(file).use { handle ->
            handle.update { settings -> settings.copy(name = "edited") }
            file.writeText("")

            assertEquals(Settings(name = "edited"), handle.reload())
            assertEquals(Settings(name = "edited"), handle.value)
            assertEquals("", file.readText())
        }
    }

    @Test
    fun `reload keeps the current value when a JSON file was emptied`() {
        val file = folder.resolve("settings.json")
        ConfigHandles.open<Settings>(file).use { handle ->
            handle.update { settings -> settings.copy(name = "edited") }
            file.writeText("")

            assertEquals(Settings(name = "edited"), handle.reload())
            assertEquals("", file.readText())
        }
    }

    @Test
    fun `reload keeps the value and the file when the candidate fails validation`() {
        val file = folder.resolve("settings.yml")
        val migrations = configMigrations<Settings> {
            version(1) { node -> node.node("size").set(node.node("size").getInt() + 1) }
        }
        ConfigHandles.open(file, migrations = migrations).use { handle ->
            val current = handle.value
            file.writeText("name: broken\nsize: -5\n")
            val written = file.readBytes()

            assertFailsWith<IllegalStateException> {
                handle.reload { candidate -> check(candidate.size >= 0) { "negative size" } }
            }

            assertEquals(current, handle.value)
            assertContentEquals(written, file.readBytes())
        }
    }

    @Test
    fun `reload writes the file back when a migration changed it`() {
        val file = folder.resolve("settings.yml")
        val migrations = configMigrations<Settings> {
            version(1) { node -> node.node("size").set(node.node("size").getInt() + 1) }
        }
        ConfigHandles.open(file, migrations = migrations).use { handle ->
            file.writeText("name: legacy\nsize: 1\n")

            assertEquals(Settings(name = "legacy", size = 2), handle.reload())
        }

        val rewritten = file.readText()
        assertContains(rewritten, "size: 2")
        assertContains(rewritten, "${ConfigMigrations.DEFAULT_VERSION_KEY}: 1")
    }

    @Test
    fun `a closed handle keeps its value and rejects changes`() {
        val handle = ConfigHandles.open<Settings>(folder.resolve("settings.yml"))
        handle.close()
        handle.close()

        assertEquals(Settings(), handle.value)
        assertFailsWith<IllegalStateException> { handle.save() }
        assertFailsWith<IllegalStateException> { handle.reload() }
        assertFailsWith<IllegalStateException> { handle.update { it.copy(size = 9) } }
        assertFalse(handle.value.size == 9)
    }

    @Test
    fun `a closed handle rejects edits`() {
        val handle = ConfigHandles.open<Counter>(folder.resolve("counter.yml"))
        handle.close()

        assertFailsWith<IllegalStateException> { handle.edit { count = 1 } }
        assertEquals(0, handle.value.count)
    }

    @ConfigSerializable
    data class Settings(val name: String = "default", val size: Int = 1)

    @ConfigSerializable
    class Counter {
        @Comment("How often it happened")
        var count: Int = 0
    }
}

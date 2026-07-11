package dev.slne.surf.api.core.config.migration

import org.spongepowered.configurate.BasicConfigurationNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigMigrationBuilderTest {
    @Test
    fun `unversioned config runs migrations in version and registration order`() {
        val calls = mutableListOf<String>()
        val builder = ConfigMigrationBuilder()
            .migration(2) { calls += "v2-first" }
            .migration(1) { calls += "v1" }
            .migration(2) { calls += "v2-second" }
        val node = BasicConfigurationNode.root()

        val result = builder.migrate(node)

        assertEquals(listOf("v1", "v2-first", "v2-second"), calls)
        assertEquals(-1, result.fromVersion)
        assertEquals(2, result.toVersion)
        assertTrue(result.migrated)
        assertEquals(2, node.node(ConfigMigrationBuilder.DEFAULT_VERSION_KEY).int)
    }

    @Test
    fun `already current config does not rerun migrations`() {
        var calls = 0
        val builder = ConfigMigrationBuilder().migration(1) { calls++ }
        val node = BasicConfigurationNode.root()

        builder.migrate(node)
        val second = builder.migrate(node)

        assertEquals(1, calls)
        assertFalse(second.migrated)
        assertEquals(1, second.fromVersion)
        assertEquals(1, second.toVersion)
    }

    @Test
    fun `custom version key is used`() {
        val builder = ConfigMigrationBuilder()
            .versionKey("meta", "version")
            .migration(3) { it.node("migrated").set(true) }
        val node = BasicConfigurationNode.root()

        builder.migrate(node)

        assertEquals(3, node.node("meta", "version").int)
        assertTrue(node.node("migrated").boolean)
    }

    @Test
    fun `empty builder and invalid versions are handled explicitly`() {
        val builder = ConfigMigrationBuilder()
        val result = builder.migrate(BasicConfigurationNode.root())

        assertFalse(builder.hasMigrations())
        assertEquals(-1, builder.latestVersion())
        assertFalse(result.migrated)
        assertFailsWith<IllegalArgumentException> { builder.migration(-1) { } }
    }
}

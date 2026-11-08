package dev.slne.surf.api.minestom.server.configuration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConfigurationPhaseBuilderTest {

    private val custom = ConfigurationTaskId("custom")
    private val noop = ConfigurationTask { }

    @Test
    fun `starts with the vanilla steps`() {
        assertEquals(
            listOf(
                ConfigurationTasks.PLAYER_COUNT,
                ConfigurationTasks.BRAND,
                ConfigurationTasks.CONFIGURATION_EVENT,
                ConfigurationTasks.SPAWN_LOCATION,
                ConfigurationTasks.ENABLED_FEATURES,
                ConfigurationTasks.SYNCHRONIZE_REGISTRIES,
                ConfigurationTasks.AWAIT_SETTINGS,
                ConfigurationTasks.RESOURCE_PACK,
                ConfigurationTasks.JOIN_WORLD,
            ),
            ConfigurationPhaseBuilder().ids,
        )
    }

    @Test
    fun `replace keeps the position of the step`() {
        val builder = ConfigurationPhaseBuilder()
        builder.replace(ConfigurationTasks.RESOURCE_PACK, noop)

        val tasks = builder.build()
        assertEquals(7, tasks.indexOfFirst { it.first == ConfigurationTasks.RESOURCE_PACK })
        assertSame(noop, tasks.single { it.first == ConfigurationTasks.RESOURCE_PACK }.second)
    }

    @Test
    fun `steps are inserted around their anchor`() {
        val builder = ConfigurationPhaseBuilder()
        builder.before(ConfigurationTasks.JOIN_WORLD, custom, noop)
        builder.after(ConfigurationTasks.BRAND, ConfigurationTaskId("after_brand"), noop)
        builder.first(ConfigurationTaskId("first"), noop)

        val ids = builder.ids
        assertEquals(ConfigurationTaskId("first"), ids.first())
        assertEquals(ConfigurationTaskId("after_brand"), ids[ids.indexOf(ConfigurationTasks.BRAND) + 1])
        assertEquals(custom, ids[ids.indexOf(ConfigurationTasks.JOIN_WORLD) - 1])
    }

    @Test
    fun `remove drops the step`() {
        val builder = ConfigurationPhaseBuilder()
        builder.remove(ConfigurationTasks.RESOURCE_PACK)

        assertEquals(8, builder.ids.size)
        assertThrows<IllegalArgumentException> { builder.remove(ConfigurationTasks.RESOURCE_PACK) }
    }

    @Test
    fun `the code of conduct runs once the settings arrived`() {
        val builder = ConfigurationPhaseBuilder()
        builder.codeOfConduct { "Be nice" }

        val ids = builder.ids
        assertEquals(ids.indexOf(ConfigurationTasks.AWAIT_SETTINGS) + 1, ids.indexOf(ConfigurationTasks.CODE_OF_CONDUCT))
    }

    @Test
    fun `an id cannot be used twice`() {
        val builder = ConfigurationPhaseBuilder()
        assertThrows<IllegalArgumentException> { builder.last(ConfigurationTasks.BRAND, noop) }
        assertThrows<IllegalArgumentException> { builder.before(custom, ConfigurationTaskId("other"), noop) }
    }
}

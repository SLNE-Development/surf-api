package dev.slne.surf.api.paper

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MockBukkitSmokeTest {

    private lateinit var server: ServerMock

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `server mock boots and serves basic api`() {
        assertNotNull(server.bukkitVersion)
        val world = server.addSimpleWorld("smoke")
        assertNotNull(world)
        assertTrue(server.worlds.isNotEmpty())
    }
}

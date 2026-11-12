package dev.slne.surf.api.minestom.server.chat

import dev.slne.surf.api.minestom.chat.ChatRenderer
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

@EnvTest
class ViewerUnawareRendererTest {

    @Test
    fun `identical arguments render once`(env: Env) = runBlocking {
        val player = env.createPlayer(env.createFlatInstance(), Pos.ZERO)
        var calls = 0
        val renderer = ChatRenderer.viewerUnaware { _, name, message ->
            calls++
            name.append(message)
        }
        val name = Component.text("name")
        val message = Component.text("message")

        val first = renderer.render(player, name, message, Audience.empty())
        val second = renderer.render(player, name, message, Audience.empty())

        assertSame(first, second)
        assertEquals(1, calls)
    }

    @Test
    fun `different arguments render again`(env: Env) = runBlocking {
        val player = env.createPlayer(env.createFlatInstance(), Pos.ZERO)
        val renderer = ChatRenderer.viewerUnaware { _, name, message -> name.append(message) }
        val name = Component.text("name")

        val first = renderer.render(player, name, Component.text("first"), Audience.empty())
        val second = renderer.render(player, name, Component.text("second"), Audience.empty())

        assertEquals(name.append(Component.text("first")), first)
        assertEquals(name.append(Component.text("second")), second)
    }
}

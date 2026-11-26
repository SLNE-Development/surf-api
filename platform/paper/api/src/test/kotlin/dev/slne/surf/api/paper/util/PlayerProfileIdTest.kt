package dev.slne.surf.api.paper.util

import com.destroystokyo.paper.profile.PlayerProfile
import com.mojang.brigadier.LiteralMessage
import dev.slne.surf.api.command.exception.CommandMessageFactory
import dev.slne.surf.api.command.exception.WrapperCommandExceptionComponent
import dev.slne.surf.api.messages.Colors
import dev.slne.surf.api.spring.SurfContexts
import io.mockk.every
import io.mockk.mockk
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.support.GenericApplicationContext
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerProfileIdTest {

    private lateinit var root: GenericApplicationContext

    @BeforeEach
    fun installRoot() {
        val factory = CommandMessageFactory { LiteralMessage("error") }

        root = GenericApplicationContext()
        root.beanFactory.registerSingleton("commandMessageFactory", factory)
        root.refresh()

        SurfContexts.install(root)
    }

    @AfterEach
    fun uninstallRoot() {
        SurfContexts.uninstall()
        root.close()
    }

    @Test
    fun `idOrFail returns the id of a profile that has one`() {
        val id = UUID.fromString("5f3b0c1e-8d6a-4f0b-9a7e-2c1d4b6e8f90")
        val profile = profile(id = id, name = "Steve")

        assertEquals(id, profile.idOrFail())
    }

    @Test
    fun `idOrFail fails the command with an error message naming the profile`() {
        val profile = profile(id = null, name = "Steve")

        val failure = assertThrows<WrapperCommandExceptionComponent> { profile.idOrFail() }
        val message = failureMessage(failure)

        assertTrue("Steve" in plainText(message), "The message does not name the profile.")
        assertTrue(Colors.ERROR in colorsOf(message), "The message is not error-styled.")
    }

    @Test
    fun `idOrThrow throws IllegalStateException for a profile without an id`() {
        val profile = profile(id = null, name = "Steve")

        assertThrows<IllegalStateException> { profile.idOrThrow() }
    }

    private fun profile(id: UUID?, name: String?): PlayerProfile {
        val profile = mockk<PlayerProfile>()
        every { profile.id } returns id
        every { profile.name } returns name

        return profile
    }

    private fun failureMessage(failure: WrapperCommandExceptionComponent): Component {
        return failure.component
    }

    private fun plainText(component: Component): String {
        return PlainTextComponentSerializer.plainText().serialize(component)
    }

    private fun colorsOf(component: Component): Set<TextColor?> {
        val colors = mutableSetOf(component.color())
        for (child in component.children()) {
            colors += colorsOf(child)
        }

        return colors
    }
}

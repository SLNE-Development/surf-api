package dev.slne.surf.api.command.exception

import com.mojang.brigadier.Message
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.slne.surf.api.spring.SurfContexts
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.support.GenericApplicationContext
import kotlin.test.assertEquals
import kotlin.test.assertSame

class WrapperCommandExceptionComponentTest {

    private class FakeMessage(val component: Component) : Message {
        override fun getString(): String = "fake:" + (component as TextComponent).content()
    }

    private val createdMessages = mutableListOf<FakeMessage>()
    private lateinit var root: GenericApplicationContext

    @BeforeEach
    fun installRoot() {
        val factory = CommandMessageFactory { component ->
            FakeMessage(component).also { createdMessages += it }
        }

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
    fun `builds the message of the exception through the factory`() {
        val component = Component.text("Kein Zugriff")

        val exception = WrapperCommandExceptionComponent(component)

        val message = createdMessages.single()
        assertSame(component, message.component)
        assertSame(message, exception.exception.rawMessage)
        assertEquals("fake:Kein Zugriff", exception.message)
    }

    @Test
    fun `uses the factory message for the exception type as well`() {
        val exception = WrapperCommandExceptionComponent(Component.text("Kein Zugriff"))

        val type = exception.exception.type as SimpleCommandExceptionType

        assertSame(createdMessages.single(), type.create().rawMessage)
    }

    @Test
    fun `keeps the original component`() {
        val component = Component.text("Kein Zugriff")

        val exception = WrapperCommandExceptionComponent(component)

        assertSame(component, exception.component)
    }
}

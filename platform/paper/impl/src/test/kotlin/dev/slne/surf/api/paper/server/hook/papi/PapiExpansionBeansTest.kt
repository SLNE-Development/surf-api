package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.paper.hook.papi.PapiExpansion
import dev.slne.surf.api.paper.hook.papi.PapiExpansionFallback
import dev.slne.surf.api.paper.hook.papi.Placeholder
import io.mockk.mockk
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import kotlin.test.*

class PapiExpansionBeansTest {

    private val onlinePlayer = mockk<Player>()
    private val offlinePlayer = mockk<OfflinePlayer>()

    @Test
    fun `offline player method receives every request`() {
        val resolver = define(Placeholders()).resolver

        assertEquals("offline:true", resolver.resolve(offlinePlayer, "offline"))
        assertEquals("offline:false", resolver.resolve(null, "offline"))
    }

    @Test
    fun `non-null offline player method resolves only requests with a player`() {
        val resolver = define(Placeholders()).resolver

        assertEquals("present", resolver.resolve(offlinePlayer, "present"))
        assertNull(resolver.resolve(null, "present"))
    }

    @Test
    fun `player method resolves only online players`() {
        val resolver = define(Placeholders()).resolver

        assertEquals("online", resolver.resolve(onlinePlayer, "online"))
        assertNull(resolver.resolve(offlinePlayer, "online"))
        assertNull(resolver.resolve(null, "online"))
    }

    @Test
    fun `arguments are the unsplit rest of the request`() {
        val resolver = define(Placeholders()).resolver

        assertEquals("args:a_b_c", resolver.resolve(onlinePlayer, "args_a_b_c"))
        assertEquals("args:", resolver.resolve(onlinePlayer, "args"))
    }

    @Test
    fun `component results are serialized to legacy text`() {
        val resolver = define(Placeholders()).resolver

        assertEquals("§chi", resolver.resolve(offlinePlayer, "component"))
    }

    @Test
    fun `empty and unknown requests go to the fallback`() {
        val resolver = define(Placeholders()).resolver

        assertEquals("fallback:", resolver.resolve(offlinePlayer, ""))
        assertEquals("fallback:missing_x", resolver.resolve(offlinePlayer, "missing_x"))
    }

    @Test
    fun `requests without a fallback resolve to null`() {
        val resolver = define(SeparatePlaceholders()) { ExternalPlaceholders() }.resolver

        assertNull(resolver.resolve(offlinePlayer, ""))
        assertNull(resolver.resolve(offlinePlayer, "missing"))
    }

    @Test
    fun `placeholder classes contribute their methods`() {
        val external = ExternalPlaceholders()
        val definition = define(SeparatePlaceholders()) { type ->
            assertSame(ExternalPlaceholders::class.java, type)
            external
        }

        assertEquals("external", definition.resolver.resolve(offlinePlayer, "external"))
    }

    @Test
    fun `kotlin objects contribute their instance without instanceOf`() {
        val definition = define(ObjectSource()) { type -> fail("instanceOf called for $type") }

        assertEquals("object", definition.resolver.resolve(offlinePlayer, "object"))
    }

    @Test
    fun `blank author and version use the plugin's`() {
        val bean = SeparatePlaceholders()
        val definition = define(bean) { ExternalPlaceholders() }

        assertEquals("plugin-author", definition.author)
        assertEquals("1.0", definition.version)
        assertSame(bean, definition.owner)
    }

    @Test
    fun `declared author and version win`() {
        val definition = define(Placeholders())

        assertEquals("someone", definition.author)
        assertEquals("2.0", definition.version)
    }

    @Test
    fun `duplicate placeholder names are rejected`() {
        assertFailsWith<IllegalArgumentException> { define(DuplicatePlaceholders()) }
    }

    @Test
    fun `reserved characters in placeholder names are rejected`() {
        assertFailsWith<IllegalArgumentException> { define(ReservedName()) }
    }

    @Test
    fun `reserved characters in the identifier are rejected`() {
        assertFailsWith<IllegalArgumentException> { define(ReservedIdentifier()) }
    }

    @Test
    fun `empty identifiers and placeholder names are rejected`() {
        assertFailsWith<IllegalArgumentException> { define(EmptyIdentifier()) }
        assertFailsWith<IllegalArgumentException> { define(EmptyName()) }
    }

    @Test
    fun `invalid signatures are rejected`() {
        assertFailsWith<IllegalArgumentException> { define(WrongPlayerType()) }
        assertFailsWith<IllegalArgumentException> { define(WrongReturnType()) }
        assertFailsWith<IllegalArgumentException> { define(WrongArgumentType()) }
    }

    private fun define(
        bean: Any,
        instanceOf: (Class<*>) -> Any = { error("No placeholder classes expected") },
    ): ExpansionDefinition {
        return PapiExpansionBeans.define(
            bean = bean,
            annotation = bean.javaClass.getAnnotation(PapiExpansion::class.java),
            defaultAuthor = "plugin-author",
            defaultVersion = "1.0",
            instanceOf = instanceOf,
        )
    }

    @PapiExpansion("test", author = "someone", version = "2.0")
    class Placeholders : PapiExpansionFallback {

        @Placeholder("offline")
        fun offline(player: OfflinePlayer?): String = "offline:${player != null}"

        @Placeholder("present")
        fun present(player: OfflinePlayer): String = "present"

        @Placeholder("online")
        fun online(player: Player): String = "online"

        @Placeholder("args")
        fun args(player: Player, args: String): String = "args:$args"

        @Placeholder("component")
        fun component(player: OfflinePlayer?): Component = Component.text("hi", NamedTextColor.RED)

        override fun onRequest(player: OfflinePlayer?, params: String): String = "fallback:$params"
    }

    @PapiExpansion("separate", placeholders = [ExternalPlaceholders::class])
    class SeparatePlaceholders

    @PapiExpansion("objectsource", placeholders = [ObjectPlaceholders::class])
    class ObjectSource

    object ObjectPlaceholders {

        @Placeholder("object")
        fun value(player: OfflinePlayer?): String = "object"
    }

    class ExternalPlaceholders {

        @Placeholder("external")
        fun external(player: OfflinePlayer?): String = "external"
    }

    @PapiExpansion("duplicate")
    class DuplicatePlaceholders {

        @Placeholder("same")
        fun first(player: OfflinePlayer?): String? = null

        @Placeholder("same")
        fun second(player: OfflinePlayer?): String? = null
    }

    @PapiExpansion("reserved")
    class ReservedName {

        @Placeholder("with_underscore")
        fun value(player: OfflinePlayer?): String? = null
    }

    @PapiExpansion("with_underscore")
    class ReservedIdentifier

    @PapiExpansion("")
    class EmptyIdentifier

    @PapiExpansion("emptyname")
    class EmptyName {

        @Placeholder("")
        fun value(player: OfflinePlayer?): String? = null
    }

    @PapiExpansion("wrongplayer")
    class WrongPlayerType {

        @Placeholder("value")
        fun value(player: String): String? = null
    }

    @PapiExpansion("wrongreturn")
    class WrongReturnType {

        @Placeholder("value")
        fun value(player: OfflinePlayer?): Int = 0
    }

    @PapiExpansion("wrongargs")
    class WrongArgumentType {

        @Placeholder("value")
        fun value(player: OfflinePlayer?, args: Int): String? = null
    }
}

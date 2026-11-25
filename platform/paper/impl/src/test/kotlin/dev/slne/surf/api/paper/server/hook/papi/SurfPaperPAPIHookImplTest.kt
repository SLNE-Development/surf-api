package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.paper.hook.papi.expansion.PapiExpansion
import dev.slne.surf.api.paper.hook.papi.expansion.PapiPlaceholder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.bukkit.OfflinePlayer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SurfPaperPAPIHookImplTest {

    private val holders = RecordingHolders()
    private val hook = SurfPaperPAPIHookImpl(holders)

    @Test
    fun `registrations before enable are attached on enable`() = runTest {
        hook.register(definition("early"))
        assertEquals(emptyList<String>(), holders.events)

        hook.onEnable()

        assertEquals(listOf("attach early"), holders.events)
    }

    @Test
    fun `registrations after enable are attached right away`() = runTest {
        hook.onEnable()

        hook.register(definition("late"))

        assertEquals(listOf("attach late"), holders.events)
    }

    @Test
    fun `duplicate identifiers are rejected`() {
        hook.register(definition("taken"))

        assertFailsWith<IllegalArgumentException> { hook.register(definition("taken")) }
    }

    @Test
    fun `unregister ignores other owners`() = runTest {
        hook.onEnable()
        hook.register(definition("owned", owner = "first"))

        hook.unregister("owned", owner = "second")
        hook.unregister("owned", owner = "first")

        assertEquals(listOf("attach owned", "detach owned"), holders.events)
    }

    @Test
    fun `unregistered identifiers can be registered again`() {
        val owner = Any()
        hook.register(definition("again", owner))
        hook.unregister("again", owner)

        hook.register(definition("again"))
    }

    @Test
    fun `a rejected attach leaves nothing registered`() = runTest {
        hook.onEnable()
        holders.rejected += "rejected"

        assertFailsWith<IllegalStateException> { hook.register(definition("rejected")) }
        holders.rejected.clear()
        hook.register(definition("rejected"))
    }

    @Test
    fun `disable detaches and forgets every registration`() = runTest {
        hook.onEnable()
        hook.register(definition("first"))
        hook.register(definition("second"))

        hook.onDisable()
        hook.register(definition("first"))

        assertEquals(
            setOf("attach first", "attach second", "detach first", "detach second"),
            holders.events.toSet(),
        )
        assertEquals(4, holders.events.size)
    }

    @Test
    fun `manual expansions resolve through their placeholders`() {
        val expansion = object : PapiExpansion(
            identifier = "manual",
            placeholder = listOf(EchoPlaceholder()),
            author = "author",
            version = "1.0",
        ) {
            override fun parseWithNoParams(player: OfflinePlayer): String = "bare"
        }
        val resolver = ManualExpansionResolver(expansion)
        val player = mockk<OfflinePlayer>()

        assertEquals("a,b", resolver.resolve(player, "echo_a_b"))
        assertEquals("bare", resolver.resolve(player, ""))
        assertNull(resolver.resolve(player, "unknown"))
        assertNull(resolver.resolve(null, "echo"))
    }

    @Test
    fun `manual expansions register through the hook`() = runTest {
        val expansion = PapiExpansion("manual", emptyList(), author = "author", version = "1.0")
        hook.onEnable()

        hook.register(expansion)
        hook.unregister(expansion)

        assertEquals(listOf("attach manual", "detach manual"), holders.events)
    }

    @Test
    fun `manual expansions reject empty and reserved names`() {
        assertFailsWith<IllegalArgumentException> {
            PapiExpansion("", emptyList(), author = "author", version = "1.0")
        }
        assertFailsWith<IllegalArgumentException> {
            PapiExpansion("with}brace", emptyList(), author = "author", version = "1.0")
        }
        assertFailsWith<IllegalArgumentException> { EmptyPlaceholder() }
    }

    private fun definition(identifier: String, owner: Any = Any()): ExpansionDefinition {
        return ExpansionDefinition(
            identifier = identifier,
            author = "author",
            version = "1.0",
            resolver = { _, _ -> null },
            owner = owner,
        )
    }

    private class EchoPlaceholder : PapiPlaceholder("echo") {
        override fun parse(player: OfflinePlayer, args: List<String>): String {
            return args.joinToString(",")
        }
    }

    private class EmptyPlaceholder : PapiPlaceholder("") {
        override fun parse(player: OfflinePlayer, args: List<String>): String? = null
    }

    private class RecordingHolders : ExpansionHolderFactory {
        val events = mutableListOf<String>()
        val rejected = mutableSetOf<String>()

        override fun create(definition: ExpansionDefinition): ExpansionHolder {
            val identifier = definition.identifier

            return object : ExpansionHolder {
                override fun attach() {
                    check(identifier !in rejected) { "Rejected $identifier" }

                    events += "attach $identifier"
                }

                override fun detach() {
                    events += "detach $identifier"
                }
            }
        }
    }
}

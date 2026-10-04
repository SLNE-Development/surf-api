package dev.slne.surf.api.minestom.server.plugins

import dev.slne.surf.api.minestom.plugin.PluginDependency
import dev.slne.surf.api.minestom.plugin.PluginMeta
import dev.slne.surf.api.minestom.server.plugins.impl.PluginCandidate
import dev.slne.surf.api.minestom.server.plugins.impl.PluginLoadOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PluginLoadOrderTest {

    private fun plugin(id: String, vararg dependencies: PluginDependency) =
        PluginCandidate(PluginMeta(id, "example.Main", dependencies = dependencies.toList()), jar = null)

    private fun required(id: String) = PluginDependency(id)
    private fun optional(id: String) = PluginDependency(id, optional = true)

    private fun PluginLoadOrder.Result.ids() = ordered.map { it.meta.id }

    @Test
    fun `dependencies come before the plugins that need them`() {
        val result = PluginLoadOrder.order(
            listOf(
                plugin("chat", required("core")),
                plugin("lobby", required("chat"), optional("queue")),
                plugin("core"),
                plugin("queue"),
            )
        )

        val ids = result.ids()
        assertTrue(ids.indexOf("core") < ids.indexOf("chat"))
        assertTrue(ids.indexOf("chat") < ids.indexOf("lobby"))
        assertTrue(ids.indexOf("queue") < ids.indexOf("lobby"))
        assertEquals(emptyMap<String, String>(), result.rejected)
    }

    @Test
    fun `a missing required dependency rejects the plugin and its dependents`() {
        val result = PluginLoadOrder.order(
            listOf(
                plugin("chat", required("core")),
                plugin("lobby", required("chat")),
                plugin("standalone", optional("core")),
            )
        )

        assertEquals(listOf("standalone"), result.ids())
        assertEquals(setOf("chat", "lobby"), result.rejected.keys)
    }

    @Test
    fun `a dependency cycle rejects every member`() {
        val result = PluginLoadOrder.order(
            listOf(
                plugin("a", required("b")),
                plugin("b", required("a")),
                plugin("c"),
            )
        )

        assertEquals(listOf("c"), result.ids())
        assertTrue(result.rejected.keys.containsAll(setOf("a", "b")))
    }

    @Test
    fun `a duplicate id keeps the first plugin`() {
        val first = plugin("core")
        val result = PluginLoadOrder.order(listOf(first, plugin("core")))

        assertEquals(listOf(first), result.ordered)
        assertEquals(1, result.rejected.size)
    }
}

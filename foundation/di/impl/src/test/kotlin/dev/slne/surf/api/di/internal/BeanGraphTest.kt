package dev.slne.surf.api.di.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BeanGraphTest {

    @Test
    fun `a dependency is created before what needs it`() {
        val order = BeanGraph.order(
            indexOf(
                definition("Service", needs = listOf(needs("Repository"))),
                definition("Repository"),
            )
        ).ids()

        assertEquals(listOf("Repository", "Service"), order)
    }

    @Test
    fun `a chain is ordered end to end`() {
        val order = BeanGraph.order(
            indexOf(
                definition("Top", needs = listOf(needs("Middle"))),
                definition("Middle", needs = listOf(needs("Bottom"))),
                definition("Bottom"),
            )
        ).ids()

        assertEquals(listOf("Bottom", "Middle", "Top"), order)
    }

    @Test
    fun `a cycle fails and names the path`() {
        val failure = assertFailsWith<CyclicDependencyException> {
            BeanGraph.order(
                indexOf(
                    definition("A", needs = listOf(needs("B"))),
                    definition("B", needs = listOf(needs("A"))),
                )
            )
        }

        assertTrue(failure.message!!.contains("A"), failure.message)
        assertTrue(failure.message!!.contains("B"), failure.message)
        assertTrue(failure.message!!.contains("Lazy"), failure.message)
    }

    @Test
    fun `a lazy parameter breaks a cycle instead of failing`() {
        val order = BeanGraph.order(
            indexOf(
                definition("A", needs = listOf(needs("B"))),
                definition("B", needs = listOf(needsLazy("A"))),
            )
        ).ids()

        assertEquals(listOf("B", "A"), order)
    }

    @Test
    fun `an optional parameter nothing provides creates no edge`() {
        val order = BeanGraph.order(
            indexOf(definition("Service", needs = listOf(needsOptional("Absent"))))
        ).ids()

        assertEquals(listOf("Service"), order)
    }

    @Test
    fun `a collection parameter waits for every provider of its element type`() {
        val order = BeanGraph.order(
            indexOf(
                definition("Registrar", needs = listOf(needsAll("Handler"))),
                definition("FirstHandler", provides = listOf("FirstHandler", "Handler")),
                definition("SecondHandler", provides = listOf("SecondHandler", "Handler")),
            )
        ).ids()

        assertEquals("Registrar", order.last())
        assertTrue(order.indexOf("FirstHandler") < order.indexOf("Registrar"))
        assertTrue(order.indexOf("SecondHandler") < order.indexOf("Registrar"))
    }

    @Test
    fun `a factory is created after the bean that declares it`() {
        val order = BeanGraph.order(
            indexOf(
                definition("Consumer", needs = listOf(needs("RedisApi"))),
                definition("RedisConnection", factories = listOf(factory("api", "RedisApi"))),
            )
        ).ids()

        assertEquals(listOf("RedisConnection", "RedisConnection#api", "Consumer"), order)
    }

    @Test
    fun `priority orders beans the graph leaves unordered`() {
        val order = BeanGraph.order(
            indexOf(
                definition("Late", priority = 100),
                definition("Early", priority = -100),
                definition("Middle"),
            )
        ).ids()

        assertEquals(listOf("Early", "Middle", "Late"), order)
    }

    @Test
    fun `priority cannot move a bean before what it depends on`() {
        val order = BeanGraph.order(
            indexOf(
                definition("Dependent", needs = listOf(needs("Dependency")), priority = -100),
                definition("Dependency", priority = 100),
            )
        ).ids()

        assertEquals(listOf("Dependency", "Dependent"), order)
    }

    @Test
    fun `dependsOnBean orders without the bean being injected`() {
        val order = BeanGraph.order(
            indexOf(
                definition("Late", dependsOn = listOf("Early")),
                definition("Early"),
            )
        ).ids()

        assertEquals(listOf("Early", "Late"), order)
    }

    @Test
    fun `a type the parent provides creates no edge here`() {
        val order = BeanGraph.order(
            indexOf(definition("Service", needs = listOf(needs("PlatformService")))),
            parentProvides = { it == "PlatformService" },
        ).ids()

        assertEquals(listOf("Service"), order)
    }

    @Test
    fun `a local provider wins over the parent for ordering`() {
        val order = BeanGraph.order(
            indexOf(
                definition("Service", needs = listOf(needs("PlatformService"))),
                definition("LocalPlatformService", provides = listOf("LocalPlatformService", "PlatformService")),
            ),
            parentProvides = { it == "PlatformService" },
        ).ids()

        assertEquals(listOf("LocalPlatformService", "Service"), order)
    }

    @Test
    fun `a self reference is ignored rather than reported as a cycle`() {
        val order = BeanGraph.order(
            indexOf(definition("SelfAware", provides = listOf("SelfAware", "Marker"), needs = listOf(needs("Marker"))))
        ).ids()

        assertEquals(listOf("SelfAware"), order)
    }
}

package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.DuplicateRpcService
import dev.slne.surf.nats.EmptyService
import dev.slne.surf.nats.InvalidRpcNameService
import dev.slne.surf.nats.MismatchedValueClassService
import dev.slne.surf.nats.MissingMethodService
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.OverloadedService
import dev.slne.surf.nats.UndescribedMethodService
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.fixtures.Tag
import dev.slne.surf.nats.test.proto.v1.CalcService
import dev.slne.surf.nats.test.proto.v1.FinderService
import dev.slne.surf.nats.test.proto.v1.OrderService
import dev.slne.surf.nats.test.proto.v1.RolesService
import dev.slne.surf.nats.test.proto.v1.Text
import org.junit.jupiter.api.AfterEach
import java.lang.reflect.Modifier
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import dev.slne.surf.nats.test.proto.v1.VersionedService as VersionedServiceV1
import dev.slne.surf.nats.test.proto.v2.VersionedService as VersionedServiceV2

class ServiceDescriptorTest {

    abstract class CalcImpl : CalcService

    interface NotGenerated {
        suspend fun ping(): Text
    }

    private val clients = mutableListOf<NatsClientImpl>()

    @AfterEach
    fun closeClients() = clients.forEach { it.close() }

    private fun client(name: String = "svc-${UUID.randomUUID().toString().take(8)}") =
        (NatsClient.create(name) as NatsClientImpl).also { clients += it }

    private fun describe(type: Class<*>) = ServiceDescriptor.of(type)

    private fun ServiceDescriptor.method(name: String) = methods.single { it.kotlinName == name }

    @Test
    fun `method subjects carry the kind, the service full name and the rpc name`() {
        val descriptor = describe(CalcService::class.java)

        assertEquals("surf.nats.test.v1.CalcService", descriptor.name)
        assertEquals("surf.rpc.surf.nats.test.v1.CalcService.Mixed", descriptor.method("mixed").subject)
        assertEquals("surf.rpc.surf.nats.test.v1.CalcService.InnerTimeout", descriptor.method("innerTimeout").subject)
        assertEquals("surf.rpc.surf.nats.test.v1.CalcService.SlowFire", descriptor.method("slowFire").subject)
        assertEquals("surf.nats.test.v1.CalcService", descriptor.names.queueGroup)
    }

    @Test
    fun `methods follow the descriptor order and are indexed consecutively`() {
        val methods = describe(CalcService::class.java).methods
        val generated = CalcService.NATS_DESCRIPTOR.methods

        assertEquals(generated.map { it.kotlinName }, methods.map { it.kotlinName })
        assertEquals(methods.indices.toList(), methods.map { it.index })
        methods.forEach { assertSame(generated[it.index], it.generated) }
    }

    @Test
    fun `value class parameters resolve the mangled JVM method and their boxing`() {
        val inline = describe(CalcService::class.java).method("inline")

        assertTrue(inline.method.name.startsWith("inline-"), inline.method.name)
        assertEquals(
            listOf(Long::class.javaPrimitiveType, Duration::class.java, String::class.java, String::class.java, Continuation::class.java),
            inline.method.parameterTypes.toList(),
        )

        val (duration, maybeDuration, tag, maybeTag) = inline.valueClasses
        assertEquals(Duration::class.java, duration?.type)
        assertEquals(Long::class.javaPrimitiveType, duration?.underlying)
        assertNull(maybeDuration, "Duration? is passed boxed")
        assertEquals(Tag::class.java, tag?.type)
        assertEquals(String::class.java, tag?.underlying)
        assertEquals(Tag::class.java, maybeTag?.type, "Tag? is passed as its underlying String")
        assertEquals(String::class.java, maybeTag?.underlying)
    }

    @Test
    fun `parameters of other types need no boxing`() {
        val descriptor = describe(CalcService::class.java)

        assertTrue(descriptor.method("mixed").valueClasses.all { it == null })
        assertTrue(descriptor.method("unsigned").valueClasses.all { it == null })
        assertEquals("mixed", descriptor.method("mixed").method.name)
    }

    @Test
    fun `default parameter bridges are ignored`() {
        val bridges = CalcService::class.java.declaredMethods.filter { Modifier.isStatic(it.modifiers) && it.isSynthetic }
        assertTrue(bridges.any { it.name == "unsigned\$default" }, bridges.toString())
        assertNotNull(Class.forName(CalcService::class.java.name + "\$DefaultImpls"))

        val unsigned = describe(CalcService::class.java).method("unsigned")
        assertEquals("unsigned", unsigned.method.name)
        assertFalse(Modifier.isStatic(unsigned.method.modifiers))

        val ping = describe(VersionedServiceV2::class.java).methods.single()
        assertEquals("ping", ping.method.name)
        assertEquals(3, ping.method.parameterCount)
    }

    @Test
    fun `recipients and result kinds come from the descriptor`() {
        val finder = describe(FinderService::class.java)
        assertEquals(Recipients.ALL_INSTANCES, finder.method("find").recipients)
        assertEquals(NatsResultKind.ANSWER, finder.method("find").result)
        assertEquals(Recipients.ALL_INSTANCES, finder.method("broadcast").recipients)
        assertEquals(NatsResultKind.UNIT, finder.method("broadcast").result)
        assertNull(finder.method("broadcast").response)
        assertEquals(Recipients.ANY_INSTANCE, finder.method("lookup").recipients)
        assertEquals(NatsResultKind.VALUE, finder.method("lookup").result)

        val calc = describe(CalcService::class.java)
        assertEquals(Recipients.ANY_INSTANCE, calc.method("mixed").recipients)
        assertEquals(NatsResultKind.VALUE, calc.method("mixed").result)
        assertEquals(NatsResultKind.UNIT, calc.method("fire").result)
    }

    @Test
    fun `call timeout comes from the descriptor or the runtime default`() {
        val calc = describe(CalcService::class.java)
        assertEquals(15.seconds, calc.method("slow").timeout)
        assertEquals(ProtocolDefaults.CORE_TIMEOUT_SECONDS.seconds, calc.method("mixed").timeout)

        val orders = describe(OrderService::class.java)
        assertEquals(ProtocolDefaults.PERSISTENT_TIMEOUT_SECONDS.seconds, orders.method("total").timeout)
        assertEquals(180.seconds, orders.method("audit").timeout)
        assertEquals(ProtocolDefaults.CORE_TIMEOUT_SECONDS.seconds, orders.method("ping").timeout)
    }

    @Test
    fun `persistent methods use the persist namespace and the service streams`() {
        val descriptor = describe(OrderService::class.java)

        assertTrue(descriptor.method("place").persistent)
        assertEquals("surf.persist.surf.nats.test.v1.OrderService.Place", descriptor.method("place").subject)
        assertFalse(descriptor.method("ping").persistent)
        assertEquals("surf.rpc.surf.nats.test.v1.OrderService.Ping", descriptor.method("ping").subject)

        val streams = assertNotNull(descriptor.streams)
        assertEquals("SURF_PERSIST_surf_nats_test_v1_OrderService", streams.stream)
        assertEquals("surf.persist.surf.nats.test.v1.OrderService.*", streams.streamFilter)
        assertEquals("SURF_DLQ_surf_nats_test_v1_OrderService", streams.dlqStream)
        assertEquals("surf.dlq.surf.nats.test.v1.OrderService", streams.dlqSubject)
        assertEquals("responders", streams.durable)
    }

    @Test
    fun `service without persistent methods has no streams`() {
        assertNull(describe(CalcService::class.java).streams)
    }

    @Test
    fun `differently named clients share one descriptor named after the contract`() {
        val first = client("transaction").serviceDescriptor(RolesService::class.java)
        val second = client("lobby").serviceDescriptor(RolesService::class.java)

        assertSame(first, second)
        assertEquals(
            listOf(
                "surf.persist.surf.nats.test.v1.RolesService.Whitelist",
                "surf.rpc.surf.nats.test.v1.RolesService.Echo",
                "surf.rpc.surf.nats.test.v1.RolesService.Owner",
                "surf.persist.surf.nats.test.v1.RolesService.Store",
                "surf.rpc.surf.nats.test.v1.RolesService.Handle",
            ),
            first.methods.map { it.subject },
        )
        assertEquals("surf.nats.test.v1.RolesService", first.names.queueGroup)
        val streams = assertNotNull(first.streams)
        assertEquals("SURF_PERSIST_surf_nats_test_v1_RolesService", streams.stream)
        assertEquals("responders", streams.durable)
        assertNull(first.presenceSubject)
    }

    @Test
    fun `major versions of one service use separate subjects and streams`() {
        val v1 = describe(VersionedServiceV1::class.java)
        val v2 = describe(VersionedServiceV2::class.java)

        assertEquals("surf.nats.test.v1.VersionedService", v1.name)
        assertEquals("surf.nats.test.v2.VersionedService", v2.name)
        assertEquals("surf.rpc.surf.nats.test.v1.VersionedService.Ping", v1.methods.single().subject)
        assertEquals("surf.rpc.surf.nats.test.v2.VersionedService.Ping", v2.methods.single().subject)
    }

    @Test
    fun `implementation class is rejected with the generated interface it implements`() {
        val client = client()
        val message = assertFailsWith<IllegalArgumentException> { client.service(CalcImpl::class) }.message.orEmpty()

        assertContains(message, "${CalcImpl::class.java.name} is not a generated surf-nats service interface")
        assertContains(message, "registerService<CalcService>(implementation)")
        assertContains(message, CalcService::class.java.name)
    }

    @Test
    fun `interface without a generated descriptor is rejected`() {
        val message = assertFailsWith<IllegalArgumentException> { describe(NotGenerated::class.java) }.message.orEmpty()

        assertContains(message, "is not a generated surf-nats service interface")
    }

    @Test
    fun `descriptor rpc without an interface method is rejected`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(MissingMethodService::class.java)
        }.message.orEmpty()

        assertContains(message, "Invalid NATS_DESCRIPTOR of ${MissingMethodService::class.java.name}")
        assertContains(message, "pong (rpc Pong): no suspend method with 0 parameters")
    }

    @Test
    fun `interface method without a descriptor rpc is rejected`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(UndescribedMethodService::class.java)
        }.message.orEmpty()

        assertContains(message, "extra is not described by any rpc of the descriptor")
    }

    @Test
    fun `two rpcs resolving to the same interface method are rejected`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(DuplicateRpcService::class.java)
        }.message.orEmpty()

        assertContains(message, "Invalid NATS_DESCRIPTOR of ${DuplicateRpcService::class.java.name}")
        assertContains(message, "ping (rpc Ping): ping is described by more than one rpc")
    }

    @Test
    fun `an rpc matching several suspend methods is rejected`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(OverloadedService::class.java)
        }.message.orEmpty()

        assertContains(message, "Invalid NATS_DESCRIPTOR of ${OverloadedService::class.java.name}")
        assertContains(message, "find (rpc Find): matches more than one method: ")
        assertContains(message, "find-")
    }

    @Test
    fun `an rpc name that is no proto identifier is rejected`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(InvalidRpcNameService::class.java)
        }.message.orEmpty()

        assertContains(message, "Invalid NATS_DESCRIPTOR of ${InvalidRpcNameService::class.java.name}")
        assertContains(message, "ping (rpc Pi ng): rpc surf.nats.test.v1.InvalidRpcNameService.Pi ng:")
    }

    @Test
    fun `a service without methods is rejected`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(EmptyService::class.java)
        }.message.orEmpty()

        assertEquals("${EmptyService::class.java.name} has no service methods", message)
    }

    @Test
    fun `value class mismatch names the parameter and its declared type`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(MismatchedValueClassService::class.java)
        }.message.orEmpty()

        assertContains(
            message,
            "pause (rpc Pause): parameter 'duration' (${Tag::class.java.name}) has no box-impl/unbox-impl for long",
        )
        assertFalse("pause-" in message, "mangled JVM method name in: $message")
    }
}

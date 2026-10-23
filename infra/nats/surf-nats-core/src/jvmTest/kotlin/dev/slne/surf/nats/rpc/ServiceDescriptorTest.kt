package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.DuplicateRpcService
import dev.slne.surf.nats.EmptyService
import dev.slne.surf.nats.InvalidSubjectService
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

    private fun describe(type: Class<*>, clientName: String? = null) =
        (if (clientName != null) client(clientName) else client()).serviceDescriptor(type)

    private fun ServiceDescriptor.method(name: String) = methods.single { it.kotlinName == name }

    @Test
    fun `prefixed method subjects carry the client, kind and major version`() {
        val descriptor = describe(CalcService::class.java, "transaction")

        assertEquals("calc", descriptor.name)
        assertEquals(1, descriptor.majorVersion)
        assertEquals("transaction.\$rpc.v1.calc.mixed", descriptor.method("mixed").subject)
        assertEquals("transaction.\$rpc.v1.calc.inner-timeout", descriptor.method("innerTimeout").subject)
        assertEquals("transaction.\$rpc.v1.calc.slow-fire", descriptor.method("slowFire").subject)
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
    fun `persistent methods use the persist namespace and versioned streams`() {
        val descriptor = describe(OrderService::class.java, "transaction")

        assertTrue(descriptor.method("place").persistent)
        assertEquals("transaction.\$persist.v1.orders.place", descriptor.method("place").subject)
        assertFalse(descriptor.method("ping").persistent)
        assertEquals("transaction.\$rpc.v1.orders.ping", descriptor.method("ping").subject)
        assertEquals("transaction", descriptor.responderGroup)

        val streams = assertNotNull(descriptor.streams)
        assertEquals("SURF_TRANSACTION_ORDERS_V1", streams.stream)
        assertEquals("transaction.\$persist.v1.orders.*", streams.streamFilter)
        assertEquals("SURF_TRANSACTION_ORDERS_V1_DLQ", streams.dlqStream)
        assertEquals("transaction.\$dlq.v1.orders", streams.dlqSubject)
    }

    @Test
    fun `service without persistent methods has no streams`() {
        assertNull(describe(CalcService::class.java).streams)
    }

    @Test
    fun `global service owns the service namespace`() {
        val descriptor = describe(RolesService::class.java, "transaction")

        assertEquals("ext-roles.\$rpc.v1.echo", descriptor.method("echo").subject)
        assertEquals("ext-roles.\$persist.v1.whitelist", descriptor.method("whitelist").subject)
        assertEquals("ext-roles.\$persist.v1.store", descriptor.method("store").subject)
        assertNull(descriptor.presenceSubject)
        assertEquals("ext-roles", descriptor.responderGroup)

        val streams = assertNotNull(descriptor.streams)
        assertEquals("SURF__EXT_ROLES_V1", streams.stream)
        assertEquals("ext-roles.\$persist.v1.*", streams.streamFilter)
        assertEquals("SURF__EXT_ROLES_V1_DLQ", streams.dlqStream)
        assertEquals("ext-roles.\$dlq.v1", streams.dlqSubject)
    }

    @Test
    fun `global service has the same names in every client`() {
        val first = describe(RolesService::class.java)
        val second = describe(RolesService::class.java)

        assertEquals(first.methods.map { it.subject }, second.methods.map { it.subject })
        assertEquals(first.streams?.stream, second.streams?.stream)
        assertEquals(first.responderGroup, second.responderGroup)
        assertEquals(first.registrationKey, second.registrationKey)
    }

    @Test
    fun `registration keys carry the scope and the major version`() {
        val client = client("transaction")

        assertEquals("client:calc:v1", client.serviceDescriptor(CalcService::class.java).registrationKey)
        assertEquals("global:ext-roles:v1", client.serviceDescriptor(RolesService::class.java).registrationKey)
        assertEquals("client:versioned:v1", client.serviceDescriptor(VersionedServiceV1::class.java).registrationKey)
        assertEquals("client:versioned:v2", client.serviceDescriptor(VersionedServiceV2::class.java).registrationKey)
    }

    @Test
    fun `major versions of one service use separate subjects`() {
        val client = client("transaction")
        val v1 = client.serviceDescriptor(VersionedServiceV1::class.java)
        val v2 = client.serviceDescriptor(VersionedServiceV2::class.java)

        assertEquals(v1.name, v2.name)
        assertEquals("transaction.\$rpc.v1.versioned.ping", v1.methods.single().subject)
        assertEquals("transaction.\$rpc.v2.versioned.ping", v2.methods.single().subject)
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
    fun `an invalid composed method subject is rejected`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(InvalidSubjectService::class.java, "transaction")
        }.message.orEmpty()

        assertContains(message, "Invalid NATS_DESCRIPTOR of ${InvalidSubjectService::class.java.name}")
        assertContains(
            message,
            "Invalid NATS subject 'transaction.\$rpc.v1.invalid-subject.pi ng' for ${InvalidSubjectService::class.java.name}.ping",
        )
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

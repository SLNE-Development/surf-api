package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.Answer
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsService
import dev.slne.surf.nats.NatsTimeout
import dev.slne.surf.nats.ResponderMode
import dev.slne.surf.nats.Responders
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.client.NatsClientImpl
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import java.util.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class ServiceDescriptorTest {

    @Serializable
    data class PlayerHealth(val health: Double)

    class NotSerializable(val value: Int)

    @Suppress("unused")
    @NatsService("player")
    interface PlayerService {
        suspend fun health(player: String): PlayerHealth?

        @Responders(ResponderMode.ALL)
        suspend fun findServer(player: String): Answer<String>

        @Responders(ResponderMode.ALL)
        suspend fun broadcast(message: String)

        @NatsTimeout(15)
        suspend fun heavyReport(): String

        suspend fun shout(
            text: @Serializable(with = UpperCaseSerializer::class) String,
        ): @Serializable(with = UpperCaseSerializer::class) String

        suspend fun customAnswer(): Answer<@Serializable(with = UpperCaseSerializer::class) String>

        fun localHelper(): Int = 42
    }

    @Suppress("unused")
    @NatsService
    interface DefaultNamedService {
        suspend fun ping(): String
    }

    @Suppress("unused")
    @NatsService(responders = ResponderMode.ALL)
    interface AllByDefaultService {
        suspend fun lookup(id: Long): Answer<String>

        @Responders(ResponderMode.ONE)
        suspend fun single(id: Long): String
    }

    @Suppress("unused")
    @NatsService("broken")
    interface BrokenService {
        fun notSuspend(): String
        suspend fun overloaded(value: Int): String
        suspend fun overloaded(value: String): String

        @Responders(ResponderMode.ALL)
        suspend fun allWithValue(): String
        suspend fun badParameter(value: NotSerializable)
        suspend fun starAnswer(): Answer<*>
    }

    @Suppress("unused")
    @NatsService("bad-timeout")
    @NatsTimeout(0)
    interface InvalidTimeoutService {
        @NatsTimeout(-5)
        suspend fun negative(): String

        suspend fun inherited(): String
    }

    @Suppress("unused")
    interface NotAnnotated {
        suspend fun ping(): String
    }

    @NatsService
    interface EmptyService

    @NatsService
    abstract class NotAnInterface

    private val clients = mutableListOf<NatsClientImpl>()

    @AfterEach
    fun closeClients() = clients.forEach { it.close() }

    private fun client(name: String = "svc-${UUID.randomUUID().toString().take(8)}") =
        (NatsClient.create(name) as NatsClientImpl).also { clients += it }

    private fun describe(type: Class<*>, clientName: String? = null) =
        (if (clientName != null) client(clientName) else client()).serviceDescriptor(type)

    private fun ServiceDescriptor.method(name: String) = methods.single { it.method.name == name }

    @Test
    fun `explicit service name and kebab-case method subjects`() {
        val descriptor = describe(PlayerService::class.java, "transaction")

        assertEquals("player", descriptor.name)
        assertEquals("transaction.player.health", descriptor.method("health").subject)
        assertEquals("transaction.player.find-server", descriptor.method("findServer").subject)
        assertEquals("transaction.player.heavy-report", descriptor.method("heavyReport").subject)
    }

    @Test
    fun `default service name is the kebab-case interface name`() {
        val descriptor = describe(DefaultNamedService::class.java, "transaction")

        assertEquals("default-named-service", descriptor.name)
        assertEquals("transaction.default-named-service.ping", descriptor.method("ping").subject)
    }

    @Test
    fun `methods are sorted by name and indexed consecutively`() {
        val methods = describe(PlayerService::class.java).methods

        assertEquals(methods.map { it.method.name }.sorted(), methods.map { it.method.name })
        assertEquals(methods.indices.toList(), methods.map { it.index })
    }

    @Test
    fun `default interface methods are not service methods`() {
        assertTrue(describe(PlayerService::class.java).methods.none { it.method.name == "localHelper" })
    }

    @Test
    fun `responder mode comes from the method or the interface`() {
        val player = describe(PlayerService::class.java)
        assertEquals(ResponderMode.ONE, player.method("health").responders)
        assertEquals(ResponderMode.ALL, player.method("findServer").responders)

        val allByDefault = describe(AllByDefaultService::class.java)
        assertEquals(ResponderMode.ALL, allByDefault.method("lookup").responders)
        assertEquals(ResponderMode.ONE, allByDefault.method("single").responders)
    }

    @Test
    fun `timeout comes from NatsTimeout`() {
        val descriptor = describe(PlayerService::class.java)
        assertEquals(15.seconds, descriptor.method("heavyReport").timeout)
        assertEquals(RpcDefaults.CORE_TIMEOUT, descriptor.method("health").timeout)
    }

    @Test
    fun `result kinds`() {
        val descriptor = describe(PlayerService::class.java)

        assertEquals(ResultKind.Unit, descriptor.method("broadcast").result)
        assertIs<ResultKind.Value>(descriptor.method("health").result)
        assertIs<ResultKind.Answer>(descriptor.method("findServer").result)
    }

    @Test
    fun `nullable value result accepts null`() {
        val client = client()
        val result = assertIs<ResultKind.Value>(client.serviceDescriptor(PlayerService::class.java).method("health").result)

        val bytes = client.codec.encode(result.serializer, null, WireFormat.CBOR)
        assertNull(client.codec.decode(result.serializer, bytes, WireFormat.CBOR.contentType))
    }

    @Test
    fun `custom serializers on parameters, results and answer values are used`() {
        val client = client()
        val descriptor = client.serviceDescriptor(PlayerService::class.java)
        val codec = client.codec
        val shout = descriptor.method("shout")

        val arguments = codec.encode(shout.arguments, arrayOf("hello"), WireFormat.JSON).decodeToString()
        assertEquals("""{"text":"HELLO"}""", arguments)

        val result = assertIs<ResultKind.Value>(shout.result)
        assertEquals("\"HELLO\"", codec.encode(result.serializer, "hello", WireFormat.JSON).decodeToString())

        val answer = assertIs<ResultKind.Answer>(descriptor.method("customAnswer").result)
        assertEquals("\"HELLO\"", codec.encode(answer.serializer, "hello", WireFormat.JSON).decodeToString())
    }

    @Test
    fun `all problems of an invalid service are reported together`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(BrokenService::class.java)
        }.message.orEmpty()

        assertContains(message, "notSuspend")
        assertContains(message, "overloaded")
        assertContains(message, "allWithValue")
        assertContains(message, "badParameter")
        assertContains(message, "starAnswer")
    }

    @Test
    fun `non-positive NatsTimeout on interface and method is rejected`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(InvalidTimeoutService::class.java)
        }.message.orEmpty()

        assertContains(message, "@NatsTimeout on InvalidTimeoutService must be positive, was 0")
        assertContains(message, "negative: @NatsTimeout must be positive, was -5")
    }

    @Test
    fun `interface without NatsService is rejected`() {
        assertFailsWith<IllegalArgumentException> { describe(NotAnnotated::class.java) }
    }

    @Test
    fun `service without methods is rejected`() {
        assertFailsWith<IllegalArgumentException> { describe(EmptyService::class.java) }
    }

    @Test
    fun `class instead of interface is rejected`() {
        assertFailsWith<IllegalArgumentException> { describe(NotAnInterface::class.java) }
    }
}

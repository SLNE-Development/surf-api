package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.NatsClient
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

        @NatsResponders(ResponderMode.ALL)
        suspend fun findServer(player: String): Answer<String>

        @NatsResponders(ResponderMode.ALL)
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

        @NatsResponders(ResponderMode.ONE)
        suspend fun single(id: Long): String
    }

    @Suppress("unused")
    @NatsService("broken")
    interface BrokenService {
        fun notSuspend(): String
        suspend fun overloaded(value: Int): String
        suspend fun overloaded(value: String): String

        @NatsResponders(ResponderMode.ALL)
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

    @Serializable
    data class WhitelistArguments(val userId: Long, val action: String)

    @Suppress("unused")
    @NatsService("roles")
    interface WireNamedService {
        @NatsMethod("whitelist-role")
        suspend fun updateWhitelistRole(@NatsParam("userId") discordUserId: Long, action: String)

        @NatsPersistent
        @NatsMethod("store")
        suspend fun persistEntry(entry: String)
    }

    @Suppress("unused")
    @NatsService("invalid-wire")
    interface InvalidWireNamesService {
        @NatsMethod("a.b")
        suspend fun dotted(): String

        suspend fun blankParam(@NatsParam(" ") value: String): String

        suspend fun sameParam(@NatsParam("id") first: Long, @NatsParam("id") second: Long): String

        @NatsMethod("same")
        suspend fun first(): String

        @NatsMethod("same")
        suspend fun second(): String

        @NatsMethod("\$internal")
        suspend fun dollar(): String
    }

    @Suppress("unused")
    @NatsService("roles-api", prefixed = false)
    interface UnprefixedService {
        suspend fun lookup(id: Long): String

        @NatsResponders(ResponderMode.ALL)
        suspend fun find(id: Long): Answer<String>

        @NatsPersistent
        suspend fun store(entry: String)
    }

    @Suppress("unused")
    @NatsService("bad.name")
    interface DottedNameService {
        suspend fun ping(): String
    }

    @Suppress("unused")
    @NatsService("Bad")
    interface UpperCaseNameService {
        suspend fun ping(): String
    }

    @Suppress("unused")
    @NatsService("bad_name")
    interface UnderscoreNameService {
        suspend fun ping(): String
    }

    @Suppress("unused")
    @NatsService("players")
    interface UuidService {
        suspend fun owner(id: UUID): UUID

        suspend fun find(id: UUID): Answer<UUID>
    }

    @Suppress("unused")
    @NatsService("roles-dlq")
    interface DlqSuffixService {
        suspend fun ping(): String
    }

    @Suppress("unused")
    @NatsService("dlq")
    interface DlqNameService {
        suspend fun ping(): String
    }

    @NatsService
    interface EmptyService

    @NatsService
    abstract class NotAnInterface

    abstract class BasePing : DefaultNamedService

    abstract class PingImpl : BasePing()

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

    @Test
    fun `implementation class is rejected with the service interface it implements`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(PingImpl::class.java)
        }.message.orEmpty()

        assertContains(message, "registerService<DefaultNamedService>(implementation)")
        assertContains(message, DefaultNamedService::class.java.name)
    }

    @Test
    fun `NatsMethod defines the method subject token`() {
        val descriptor = describe(WireNamedService::class.java, "transaction")

        assertEquals("transaction.roles.whitelist-role", descriptor.method("updateWhitelistRole").subject)
        assertEquals($$"transaction.$persist.roles.store", descriptor.method("persistEntry").subject)
    }

    @Test
    fun `NatsParam defines the argument name on the wire`() {
        val client = client()
        val arguments = client.serviceDescriptor(WireNamedService::class.java).method("updateWhitelistRole").arguments
        val codec = client.codec

        val json = codec.encode(arguments, arrayOf(42L, "ADD"), WireFormat.JSON).decodeToString()
        assertEquals("""{"user_id":42,"action":"ADD"}""", json)

        val external = """{"user_id":7,"action":"REMOVE"}""".encodeToByteArray()
        assertContentEquals(arrayOf<Any?>(7L, "REMOVE"), codec.decode(arguments, external, WireFormat.JSON.contentType))

        val cbor = codec.encode(arguments, arrayOf(42L, "ADD"), WireFormat.CBOR)
        assertEquals(
            WhitelistArguments(42, "ADD"),
            codec.decode(WhitelistArguments.serializer(), cbor, WireFormat.CBOR.contentType),
        )
    }

    @Test
    fun `invalid wire names are reported together`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(InvalidWireNamesService::class.java)
        }.message.orEmpty()

        assertContains(message, "dotted: @NatsMethod name must be a single subject token, was 'a.b'")
        assertContains(message, "blankParam: @NatsParam on parameter 'value' must not be blank")
        assertContains(message, "sameParam: argument name 'id' is used by more than one parameter")
        assertContains(message, "invalid-wire.same' is used by first, second")
        assertContains(message, "dollar: @NatsMethod name must not start with '\$', was '\$internal'")
    }

    @Test
    fun `unprefixed service owns the service namespace`() {
        val descriptor = describe(UnprefixedService::class.java, "transaction")

        assertEquals("roles-api.lookup", descriptor.method("lookup").subject)
        assertEquals("roles-api.find", descriptor.method("find").subject)
        assertEquals("roles-api.\$persist.store", descriptor.method("store").subject)
        assertEquals("roles-api.\$presence", descriptor.presenceSubject)
        assertEquals("roles-api", descriptor.responderGroup)

        val streams = assertNotNull(descriptor.streams)
        assertEquals("SURF__ROLES_API", streams.stream)
        assertEquals("roles-api.\$persist.>", streams.filter)
        assertEquals("SURF__ROLES_API_DLQ", streams.dlqStream)
        assertEquals("roles-api.\$dlq", streams.dlqSubject)
    }

    @Test
    fun `unprefixed service has the same names in every client`() {
        val first = describe(UnprefixedService::class.java)
        val second = describe(UnprefixedService::class.java)

        assertEquals(first.methods.map { it.subject }, second.methods.map { it.subject })
        assertEquals(first.streams?.stream, second.streams?.stream)
        assertEquals(first.responderGroup, second.responderGroup)
    }

    @Test
    fun `prefixed service uses the client name for responders and streams`() {
        val descriptor = describe(WireNamedService::class.java, "transaction")

        assertEquals("transaction", descriptor.responderGroup)
        val streams = assertNotNull(descriptor.streams)
        assertEquals("SURF_TRANSACTION_ROLES", streams.stream)
        assertEquals("transaction.\$persist.roles.>", streams.filter)
        assertEquals("transaction.\$dlq.roles", streams.dlqSubject)
    }

    @Test
    fun `service names must be kebab-case`() {
        val types = listOf(
            DottedNameService::class.java,
            UpperCaseNameService::class.java,
            UnderscoreNameService::class.java,
        )
        for (type in types) {
            val message = assertFailsWith<IllegalArgumentException> { describe(type) }.message.orEmpty()
            assertContains(message, "must be kebab-case", message = type.simpleName)
        }
    }

    @Test
    fun `serializers follow the wire format`() {
        val client = client()
        val descriptor = client.serviceDescriptor(UuidService::class.java)
        val codec = client.codec
        val owner = descriptor.method("owner")
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

        assertEquals("""{"id":"$uuid"}""", codec.encode(owner.arguments, arrayOf(uuid), WireFormat.JSON).decodeToString())
        val external = """{"id":"$uuid"}""".encodeToByteArray()
        assertContentEquals(arrayOf<Any?>(uuid), codec.decode(owner.arguments, external, WireFormat.JSON.contentType))

        val value = assertIs<ResultKind.Value>(owner.result)
        assertEquals("\"$uuid\"", codec.encode(value.serializer, uuid, WireFormat.JSON).decodeToString())
        val answer = assertIs<ResultKind.Answer>(descriptor.method("find").result)
        assertEquals("\"$uuid\"", codec.encode(answer.serializer, uuid, WireFormat.JSON).decodeToString())

        val cbor = codec.encode(owner.arguments, arrayOf(uuid), WireFormat.CBOR)
        val uuidString = uuid.toString().encodeToByteArray().asList()
        assertTrue(cbor.asList().windowed(uuidString.size).none { it == uuidString }, "CBOR must encode the UUID binary")
        assertContentEquals(arrayOf<Any?>(uuid), codec.decode(owner.arguments, cbor, WireFormat.CBOR.contentType))
    }

    @Test
    fun `service names ending in dlq are reserved`() {
        for (type in listOf(DlqSuffixService::class.java, DlqNameService::class.java)) {
            val message = assertFailsWith<IllegalArgumentException> { describe(type) }.message.orEmpty()
            assertContains(message, "is reserved", message = type.simpleName)
        }
    }
}

package dev.slne.surf.nats.js

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.contract.Contracts
import dev.slne.surf.nats.js.contract.ProtocolOptionReader
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.DescService
import dev.slne.surf.nats.js.fixtures.AuditService
import dev.slne.surf.nats.js.fixtures.ChatMessageSchema
import dev.slne.surf.nats.js.fixtures.PlainMessageSchema
import dev.slne.surf.nats.js.fixtures.PlayerJoinedSchema
import dev.slne.surf.nats.js.fixtures.PlayerService
import dev.slne.surf.nats.js.fixtures.RegistryService
import dev.slne.surf.nats.js.fixtures.RulesSchema
import dev.slne.surf.nats.js.fixtures.ServerStartedSchema
import dev.slne.surf.nats.js.fixtures.StreamingService
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.rpc.Recipients
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import dev.slne.surf.nats.js.fixtures.v2.PlayerJoinedSchema as PlayerJoinedV2Schema
import dev.slne.surf.nats.js.fixtures.v2.PlayerService as PlayerServiceV2

class ContractsTest {

    private fun method(service: DescService, localName: String): DescMethod = service.methods.first { it.localName == localName }

    @Test
    fun eventDefaultSubjectIsKebabCaseOfTheMessageName() {
        val contract = Contracts.event(PlayerJoinedSchema)
        assertEquals("surf.natsjs.test.v1.PlayerJoined", contract.typeName)
        assertEquals("player-joined", contract.subject)
        assertFalse(contract.global)
        assertEquals(1, contract.majorVersion)
        assertEquals("surf-punish.v1.player-joined", contract.subject("surf-punish"))
    }

    @Test
    fun eventSubjectOptionAndVersionToken() {
        assertEquals("surf-punish.v1.chat.message", Contracts.event(ChatMessageSchema).subject("surf-punish"))
        assertEquals("surf-punish.v2.player-joined", Contracts.event(PlayerJoinedV2Schema).subject("surf-punish"))
    }

    @Test
    fun globalEventIgnoresTheOwner() {
        val contract = Contracts.event(ServerStartedSchema)
        assertTrue(contract.global)
        assertEquals("server.v1.started", contract.subject(null))
        assertEquals("server.v1.started", contract.subject("surf-punish"))
    }

    @Test
    fun nonGlobalEventRequiresAValidOwner() {
        val missing = assertFailsWith<IllegalArgumentException> { Contracts.event(PlayerJoinedSchema).subject(null) }
        assertTrue(missing.message!!.contains("is not global"), missing.message)
        assertFailsWith<IllegalArgumentException> { Contracts.event(PlayerJoinedSchema).subject("Surf_Punish") }
    }

    @Test
    fun messageWithoutEventOptionIsRejected() {
        val error = assertFailsWith<IllegalArgumentException> { Contracts.event(PlainMessageSchema) }
        assertTrue(error.message!!.contains("is not a surf-nats event"), error.message)
    }

    @Test
    fun contractsAreCachedPerDescriptor() {
        assertSame(Contracts.event(PlayerJoinedSchema), Contracts.event(PlayerJoinedSchema))
        val find = method(PlayerService, "findPlayer")
        assertSame(Contracts.method(find), Contracts.method(find))
    }

    @Test
    fun coreMethodDefaults() {
        val contract = Contracts.method(method(PlayerService, "findPlayer"))
        assertEquals("players", contract.service.name)
        assertEquals("find-player", contract.name)
        assertFalse(contract.persistent)
        assertEquals(Recipients.ANY_INSTANCE, contract.recipients)
        assertEquals(NatsResultKind.VALUE, contract.resultKind)
        assertEquals(ProtocolDefaults.CORE_TIMEOUT_SECONDS.seconds, contract.defaultTimeout)

        val names = contract.service.names("surf-punish")
        assertEquals("surf-punish.\$rpc.v1.players.find-player", names.methodSubject(contract.name, contract.persistent))
    }

    @Test
    fun resultKindsAndMethodOptions() {
        assertEquals(NatsResultKind.UNIT, Contracts.method(method(PlayerService, "kick")).resultKind)

        val locate = Contracts.method(method(PlayerService, "locate"))
        assertEquals(Recipients.ALL_INSTANCES, locate.recipients)
        assertEquals(NatsResultKind.ANSWER, locate.resultKind)

        val broadcast = Contracts.method(method(PlayerService, "broadcast"))
        assertEquals("broadcast-all", broadcast.name)
        assertEquals(Recipients.ALL_INSTANCES, broadcast.recipients)
        assertEquals(NatsResultKind.UNIT, broadcast.resultKind)

        assertEquals(1.seconds, Contracts.method(method(PlayerService, "slowFind")).defaultTimeout)
    }

    @Test
    fun persistentServiceInheritance() {
        val record = Contracts.method(method(AuditService, "record"))
        assertTrue(record.persistent)
        assertEquals(NatsResultKind.UNIT, record.resultKind)
        assertEquals(10.seconds, record.defaultTimeout)

        val names = record.service.names("surf-punish")
        assertEquals("surf-punish.\$persist.v1.audit-service.record", names.methodSubject(record.name, true))
        assertEquals("SURF_SURF_PUNISH_AUDIT_SERVICE_V1", names.stream)

        val query = Contracts.method(method(AuditService, "query"))
        assertTrue(query.persistent)
        assertEquals(NatsResultKind.VALUE, query.resultKind)

        // persistent = false on the method overrides the service, timeout 0 inherits the service's 10 s
        val ping = Contracts.method(method(AuditService, "ping"))
        assertFalse(ping.persistent)
        assertEquals(10.seconds, ping.defaultTimeout)
        assertEquals(NatsResultKind.UNIT, ping.resultKind)
    }

    @Test
    fun globalServiceAndRecipientsInheritance() {
        val lookup = Contracts.method(method(RegistryService, "lookup"))
        assertTrue(lookup.service.global)
        assertEquals(Recipients.ANY_INSTANCE, lookup.recipients)
        assertEquals(NatsResultKind.VALUE, lookup.resultKind)
        assertEquals("registry-service.\$rpc.v1.lookup", lookup.service.names(null).methodSubject(lookup.name, false))
        assertEquals("registry-service.\$rpc.v1.lookup", lookup.service.names("ignored").methodSubject(lookup.name, false))

        val announce = Contracts.method(method(RegistryService, "announce"))
        assertEquals(Recipients.ALL_INSTANCES, announce.recipients)
        assertEquals(NatsResultKind.UNIT, announce.resultKind)
    }

    @Test
    fun versionedServiceNames() {
        val find = Contracts.method(method(PlayerServiceV2, "findPlayer"))
        assertEquals(2, find.service.majorVersion)
        assertEquals("surf-punish.\$rpc.v2.players.find-player", find.service.names("surf-punish").methodSubject(find.name, false))
    }

    @Test
    fun streamingMethodsAreRejected() {
        val error = assertFailsWith<IllegalArgumentException> { Contracts.method(method(StreamingService, "watch")) }
        assertTrue(error.message!!.contains("streaming rpcs are not supported"), error.message)
    }

    @Test
    fun nullableOption() {
        val fields = RulesSchema.fields.associateBy { it.localName }
        assertTrue(ProtocolOptionReader.isNullable(fields.getValue("nullableText")))
        assertFalse(ProtocolOptionReader.isNullable(fields.getValue("requiredText")))
        assertFalse(ProtocolOptionReader.isNullable(fields.getValue("defaulted")))
    }
}

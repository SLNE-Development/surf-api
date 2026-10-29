package dev.slne.surf.nats.js

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.descriptor.ProtocolOptionReader
import dev.slne.surf.nats.js.event.EventDescriptor
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
import dev.slne.surf.nats.js.rpc.MethodDescriptor
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

class DescriptorsTest {

    private fun desc(service: DescService, localName: String): DescMethod = service.methods.first { it.localName == localName }

    private fun method(service: DescService, localName: String): MethodDescriptor = MethodDescriptor.of(desc(service, localName))

    @Test
    fun eventDefaultSubjectIsKebabCaseOfTheMessageName() {
        val descriptor = EventDescriptor.of(PlayerJoinedSchema)
        assertEquals("surf.natsjs.test.v1.PlayerJoined", descriptor.typeName)
        assertEquals("player-joined", descriptor.subject)
        assertFalse(descriptor.global)
        assertEquals(1, descriptor.majorVersion)
        assertEquals("surf-punish.v1.player-joined", descriptor.subject("surf-punish"))
    }

    @Test
    fun eventSubjectOptionAndVersionToken() {
        assertEquals("surf-punish.v1.chat.message", EventDescriptor.of(ChatMessageSchema).subject("surf-punish"))
        assertEquals("surf-punish.v2.player-joined", EventDescriptor.of(PlayerJoinedV2Schema).subject("surf-punish"))
    }

    @Test
    fun globalEventIgnoresTheOwner() {
        val descriptor = EventDescriptor.of(ServerStartedSchema)
        assertTrue(descriptor.global)
        assertEquals("server.v1.started", descriptor.subject(null))
        assertEquals("server.v1.started", descriptor.subject("surf-punish"))
    }

    @Test
    fun nonGlobalEventRequiresAValidOwner() {
        val missing = assertFailsWith<IllegalArgumentException> { EventDescriptor.of(PlayerJoinedSchema).subject(null) }
        assertTrue(missing.message!!.contains("is not global"), missing.message)
        assertFailsWith<IllegalArgumentException> { EventDescriptor.of(PlayerJoinedSchema).subject("Surf_Punish") }
    }

    @Test
    fun messageWithoutEventOptionIsRejected() {
        val error = assertFailsWith<IllegalArgumentException> { EventDescriptor.of(PlainMessageSchema) }
        assertTrue(error.message!!.contains("is not a surf-nats event"), error.message)
    }

    @Test
    fun descriptorsAreCachedPerSchema() {
        assertSame(EventDescriptor.of(PlayerJoinedSchema), EventDescriptor.of(PlayerJoinedSchema))
        val find = desc(PlayerService, "findPlayer")
        assertSame(MethodDescriptor.of(find), MethodDescriptor.of(find))
        assertSame(MethodDescriptor.of(find).service, method(PlayerService, "kick").service)
    }

    @Test
    fun coreMethodDefaults() {
        val find = method(PlayerService, "findPlayer")
        assertEquals("players", find.service.name)
        assertEquals("find-player", find.name)
        assertEquals("surf.natsjs.test.v1.PlayerService.FindPlayer", find.label)
        assertFalse(find.persistent)
        assertEquals(Recipients.ANY_INSTANCE, find.recipients)
        assertEquals(NatsResultKind.VALUE, find.result)
        assertEquals(ProtocolDefaults.CORE_TIMEOUT_SECONDS.seconds, find.timeout)
        assertEquals("surf-punish.\$rpc.v1.players.find-player", find.subject("surf-punish"))
    }

    @Test
    fun resultKindsAndMethodOptions() {
        assertEquals(NatsResultKind.UNIT, method(PlayerService, "kick").result)

        val locate = method(PlayerService, "locate")
        assertEquals(Recipients.ALL_INSTANCES, locate.recipients)
        assertEquals(NatsResultKind.ANSWER, locate.result)

        val broadcast = method(PlayerService, "broadcast")
        assertEquals("broadcast-all", broadcast.name)
        assertEquals(Recipients.ALL_INSTANCES, broadcast.recipients)
        assertEquals(NatsResultKind.UNIT, broadcast.result)

        assertEquals(1.seconds, method(PlayerService, "slowFind").timeout)
    }

    @Test
    fun persistentServiceInheritance() {
        val record = method(AuditService, "record")
        assertTrue(record.persistent)
        assertEquals(NatsResultKind.UNIT, record.result)
        assertEquals(10.seconds, record.timeout)
        assertEquals("surf-punish.\$persist.v1.audit-service.record", record.subject("surf-punish"))
        assertEquals("SURF_SURF_PUNISH_AUDIT_SERVICE_V1", record.stream("surf-punish"))

        val query = method(AuditService, "query")
        assertTrue(query.persistent)
        assertEquals(NatsResultKind.VALUE, query.result)

        // persistent = false on the method overrides the service, timeout 0 inherits the service's 10 s
        val ping = method(AuditService, "ping")
        assertFalse(ping.persistent)
        assertEquals(10.seconds, ping.timeout)
        assertEquals(NatsResultKind.UNIT, ping.result)
        assertEquals("surf-punish.\$rpc.v1.audit-service.ping", ping.subject("surf-punish"))
    }

    @Test
    fun globalServiceAndRecipientsInheritance() {
        val lookup = method(RegistryService, "lookup")
        assertTrue(lookup.service.global)
        assertEquals(Recipients.ANY_INSTANCE, lookup.recipients)
        assertEquals(NatsResultKind.VALUE, lookup.result)
        assertEquals("registry-service.\$rpc.v1.lookup", lookup.subject(null))
        assertEquals("registry-service.\$rpc.v1.lookup", lookup.subject("ignored"))

        val announce = method(RegistryService, "announce")
        assertEquals(Recipients.ALL_INSTANCES, announce.recipients)
        assertEquals(NatsResultKind.UNIT, announce.result)
    }

    @Test
    fun versionedServiceNames() {
        val find = method(PlayerServiceV2, "findPlayer")
        assertEquals(2, find.service.majorVersion)
        assertEquals("surf-punish.\$rpc.v2.players.find-player", find.subject("surf-punish"))
    }

    @Test
    fun streamingMethodsAreRejectedWithTheirService() {
        val error = assertFailsWith<IllegalArgumentException> { method(StreamingService, "watch") }
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

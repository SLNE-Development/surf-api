package dev.slne.surf.nats.protocol

import dev.slne.surf.nats.protocol.RecipientsOption.ALL_INSTANCES
import dev.slne.surf.nats.protocol.RecipientsOption.ANY_INSTANCE
import dev.slne.surf.nats.protocol.RecipientsOption.UNSPECIFIED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ProtocolResolutionTest {

    @Test
    fun namesInheritKebabCaseWhenUnsetOrEmpty() {
        assertEquals("player-health-changed", ProtocolResolution.name(null, "PlayerHealthChanged"))
        assertEquals("find-server", ProtocolResolution.name("", "FindServer"))
        assertEquals("punishment.created", ProtocolResolution.name("punishment.created", "PunishmentCreated"))
        assertEquals("Custom", ProtocolResolution.name("Custom", "Other"))
    }

    @Test
    fun persistentResolvesMethodOverService() {
        assertFalse(ProtocolResolution.persistent(method = null, service = null))
        assertTrue(ProtocolResolution.persistent(method = null, service = true))
        assertFalse(ProtocolResolution.persistent(method = null, service = false))
        assertFalse(ProtocolResolution.persistent(method = false, service = true))
        assertTrue(ProtocolResolution.persistent(method = true, service = null))
        assertTrue(ProtocolResolution.persistent(method = true, service = false))
    }

    @Test
    fun callTimeoutResolvesMethodOverServiceAndZeroInherits() {
        assertNull(ProtocolResolution.callTimeoutSeconds(method = null, service = null))
        assertNull(ProtocolResolution.callTimeoutSeconds(method = 0, service = null))
        assertNull(ProtocolResolution.callTimeoutSeconds(method = 0, service = 0))
        assertEquals(30, ProtocolResolution.callTimeoutSeconds(method = null, service = 30))
        assertEquals(30, ProtocolResolution.callTimeoutSeconds(method = 0, service = 30))
        assertEquals(10, ProtocolResolution.callTimeoutSeconds(method = 10, service = 30))
        assertEquals(10, ProtocolResolution.callTimeoutSeconds(method = 10, service = null))
    }

    @Test
    fun effectiveCallTimeoutFallsBackToDefaults() {
        assertEquals(5, ProtocolResolution.effectiveCallTimeoutSeconds(null, persistent = false))
        assertEquals(60, ProtocolResolution.effectiveCallTimeoutSeconds(null, persistent = true))
        assertEquals(7, ProtocolResolution.effectiveCallTimeoutSeconds(7, persistent = true))
        assertEquals(7, ProtocolResolution.effectiveCallTimeoutSeconds(7, persistent = false))
    }

    @Test
    fun recipientsResolveMethodOverServiceAndUnspecifiedInherits() {
        assertEquals(ANY_INSTANCE, ProtocolResolution.recipients(method = null, service = null))
        assertEquals(ANY_INSTANCE, ProtocolResolution.recipients(method = UNSPECIFIED, service = UNSPECIFIED))
        assertEquals(ALL_INSTANCES, ProtocolResolution.recipients(method = null, service = ALL_INSTANCES))
        assertEquals(ALL_INSTANCES, ProtocolResolution.recipients(method = UNSPECIFIED, service = ALL_INSTANCES))
        assertEquals(ALL_INSTANCES, ProtocolResolution.recipients(method = ALL_INSTANCES, service = UNSPECIFIED))
        assertEquals(ANY_INSTANCE, ProtocolResolution.recipients(method = ANY_INSTANCE, service = ALL_INSTANCES))
        assertEquals(ALL_INSTANCES, ProtocolResolution.recipients(method = ALL_INSTANCES, service = null))
    }

    @Test
    fun recipientsOptionMatchesProtoEnum() {
        assertEquals(UNSPECIFIED, RecipientsOption.ofNumber(0))
        assertEquals(ANY_INSTANCE, RecipientsOption.ofNumber(1))
        assertEquals(ALL_INSTANCES, RecipientsOption.ofNumber(2))
        assertNull(RecipientsOption.ofNumber(3))
        assertNull(RecipientsOption.ofNumber(-1))
        assertEquals(ANY_INSTANCE, RecipientsOption.ofName("NATS_RECIPIENTS_ANY_INSTANCE"))
        assertEquals(ProtocolVectors.recipientsConstants.size, RecipientsOption.entries.size)
        for ((protoName, number) in ProtocolVectors.recipientsConstants) {
            val option = assertNotNull(RecipientsOption.ofName(protoName), protoName)
            assertEquals(number, option.number, protoName)
            assertEquals(protoName, option.protoName)
            assertEquals("NATS_RECIPIENTS_${option.name}", protoName)
            assertEquals(option, RecipientsOption.ofNumber(number), protoName)
        }
        for (name in ProtocolVectors.invalidRecipientsNames) {
            assertNull(RecipientsOption.ofName(name), "'$name'")
        }
    }

    @Test
    fun defaults() {
        assertEquals(5, ProtocolDefaults.CORE_TIMEOUT_SECONDS)
        assertEquals(60, ProtocolDefaults.PERSISTENT_TIMEOUT_SECONDS)
        assertEquals(120, ProtocolDefaults.DEDUPE_WINDOW_SECONDS)
        assertEquals(7.days.inWholeSeconds, ProtocolDefaults.PERSIST_MAX_AGE_SECONDS.toLong())
        assertEquals("surf-nats-payloads", ProtocolDefaults.TRANSIENT_PAYLOAD_BUCKET)
        assertEquals(600, ProtocolDefaults.TRANSIENT_PAYLOAD_TTL_SECONDS)
        assertEquals("surf-nats-persistent-payloads", ProtocolDefaults.PERSISTENT_PAYLOAD_BUCKET)
        assertEquals(ProtocolDefaults.PERSIST_MAX_AGE_SECONDS, ProtocolDefaults.PERSISTENT_PAYLOAD_TTL_SECONDS)
        assertEquals(128, ProtocolDefaults.PAYLOAD_REF_RESERVE)
        assertEquals(4096, ProtocolDefaults.MAX_ERROR_MESSAGE_LENGTH)
        assertEquals(5, ProtocolDefaults.PRESENCE_HEARTBEAT_SECONDS)
        assertEquals(15, ProtocolDefaults.PRESENCE_TTL_SECONDS)
    }

    @Test
    fun natsTtlRoundsUpToWholeSecondsWithinTheStreamMaxAge() {
        assertEquals(1, ProtocolDefaults.natsTtlSeconds(Duration.ZERO))
        assertEquals(1, ProtocolDefaults.natsTtlSeconds((-5).seconds))
        assertEquals(1, ProtocolDefaults.natsTtlSeconds(1.milliseconds))
        assertEquals(1, ProtocolDefaults.natsTtlSeconds(1000.milliseconds))
        assertEquals(2, ProtocolDefaults.natsTtlSeconds(1001.milliseconds))
        assertEquals(60, ProtocolDefaults.natsTtlSeconds(60.seconds))
        assertEquals(604_800, ProtocolDefaults.natsTtlSeconds(7.days))
        assertEquals(604_800, ProtocolDefaults.natsTtlSeconds(8.days))
        assertEquals(604_800, ProtocolDefaults.natsTtlSeconds(Duration.INFINITE))
    }

    @Test
    fun offloadThresholdIncludesHeadersAndReserve() {
        assertFalse(ProtocolDefaults.requiresOffload(100, 40, 10, maxPayload = 278))
        assertTrue(ProtocolDefaults.requiresOffload(100, 40, 10, maxPayload = 277))
        assertTrue(ProtocolDefaults.requiresOffload(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE.toLong()))
        assertFalse(ProtocolDefaults.requiresOffload(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Long.MAX_VALUE))
    }

    @Test
    fun offloadThresholdCountsHeaderLines() {
        // jnats writes `name:value\r\n`, nats.js `\r\nname: value`
        assertEquals(20 + 13 + 3, ProtocolDefaults.headerLineBytes("Nats-Expected-Stream", "1700000005000", 3))
        assertEquals(20 + 13 + 4, ProtocolDefaults.headerLineBytes("Nats-Expected-Stream", "1700000005000", 4))

        val line = ProtocolDefaults.headerLineBytes(ProtocolHeaders.DEADLINE, "1700000005000", 3)
        val maxPayload = 1_048_576L
        val atThreshold = (maxPayload - 200 - line - ProtocolDefaults.PAYLOAD_REF_RESERVE).toInt()
        assertFalse(ProtocolDefaults.requiresOffload(atThreshold, 200, line, maxPayload))
        assertTrue(ProtocolDefaults.requiresOffload(atThreshold + 1, 200, line, maxPayload))
    }

    @Test
    fun payloadBucketsUseTheProtocolNamesAndTtls() {
        assertEquals(ProtocolDefaults.TRANSIENT_PAYLOAD_BUCKET, PayloadBucket.TRANSIENT.bucketName)
        assertEquals(ProtocolDefaults.TRANSIENT_PAYLOAD_TTL_SECONDS.seconds, PayloadBucket.TRANSIENT.ttl)
        assertEquals(ProtocolDefaults.PERSISTENT_PAYLOAD_BUCKET, PayloadBucket.PERSISTENT.bucketName)
        assertEquals(ProtocolDefaults.PERSISTENT_PAYLOAD_TTL_SECONDS.seconds, PayloadBucket.PERSISTENT.ttl)
        assertEquals(
            "Payload of 5 bytes must be offloaded, but its lifetime 11m exceeds the TTL of 10m (bucket surf-nats-payloads)",
            PayloadBucket.TRANSIENT.lifetimeExceededMessage(5, 660.seconds),
        )
    }

    @Test
    fun eventsResolveTheirSubject() {
        val event = ProtocolResolution.event(EventOptions(), "PlayerHealthChanged")
        assertEquals("player-health-changed", event.subject)
        assertFalse(event.global)
        assertEquals(emptyList(), event.problems("a.PlayerHealthChanged"))

        val global = ProtocolResolution.event(EventOptions(subject = "roles.whitelisted", global = true), "X")
        assertEquals("roles.whitelisted", global.subject)
        assertTrue(global.global)

        assertEquals(
            listOf(
                "event a.X has an invalid subject '\$rpc.x' (tokens must not be empty, contain '.', '*', '>' " +
                        "or whitespace, or start with '\$')"
            ),
            ProtocolResolution.event(EventOptions(subject = "\$rpc.x"), "X").problems("a.X"),
        )
    }

    @Test
    fun methodsResolveAgainstTheirService() {
        val service = ProtocolResolution.service(
            ServiceOptions(callTimeoutSeconds = 30, recipients = ALL_INSTANCES, persistent = true),
            "PlayerStats",
        )
        assertEquals("player-stats", service.name)
        assertFalse(service.global)
        assertEquals(emptyList(), service.problems("a.PlayerStats"))

        val inherited = ProtocolResolution.method(MethodOptions(), service, "FindPlayer", emptyResponse = false, streaming = false)
        assertEquals("find-player", inherited.name)
        assertTrue(inherited.persistent)
        assertEquals(30, inherited.callTimeoutSeconds)
        assertEquals(30, inherited.effectiveCallTimeoutSeconds)
        assertEquals(ALL_INSTANCES, inherited.recipients)
        assertEquals(ResultKindOption.ANSWER, inherited.resultKind)
        assertEquals(listOf("rpc a.FindPlayer: persistent methods cannot address ALL_INSTANCES"), inherited.problems("a.FindPlayer"))

        val own = ProtocolResolution.method(
            MethodOptions(name = "find", persistent = false, callTimeoutSeconds = 0, recipients = ANY_INSTANCE),
            service,
            "FindPlayer",
            emptyResponse = false,
            streaming = false,
        )
        assertEquals("find", own.name)
        assertFalse(own.persistent)
        assertEquals(30, own.callTimeoutSeconds)
        assertEquals(ResultKindOption.VALUE, own.resultKind)
        assertEquals(emptyList(), own.problems("a.FindPlayer"))
    }

    @Test
    fun resultKindsFollowTheResponseAndRecipients() {
        assertEquals(ResultKindOption.UNIT, ResultKindOption.of(emptyResponse = true, recipients = ALL_INSTANCES))
        assertEquals(ResultKindOption.UNIT, ResultKindOption.of(emptyResponse = true, recipients = ANY_INSTANCE))
        assertEquals(ResultKindOption.ANSWER, ResultKindOption.of(emptyResponse = false, recipients = ALL_INSTANCES))
        assertEquals(ResultKindOption.VALUE, ResultKindOption.of(emptyResponse = false, recipients = ANY_INSTANCE))
    }

    @Test
    fun methodProblems() {
        val core = ProtocolResolution.service(ServiceOptions(), "S")
        fun problems(options: MethodOptions, emptyResponse: Boolean = true, streaming: Boolean = false) =
            ProtocolResolution.method(options, core, "A", emptyResponse, streaming).problems("A")

        assertEquals(listOf("rpc A: streaming rpcs are not supported"), problems(MethodOptions(name = "a.b"), streaming = true))
        assertEquals(
            listOf(
                "rpc A resolves to the method name 'a.b', which is not a single subject token " +
                        "(no '.', '*', '>', whitespace, no leading '\$')"
            ),
            problems(MethodOptions(name = "a.b")),
        )
        assertEquals(emptyList(), problems(MethodOptions(persistent = true, callTimeoutSeconds = 120)))
        assertEquals(
            listOf("rpc A: the call timeout of a persistent fire method (121s) must not exceed the 120s duplicate window"),
            problems(MethodOptions(persistent = true, callTimeoutSeconds = 121)),
        )
        assertEquals(emptyList(), problems(MethodOptions(persistent = true, callTimeoutSeconds = 121), emptyResponse = false))
    }

    @Test
    fun requiredFieldsHaveExplicitPresenceAndNoFallback() {
        assertTrue(ProtocolFieldRules.isRequired(explicitPresence = true, oneofMember = false, nullable = false, hasDefault = false))
        assertFalse(ProtocolFieldRules.isRequired(explicitPresence = false, oneofMember = false, nullable = false, hasDefault = false))
        assertFalse(ProtocolFieldRules.isRequired(explicitPresence = true, oneofMember = true, nullable = false, hasDefault = false))
        assertFalse(ProtocolFieldRules.isRequired(explicitPresence = true, oneofMember = false, nullable = true, hasDefault = false))
        assertFalse(ProtocolFieldRules.isRequired(explicitPresence = true, oneofMember = false, nullable = false, hasDefault = true))
    }

    @Test
    fun errorMessagesAreCapped() {
        assertEquals("", ProtocolDefaults.errorMessage(null))
        assertEquals("boom", ProtocolDefaults.errorMessage("boom"))
        assertEquals(4096, ProtocolDefaults.errorMessage("x".repeat(5000)).length)
    }
}

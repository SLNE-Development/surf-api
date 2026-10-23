package dev.slne.surf.nats.protocol

/**
 * Golden protocol vectors shared by the generator, the JVM runtime and the JS runtime tests.
 *
 * The kebab-case outputs were produced by the regex implementation that the JVM runtime used before the shared
 * sources (`(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])`, pieces lowercased and joined with `-`).
 */
internal object ProtocolVectors {

    val kebabCase = listOf(
        "PlayerHealthChanged" to "player-health-changed",
        "NATSEvent" to "nats-event",
        "HTTPServer2Up" to "http-server2-up",
        "Event2Created" to "event2-created",
        "Ping" to "ping",
        "A" to "a",
        "a" to "a",
        "AB" to "ab",
        "ABc" to "a-bc",
        "aB" to "a-b",
        "aBC" to "a-bc",
        "ABCDef" to "abc-def",
        "X2" to "x2",
        "X2Y" to "x2-y",
        "x9Y" to "x9-y",
        "A1B2C3" to "a1-b2-c3",
        "IOError" to "io-error",
        "GetHTTP" to "get-http",
        "ServerV2" to "server-v2",
        "V2Server" to "v2-server",
        "v1Thing" to "v1-thing",
        "PlayerV2Event" to "player-v2-event",
        "Player2" to "player2",
        "HTTP2Server" to "http2-server",
        "Server2HTTP" to "server2-http",
        "FindServer" to "find-server",
        "findServer" to "find-server",
        "playerHealth" to "player-health",
        "getPlayerByUUID" to "get-player-by-uuid",
        "UUIDToString" to "uuid-to-string",
        "lowercase" to "lowercase",
        "Already-Kebab" to "already-kebab",
        "Player_Health" to "player_health",
        "Player_HealthChanged" to "player_health-changed",
        "PLAYER_HEALTH" to "player_health",
        "Mixed_CASE_Name" to "mixed_case_name",
        "GetPlayer_ByName" to "get-player_by-name",
        "get_player" to "get_player",
        "a_B" to "a_b",
        "A_b" to "a_b",
        "Ab_Cd" to "ab_cd",
        "_Leading" to "_leading",
        "Trailing_" to "trailing_",
        "ÄBc" to "äbc",
        "" to "",
    )

    val validTokens = listOf("a", "\$rpc", "b-c", "d_e", "Mixed", "1", "a\$b", "a/b", "#", "x y", "ä")

    val invalidTokens = listOf(
        "", "a.b", ".", "*", ">", "a*", "a>", " ", "a b", "a\tb", "a\nb", "a\u000Bb", "a\u000Cb", "a\rb",
    )

    val validSubjects = listOf(
        "a",
        "punishment.created",
        "a.b-c.d_e",
        "Mixed.Case",
        "surf-punish.\$rpc.v1.player.ban",
        "roles.\$persist.v2.grant",
        "x y.z",
    )

    val invalidSubjects = listOf(
        "", "a b", "a..b", "a.*", "a.>", ".a", "a.", ".", "..", "a\tb", "a\nb", "a.b\n", "a\u000Bb", "a\u000Cb",
        "a\rb", "a.b c",
    )

    val validEventSubjects = listOf("a", "punishment.created", "a.b\$", "x.y-z.w_v")

    val invalidEventSubjects = listOf("\$rpc", "a.\$b", "\$", "a.\$rpc.b") + invalidSubjects

    val kebabNames = listOf("a", "surf-punish", "a1-b2", "1", "abc-def-ghi")

    val invalidKebabNames = listOf("", "-", "-a", "a-", "a--b", "A", "a_b", "a.b", "a b", "ä")

    val majorVersions = listOf(
        "surf.punish.v1" to 1,
        "surf.types.paper.v1" to 1,
        "a.v2" to 2,
        "surf.x.v10" to 10,
        "x.v2147483647" to Int.MAX_VALUE,
    )

    val invalidVersionPackages = listOf(
        "",
        "v1",
        ".v1",
        "surf.punish",
        "surf.punish.",
        "surf.punish.v",
        "surf.punish.v0",
        "surf.punish.v01",
        "surf.punish.1",
        "surf.punish.V1",
        "surf.punish.vx1",
        "surf.punish.v1beta1",
        "surf.punish.v1.",
        "surf.punish.v-1",
        "surf.punish.v+1",
        "surf.v1.punish",
        "surf.punish.v2147483648",
    )

    // surf.nats.v1.NatsRecipients in surf/nats/v1/options.proto.
    val recipientsConstants = listOf(
        "NATS_RECIPIENTS_UNSPECIFIED" to 0,
        "NATS_RECIPIENTS_ANY_INSTANCE" to 1,
        "NATS_RECIPIENTS_ALL_INSTANCES" to 2,
    )

    val invalidRecipientsNames = listOf(
        "",
        "UNSPECIFIED",
        "ANY_INSTANCE",
        "ALL_INSTANCES",
        "NATS_RECIPIENTS_",
        "nats_recipients_any_instance",
        "NATS_RECIPIENTS_ANY_INSTANCE ",
        "NatsRecipients.NATS_RECIPIENTS_ANY_INSTANCE",
    )

    class EventVector(val client: String?, val subject: String, val majorVersion: Int, val expected: String)

    val events = listOf(
        EventVector("punish", "punishment.created", 1, "punish.v1.punishment.created"),
        EventVector("punish", "created", 1, "punish.v1.created"),
        EventVector("surf-punish", "player-health-changed", 3, "surf-punish.v3.player-health-changed"),
        EventVector(null, "roles.whitelisted", 1, "roles.v1.whitelisted"),
        EventVector(null, "whitelisted", 1, "whitelisted.v1"),
        EventVector(null, "a.b.c", 12, "a.v12.b.c"),
    )

    class MethodVector(val method: String, val persistent: Boolean, val expected: String)

    class ServiceVector(
        val client: String?,
        val service: String,
        val majorVersion: Int,
        val rpcBase: String,
        val persistBase: String,
        val presenceSubject: String,
        val dlqSubject: String,
        val streamFilter: String,
        val stream: String,
        val dlqStream: String,
        val responderGroup: String,
        val registrationKey: String,
        val methods: List<MethodVector>,
    )

    val services = listOf(
        ServiceVector(
            client = "surf-punish",
            service = "player",
            majorVersion = 1,
            rpcBase = "surf-punish.\$rpc.v1.player",
            persistBase = "surf-punish.\$persist.v1.player",
            presenceSubject = "surf-punish.\$presence.v1.player",
            dlqSubject = "surf-punish.\$dlq.v1.player",
            streamFilter = "surf-punish.\$persist.v1.player.*",
            stream = "SURF_SURF_PUNISH_PLAYER_V1",
            dlqStream = "SURF_SURF_PUNISH_PLAYER_V1_DLQ",
            responderGroup = "surf-punish",
            registrationKey = "client:player:v1",
            methods = listOf(
                MethodVector("health", persistent = false, "surf-punish.\$rpc.v1.player.health"),
                MethodVector("ban", persistent = true, "surf-punish.\$persist.v1.player.ban"),
            ),
        ),
        ServiceVector(
            client = "surf-punish",
            service = "player",
            majorVersion = 2,
            rpcBase = "surf-punish.\$rpc.v2.player",
            persistBase = "surf-punish.\$persist.v2.player",
            presenceSubject = "surf-punish.\$presence.v2.player",
            dlqSubject = "surf-punish.\$dlq.v2.player",
            streamFilter = "surf-punish.\$persist.v2.player.*",
            stream = "SURF_SURF_PUNISH_PLAYER_V2",
            dlqStream = "SURF_SURF_PUNISH_PLAYER_V2_DLQ",
            responderGroup = "surf-punish",
            registrationKey = "client:player:v2",
            methods = listOf(
                MethodVector("health", persistent = false, "surf-punish.\$rpc.v2.player.health"),
            ),
        ),
        ServiceVector(
            client = "punish",
            service = "player-stats",
            majorVersion = 10,
            rpcBase = "punish.\$rpc.v10.player-stats",
            persistBase = "punish.\$persist.v10.player-stats",
            presenceSubject = "punish.\$presence.v10.player-stats",
            dlqSubject = "punish.\$dlq.v10.player-stats",
            streamFilter = "punish.\$persist.v10.player-stats.*",
            stream = "SURF_PUNISH_PLAYER_STATS_V10",
            dlqStream = "SURF_PUNISH_PLAYER_STATS_V10_DLQ",
            responderGroup = "punish",
            registrationKey = "client:player-stats:v10",
            methods = listOf(
                MethodVector("top-players", persistent = false, "punish.\$rpc.v10.player-stats.top-players"),
            ),
        ),
        ServiceVector(
            client = null,
            service = "roles",
            majorVersion = 2,
            rpcBase = "roles.\$rpc.v2",
            persistBase = "roles.\$persist.v2",
            presenceSubject = "roles.\$presence.v2",
            dlqSubject = "roles.\$dlq.v2",
            streamFilter = "roles.\$persist.v2.*",
            stream = "SURF__ROLES_V2",
            dlqStream = "SURF__ROLES_V2_DLQ",
            responderGroup = "roles",
            registrationKey = "global:roles:v2",
            methods = listOf(
                MethodVector("find-server", persistent = false, "roles.\$rpc.v2.find-server"),
                MethodVector("grant", persistent = true, "roles.\$persist.v2.grant"),
            ),
        ),
        ServiceVector(
            client = null,
            service = "player-sync",
            majorVersion = 1,
            rpcBase = "player-sync.\$rpc.v1",
            persistBase = "player-sync.\$persist.v1",
            presenceSubject = "player-sync.\$presence.v1",
            dlqSubject = "player-sync.\$dlq.v1",
            streamFilter = "player-sync.\$persist.v1.*",
            stream = "SURF__PLAYER_SYNC_V1",
            dlqStream = "SURF__PLAYER_SYNC_V1_DLQ",
            responderGroup = "player-sync",
            registrationKey = "global:player-sync:v1",
            methods = listOf(
                MethodVector("push", persistent = true, "player-sync.\$persist.v1.push"),
            ),
        ),
    )
}

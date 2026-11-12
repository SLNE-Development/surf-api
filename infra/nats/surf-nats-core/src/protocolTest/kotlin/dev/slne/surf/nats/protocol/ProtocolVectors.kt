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

    val validSubjects = listOf(
        "a",
        "punishment.created",
        "a.b-c.d_e",
        "Mixed.Case",
        "surf.rpc.surf.punish.v1.PlayerService.Ban",
        "surf.persist.surf.roles.v2.RolesService.Grant",
        "x y.z",
    )

    val invalidSubjects = listOf(
        "", "a b", "a..b", "a.*", "a.>", ".a", "a.", ".", "..", "a\tb", "a\nb", "a.b\n", "a\u000Bb", "a\u000Cb",
        "a\rb", "a.b c",
    )

    val protoNames = listOf(
        "a",
        "A",
        "surf.punish.v1.PlayerService",
        "surf.demo.v1.PlayerJoined.Location",
        "a.b_c",
        "a_b.c",
        "a_",
        "x__y.Z9",
        "HTTPServer2Up",
    )

    val invalidProtoNames = listOf(
        "", ".", "..", "a.", ".a", "a..b", "_a", "a._b", "1a", "a.1b", "a-b", "a b", "a\$", "\$rpc", "ä", "a.*",
        "a.>", "a.b ",
    )

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

    class EventVector(val messageName: String, val expected: String)

    val events = listOf(
        EventVector("surf.punish.v1.PlayerJoined", "surf.evt.surf.punish.v1.PlayerJoined"),
        EventVector("surf.demo.v1.PlayerJoined.Location", "surf.evt.surf.demo.v1.PlayerJoined.Location"),
        EventVector("surf.servers.v2.HTTPServer2Up", "surf.evt.surf.servers.v2.HTTPServer2Up"),
        EventVector("my_pkg.v1.Snake_Event", "surf.evt.my_pkg.v1.Snake_Event"),
    )

    class MethodVector(val method: String, val persistent: Boolean, val expected: String)

    class ServiceVector(
        val service: String,
        val rpcBase: String,
        val persistBase: String,
        val presenceSubject: String,
        val dlqSubject: String,
        val streamFilter: String,
        val stream: String,
        val dlqStream: String,
        val methods: List<MethodVector>,
    )

    val services = listOf(
        ServiceVector(
            service = "surf.punish.v1.PlayerService",
            rpcBase = "surf.rpc.surf.punish.v1.PlayerService",
            persistBase = "surf.persist.surf.punish.v1.PlayerService",
            presenceSubject = "surf.presence.surf.punish.v1.PlayerService",
            dlqSubject = "surf.dlq.surf.punish.v1.PlayerService",
            streamFilter = "surf.persist.surf.punish.v1.PlayerService.*",
            stream = "SURF_PERSIST_surf_punish_v1_PlayerService",
            dlqStream = "SURF_DLQ_surf_punish_v1_PlayerService",
            methods = listOf(
                MethodVector("FindPlayer", persistent = false, "surf.rpc.surf.punish.v1.PlayerService.FindPlayer"),
                MethodVector("Store", persistent = true, "surf.persist.surf.punish.v1.PlayerService.Store"),
            ),
        ),
        ServiceVector(
            service = "surf.punish.v2.PlayerService",
            rpcBase = "surf.rpc.surf.punish.v2.PlayerService",
            persistBase = "surf.persist.surf.punish.v2.PlayerService",
            presenceSubject = "surf.presence.surf.punish.v2.PlayerService",
            dlqSubject = "surf.dlq.surf.punish.v2.PlayerService",
            streamFilter = "surf.persist.surf.punish.v2.PlayerService.*",
            stream = "SURF_PERSIST_surf_punish_v2_PlayerService",
            dlqStream = "SURF_DLQ_surf_punish_v2_PlayerService",
            methods = listOf(
                MethodVector("FindPlayer", persistent = false, "surf.rpc.surf.punish.v2.PlayerService.FindPlayer"),
            ),
        ),
        ServiceVector(
            service = "my_pkg.v10.Snake_Service",
            rpcBase = "surf.rpc.my_pkg.v10.Snake_Service",
            persistBase = "surf.persist.my_pkg.v10.Snake_Service",
            presenceSubject = "surf.presence.my_pkg.v10.Snake_Service",
            dlqSubject = "surf.dlq.my_pkg.v10.Snake_Service",
            streamFilter = "surf.persist.my_pkg.v10.Snake_Service.*",
            stream = "SURF_PERSIST_my__pkg_v10_Snake__Service",
            dlqStream = "SURF_DLQ_my__pkg_v10_Snake__Service",
            methods = listOf(
                MethodVector("get_player", persistent = false, "surf.rpc.my_pkg.v10.Snake_Service.get_player"),
                MethodVector("Push", persistent = true, "surf.persist.my_pkg.v10.Snake_Service.Push"),
            ),
        ),
        ServiceVector(
            service = "a.b_c",
            rpcBase = "surf.rpc.a.b_c",
            persistBase = "surf.persist.a.b_c",
            presenceSubject = "surf.presence.a.b_c",
            dlqSubject = "surf.dlq.a.b_c",
            streamFilter = "surf.persist.a.b_c.*",
            stream = "SURF_PERSIST_a_b__c",
            dlqStream = "SURF_DLQ_a_b__c",
            methods = emptyList(),
        ),
        ServiceVector(
            service = "a_b.c",
            rpcBase = "surf.rpc.a_b.c",
            persistBase = "surf.persist.a_b.c",
            presenceSubject = "surf.presence.a_b.c",
            dlqSubject = "surf.dlq.a_b.c",
            streamFilter = "surf.persist.a_b.c.*",
            stream = "SURF_PERSIST_a__b_c",
            dlqStream = "SURF_DLQ_a__b_c",
            methods = emptyList(),
        ),
    )
}

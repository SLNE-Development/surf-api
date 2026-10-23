package dev.slne.surf.nats.gradle.generator

import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.OPTIONS_IMPORT
import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.PROTO2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

/**
 * Events and services: descriptors, option resolution and flattened rpc signatures.
 */
class ContractGenerationTest {
    @TempDir
    lateinit var temp: File

    private fun contracts(body: String, protoPackage: String = "surf.test.v1", vararg imports: String): String {
        return GeneratorHarness(temp).proto(
            "contracts.proto",
            """
            |$PROTO2
            |package $protoPackage;
            |$OPTIONS_IMPORT
            |import "google/protobuf/empty.proto";
            |${imports.joinToString("\n") { "import \"$it\";" }}
            |$body
            """.trimMargin(),
        ).generateFile("Contracts.kt")
    }

    @Test
    fun `events implement NatsEvent and expose a static descriptor`() {
        val text = contracts(
            """
            message PlayerHealthChanged {
              option (surf.nats.v1.event) = {};
              optional double health = 1;
            }
            """
        )
        assertContainsAll(
            text,
            "import dev.slne.surf.nats.descriptor.NatsEvent\n",
            "public data class PlayerHealthChanged(\n    @ProtoNumber(1)\n    public val health: Double,\n) : NatsEvent {",
            "override val natsDescriptor: NatsEventDescriptor<*>\n        get() = NATS_DESCRIPTOR",
            "public companion object {\n        @JvmField\n        public val NATS_DESCRIPTOR: NatsEventDescriptor<PlayerHealthChanged> =",
            "protoName = \"surf.test.v1.PlayerHealthChanged\",",
            "subject = \"player-health-changed\",",
            "global = false,",
            "majorVersion = 1,",
            "serializer = serializer(),",
        )
        assertNotContains(text, "import dev.slne.surf.nats.event.NatsEvent")
    }

    @Test
    fun `event subjects default to the kebab-case message name of the shared protocol`() {
        val text = contracts(
            """
            message HTTPServer2Up { option (surf.nats.v1.event) = {}; }
            message NATSEvent { option (surf.nats.v1.event).global = true; }
            message Explicit { option (surf.nats.v1.event) = { subject: "panel.punishment.created" global: true }; }
            message Blank { option (surf.nats.v1.event) = { subject: "" }; }
            """,
            protoPackage = "surf.test.v3",
        )
        assertContainsAll(
            text,
            "subject = \"http-server2-up\",\n                    global = false,\n                    majorVersion = 3,",
            "subject = \"nats-event\",\n                    global = true,",
            "subject = \"panel.punishment.created\",\n                    global = true,",
            "subject = \"blank\",",
        )
    }

    @Test
    fun `nested messages can be events`() {
        val text = contracts("message Outer { message Inner { option (surf.nats.v1.event) = {}; optional int32 x = 1; } }")
        assertContainsAll(text, "public data class Inner(", ") : NatsEvent {", "subject = \"inner\",")
    }

    @Test
    fun `messages without the event option are not events`() {
        val text = contracts("message Plain { optional int32 x = 1; }")
        assertNotContains(text, "NatsEvent", "NATS_DESCRIPTOR", "companion")
    }

    @Test
    fun `service methods take the flattened request fields as parameters`() {
        val text = contracts(
            """
            service PlayerService {
              rpc FindServer(FindServerRequest) returns (FindServerReply);
            }
            message FindServerRequest {
              optional string player = 1;
              optional int32 limit = 2 [default = 5];
              optional string region = 3 [(surf.nats.v1.nullable) = true];
              repeated string tags = 4;
              oneof filter { string name = 5; int64 id = 6; }
              optional surf.types.v1.Uuid owner = 7;
            }
            message FindServerReply { optional string server = 1 [(surf.nats.v1.nullable) = true]; }
            """,
            imports = arrayOf("surf/types/v1/uuid.proto"),
        )
        assertContainsAll(
            text,
            "public interface PlayerService {\n    public suspend fun findServer(\n        player: String,\n" +
                    "        limit: Int = 5,\n        region: String? = null,\n        tags: List<String> = emptyList(),\n" +
                    "        filter: FindServerRequest.Filter? = null,\n        owner: UUID,\n    ): FindServerReply",
            "NatsMethodDescriptor<FindServerRequest, FindServerReply>(",
            "protoName = \"FindServer\",",
            "name = \"find-server\",",
            "kotlinName = \"findServer\",",
            "NatsParameter(name = \"player\", type = String::class, nullable = false),",
            "NatsParameter(name = \"region\", type = String::class, nullable = true),",
            "NatsParameter(name = \"tags\", type = List::class, nullable = false),",
            "NatsParameter(name = \"filter\", type = FindServerRequest.Filter::class, nullable = true),",
            "NatsParameter(name = \"owner\", type = UUID::class, nullable = false),",
            "requestSerializer = FindServerRequest.serializer(),",
            "responseSerializer = FindServerReply.serializer(),",
            "toRequest = { a -> FindServerRequest(player = a[0] as String, limit = a[1] as Int, region = a[2] as String?, " +
                    "tags = a[3] as List<String>, filter = a[4] as FindServerRequest.Filter?, owner = a[5] as UUID) },",
            "toArguments = { r -> arrayOf<Any?>(r.player, r.limit, r.region, r.tags, r.filter, r.owner) },",
            "@JvmField\n        @Suppress(\"UNCHECKED_CAST\")\n        public val NATS_DESCRIPTOR: NatsServiceDescriptor<PlayerService> =",
        )
    }

    @Test
    fun `method parameters follow ascending field numbers of the request`() {
        val text = contracts(
            """
            service OrderService {
              rpc Place(PlaceRequest) returns (google.protobuf.Empty);
            }
            message PlaceRequest {
              optional string note = 3;
              optional int64 id = 1;
              optional int32 amount = 2;
            }
            """
        )
        assertContainsAll(
            text,
            "public suspend fun place(\n        id: Long,\n        amount: Int,\n        note: String,\n    )\n",
            "toRequest = { a -> PlaceRequest(id = a[0] as Long, amount = a[1] as Int, note = a[2] as String) },",
            "toArguments = { r -> arrayOf<Any?>(r.id, r.amount, r.note) },",
        )
        assertInOrder(
            text,
            "NatsParameter(name = \"id\", type = Long::class, nullable = false),",
            "NatsParameter(name = \"amount\", type = Int::class, nullable = false),",
            "NatsParameter(name = \"note\", type = String::class, nullable = false),",
        )
    }

    @Test
    fun `reordering request field declarations does not change the generated service`() {
        fun generate(root: String, request: String) = GeneratorHarness(temp.resolve(root)).proto(
            "contracts.proto",
            """
            |$PROTO2
            |package surf.test.v1;
            |$OPTIONS_IMPORT
            |service OrderService {
            |  rpc Place(PlaceRequest) returns (PlaceReply);
            |}
            |message PlaceReply { optional int64 id = 1; }
            |$request
            """.trimMargin(),
        ).generateFile("Contracts.kt")

        val declared = generate(
            "declared",
            "message PlaceRequest { optional string note = 3; oneof target { string server = 5; int64 player = 4; } " +
                    "optional int64 id = 1; optional int32 amount = 2 [default = 1]; }",
        )
        val reordered = generate(
            "reordered",
            "message PlaceRequest { optional int32 amount = 2 [default = 1]; optional int64 id = 1; " +
                    "oneof target { int64 player = 4; string server = 5; } optional string note = 3; }",
        )
        assertEquals(declared, reordered)
        assertContainsAll(
            declared,
            "public suspend fun place(\n        id: Long,\n        amount: Int = 1,\n        note: String,\n" +
                    "        target: PlaceRequest.Target? = null,\n    ): PlaceReply",
            "toArguments = { r -> arrayOf<Any?>(r.id, r.amount, r.note, r.target) },",
        )
    }

    @Test
    fun `empty and fieldless requests produce parameterless methods`() {
        val text = contracts(
            """
            service PingService {
              rpc Ping(google.protobuf.Empty) returns (Pong);
              rpc Hello(surf.test.v1.Hello) returns (google.protobuf.Empty);
            }
            message Pong { optional int64 at = 1; }
            message Hello {}
            """
        )
        assertContainsAll(
            text,
            "public suspend fun ping(): Pong",
            "public suspend fun hello()\n",
            "NatsMethodDescriptor<Unit, Pong>(",
            "requestSerializer = EmptySerializer,",
            "toRequest = { },",
            "NatsMethodDescriptor<Hello, Unit>(",
            "requestSerializer = Hello.serializer(),",
            "responseSerializer = null,",
            "toRequest = { HELLO_INSTANCE },",
            "toArguments = { NO_ARGUMENTS },",
            "private val NO_ARGUMENTS: Array<Any?> = emptyArray()",
            "private val HELLO_INSTANCE: Hello = Hello()",
            "parameters = emptyList(),",
            "import dev.slne.surf.nats.types.EmptySerializer",
        )
        assertNotContains(text, "UNCHECKED_CAST")
    }

    @Test
    fun `result kinds follow the response type and the resolved recipients`() {
        val text = contracts(
            """
            service FinderService {
              option (surf.nats.v1.service) = { recipients: NATS_RECIPIENTS_ALL_INSTANCES };
              rpc Find(Query) returns (Reply);
              rpc Broadcast(Query) returns (google.protobuf.Empty);
              rpc Single(Query) returns (Reply) { option (surf.nats.v1.method) = { recipients: NATS_RECIPIENTS_ANY_INSTANCE }; }
              rpc Inherit(Query) returns (Reply) { option (surf.nats.v1.method) = { recipients: NATS_RECIPIENTS_UNSPECIFIED }; }
            }
            message Query { optional int64 id = 1; }
            message Reply { optional string value = 1 [(surf.nats.v1.nullable) = true]; }
            """
        )
        assertContainsAll(
            text,
            "public suspend fun find(id: Long): Answer<Reply>",
            "public suspend fun broadcast(id: Long)\n",
            "public suspend fun single(id: Long): Reply",
            "public suspend fun inherit(id: Long): Answer<Reply>",
            "NatsMethodDescriptor<Query, Answer<Reply>>(\n                            protoName = \"Find\",",
            "recipients = Recipients.ALL_INSTANCES,\n                            resultKind = NatsResultKind.ANSWER,",
            "recipients = Recipients.ALL_INSTANCES,\n                            resultKind = NatsResultKind.UNIT,",
            "recipients = Recipients.ANY_INSTANCE,\n                            resultKind = NatsResultKind.VALUE,",
            "import dev.slne.surf.nats.rpc.Answer",
        )
    }

    @Test
    fun `method options inherit from service options`() {
        val text = contracts(
            """
            service OrderService {
              option (surf.nats.v1.service) = { name: "orders" persistent: true call_timeout_seconds: 30 };
              rpc Place(Order) returns (google.protobuf.Empty);
              rpc Ping(google.protobuf.Empty) returns (Order) { option (surf.nats.v1.method) = { persistent: false name: "health-check" }; }
              rpc Total(google.protobuf.Empty) returns (Order) { option (surf.nats.v1.method) = { call_timeout_seconds: 0 }; }
              rpc Slow(google.protobuf.Empty) returns (Order) { option (surf.nats.v1.method) = { call_timeout_seconds: 300 }; }
            }
            service PlainService { rpc Get(google.protobuf.Empty) returns (Order); }
            message Order { optional int64 id = 1; }
            """
        )
        assertContainsAll(
            text,
            "name = \"orders\",\n                    global = false,",
            "protoName = \"Place\",\n                            name = \"place\",\n                            kotlinName = \"place\",\n" +
                    "                            persistent = true,\n                            callTimeoutSeconds = 30,",
            "protoName = \"Ping\",\n                            name = \"health-check\",\n                            kotlinName = \"ping\",\n" +
                    "                            persistent = false,\n                            callTimeoutSeconds = 30,",
            "protoName = \"Total\",\n                            name = \"total\",\n                            kotlinName = \"total\",\n" +
                    "                            persistent = true,\n                            callTimeoutSeconds = 30,",
            "protoName = \"Slow\",\n                            name = \"slow\",\n                            kotlinName = \"slow\",\n" +
                    "                            persistent = true,\n                            callTimeoutSeconds = 300,",
            "name = \"plain-service\",",
            "protoName = \"Get\",\n                            name = \"get\",\n                            kotlinName = \"get\",\n" +
                    "                            persistent = false,\n                            callTimeoutSeconds = null,\n" +
                    "                            recipients = Recipients.ANY_INSTANCE,",
        )
    }

    @Test
    fun `global services carry the global flag and the package version`() {
        val text = contracts(
            """
            service RolesApi {
              option (surf.nats.v1.service) = { name: "roles-api" global: true };
              rpc Lookup(google.protobuf.Empty) returns (google.protobuf.Empty);
            }
            """,
            protoPackage = "surf.roles.v2",
        )
        assertContainsAll(
            text,
            "protoName = \"surf.roles.v2.RolesApi\",\n                    name = \"roles-api\",\n                    global = true,\n" +
                    "                    majorVersion = 2,",
            "public suspend fun lookup()\n",
        )
    }

    @Test
    fun `a persistent fire at exactly the duplicate window is accepted`() {
        val text = contracts(
            """
            service EdgeService {
              rpc Store(Item) returns (google.protobuf.Empty) { option (surf.nats.v1.method) = { persistent: true call_timeout_seconds: 120 }; }
            }
            message Item { optional int64 id = 1; }
            """
        )
        assertContainsAll(text, "callTimeoutSeconds = 120,")
    }

    @Test
    fun `timeout options accept hexadecimal integer literals`() {
        val text = contracts(
            """
            service EdgeService {
              option (surf.nats.v1.service) = { call_timeout_seconds: 0x10 };
              rpc Inherit(Item) returns (Item);
              rpc Own(Item) returns (Item) { option (surf.nats.v1.method).call_timeout_seconds = 0X1e; }
            }
            message Item { optional int64 id = 1; }
            """
        )
        assertInOrder(text, "kotlinName = \"inherit\",", "callTimeoutSeconds = 16,", "kotlinName = \"own\",", "callTimeoutSeconds = 30,")
    }

    @Test
    fun `octal timeout options are rejected by wire-schema`() {
        val errors = GeneratorHarness(temp).proto(
            "contracts.proto",
            "$PROTO2\npackage surf.test.v1;\n$OPTIONS_IMPORT\nservice EdgeService {\n" +
                    "  rpc Own(Item) returns (Item) { option (surf.nats.v1.method).call_timeout_seconds = 010; }\n}\n" +
                    "message Item { optional int64 id = 1; }",
        ).errors()
        assertError(errors, "contracts.proto:5:", "invalid option value \"010\" for uint32")
    }
}

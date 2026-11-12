package dev.slne.surf.nats.gradle.generator

import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.OPTIONS_IMPORT
import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.PROTO2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

/**
 * Events and services: descriptors, option resolution and rpc signatures.
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
              option (surf.nats.v1.event) = true;
              required double health = 1;
            }
            """
        )
        assertContainsAll(
            text,
            "import dev.slne.surf.nats.descriptor.NatsEvent\n",
            "public data class PlayerHealthChanged(\n    @ProtoNumber(1)\n    public val health: Double,\n) : NatsEvent {",
            "override val natsDescriptor: NatsEventDescriptor<*>\n        get() = NATS_DESCRIPTOR",
            "public companion object {\n        @JvmField\n        public val NATS_DESCRIPTOR: NatsEventDescriptor<PlayerHealthChanged> =",
            "NatsEventDescriptor<PlayerHealthChanged>(\n                    protoName = \"surf.test.v1.PlayerHealthChanged\",\n" +
                    "                    serializer = serializer(),\n                )",
        )
        assertNotContains(text, "import dev.slne.surf.nats.event.NatsEvent", "subject =", "global =", "majorVersion =")
    }

    @Test
    fun `event descriptors carry the exact proto full name`() {
        val text = contracts(
            """
            message HTTPServer2Up { option (surf.nats.v1.event) = true; }
            message snake_Event { option (surf.nats.v1.event) = true; }
            message Outer { message Inner { option (surf.nats.v1.event) = true; required int32 x = 1; } }
            """,
            protoPackage = "surf.test_pkg.v3",
        )
        assertContainsAll(
            text,
            "protoName = \"surf.test_pkg.v3.HTTPServer2Up\",",
            "protoName = \"surf.test_pkg.v3.snake_Event\",",
            "public data class Inner(",
            ") : NatsEvent {",
            "protoName = \"surf.test_pkg.v3.Outer.Inner\",",
        )
    }

    @Test
    fun `messages with the event option set to false are not events`() {
        val text = contracts("message Plain { option (surf.nats.v1.event) = false; required int32 x = 1; }")
        assertNotContains(text, "NatsEvent", "NATS_DESCRIPTOR", "companion")
    }

    @Test
    fun `messages without the event option are not events`() {
        val text = contracts("message Plain { required int32 x = 1; }")
        assertNotContains(text, "NatsEvent", "NATS_DESCRIPTOR", "companion")
    }

    @Test
    fun `service methods take the request message as their only parameter`() {
        val text = contracts(
            """
            service PlayerService {
              rpc FindServer(FindServerRequest) returns (FindServerReply);
            }
            message FindServerRequest {
              required string player = 1;
              optional int32 limit = 2 [default = 5];
              optional string region = 3;
              repeated string tags = 4;
              oneof filter { string name = 5; int64 id = 6; }
              required surf.types.v1.Uuid owner = 7;
            }
            message FindServerReply { optional string server = 1; }
            """,
            imports = arrayOf("surf/types/v1/uuid.proto"),
        )
        assertContainsAll(
            text,
            "public interface PlayerService {\n    public suspend fun findServer(request: FindServerRequest): FindServerReply\n",
            "NatsMethodDescriptor<FindServerRequest, FindServerReply>(",
            "protoName = \"FindServer\",",
            "kotlinName = \"findServer\",",
            "requestSerializer = FindServerRequest.serializer(),",
            "responseSerializer = FindServerReply.serializer(),\n                        ),",
            "@JvmField\n        public val NATS_DESCRIPTOR: NatsServiceDescriptor<PlayerService> =",
        )
        assertNotContains(text, "NatsParameter", "parameters =", "toRequest", "toArguments", "UNCHECKED_CAST")
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
            |message PlaceReply { required int64 id = 1; }
            |$request
            """.trimMargin(),
        ).generateFile("Contracts.kt")

        val declared = generate(
            "declared",
            "message PlaceRequest { required string note = 3; oneof target { string server = 5; int64 player = 4; } " +
                    "required int64 id = 1; optional int32 amount = 2 [default = 1]; }",
        )
        val reordered = generate(
            "reordered",
            "message PlaceRequest { optional int32 amount = 2 [default = 1]; required int64 id = 1; " +
                    "oneof target { int64 player = 4; string server = 5; } required string note = 3; }",
        )
        assertEquals(declared, reordered)
        assertContainsAll(declared, "public suspend fun place(request: PlaceRequest): PlaceReply\n")
    }

    @Test
    fun `Empty requests produce parameterless methods and fieldless requests keep their parameter`() {
        val text = contracts(
            """
            service PingService {
              rpc Ping(google.protobuf.Empty) returns (Pong);
              rpc Hello(surf.test.v1.Hello) returns (google.protobuf.Empty);
            }
            message Pong { required int64 at = 1; }
            message Hello {}
            """
        )
        assertContainsAll(
            text,
            "public suspend fun ping(): Pong\n",
            "public suspend fun hello(request: Hello)\n",
            "NatsMethodDescriptor<Unit, Pong>(",
            "requestSerializer = EmptySerializer,",
            "NatsMethodDescriptor<Hello, Unit>(",
            "requestSerializer = Hello.serializer(),",
            "responseSerializer = null,",
            "import dev.slne.surf.nats.types.EmptySerializer",
        )
        assertNotContains(
            text,
            "toRequest",
            "toArguments",
            "parameters =",
            "NO_ARGUMENTS",
            "HELLO_INSTANCE",
            "UNCHECKED_CAST",
            "request: Unit",
        )
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
            message Query { required int64 id = 1; }
            message Reply { optional string value = 1; }
            """
        )
        assertContainsAll(
            text,
            "public suspend fun find(request: Query): Answer<Reply>",
            "public suspend fun broadcast(request: Query)\n",
            "public suspend fun single(request: Query): Reply",
            "public suspend fun inherit(request: Query): Answer<Reply>",
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
              option (surf.nats.v1.service) = { persistent: true call_timeout_seconds: 30 };
              rpc Place(Order) returns (google.protobuf.Empty);
              rpc Ping(google.protobuf.Empty) returns (Order) { option (surf.nats.v1.method) = { persistent: false }; }
              rpc Total(google.protobuf.Empty) returns (Order) { option (surf.nats.v1.method) = { call_timeout_seconds: 0 }; }
              rpc Slow(google.protobuf.Empty) returns (Order) { option (surf.nats.v1.method) = { call_timeout_seconds: 300 }; }
            }
            service PlainService { rpc Get(google.protobuf.Empty) returns (Order); }
            message Order { required int64 id = 1; }
            """
        )
        assertContainsAll(
            text,
            "protoName = \"surf.test.v1.OrderService\",\n                    methods = listOf(",
            "protoName = \"Place\",\n                            kotlinName = \"place\",\n" +
                    "                            persistent = true,\n                            callTimeoutSeconds = 30,",
            "protoName = \"Ping\",\n                            kotlinName = \"ping\",\n" +
                    "                            persistent = false,\n                            callTimeoutSeconds = 30,",
            "protoName = \"Total\",\n                            kotlinName = \"total\",\n" +
                    "                            persistent = true,\n                            callTimeoutSeconds = 30,",
            "protoName = \"Slow\",\n                            kotlinName = \"slow\",\n" +
                    "                            persistent = true,\n                            callTimeoutSeconds = 300,",
            "protoName = \"surf.test.v1.PlainService\",",
            "protoName = \"Get\",\n                            kotlinName = \"get\",\n" +
                    "                            persistent = false,\n                            callTimeoutSeconds = null,\n" +
                    "                            recipients = Recipients.ANY_INSTANCE,",
        )
    }

    @Test
    fun `service descriptors carry the proto full name and the exact rpc names`() {
        val text = contracts(
            """
            service Roles_Api {
              rpc lookup_Role(google.protobuf.Empty) returns (google.protobuf.Empty);
            }
            """,
            protoPackage = "surf.roles.v2",
        )
        assertContainsAll(
            text,
            "NatsServiceDescriptor<Roles_Api>(\n                    protoName = \"surf.roles.v2.Roles_Api\",\n" +
                    "                    methods = listOf(",
            "protoName = \"lookup_Role\",\n                            kotlinName = \"lookupRole\",",
            "public suspend fun lookupRole()\n",
        )
        assertNotContains(text, "global =", "majorVersion =", " name =")
    }

    @Test
    fun `a persistent fire at exactly the duplicate window is accepted`() {
        val text = contracts(
            """
            service EdgeService {
              rpc Store(Item) returns (google.protobuf.Empty) { option (surf.nats.v1.method) = { persistent: true call_timeout_seconds: 120 }; }
            }
            message Item { required int64 id = 1; }
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
            message Item { required int64 id = 1; }
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
                    "message Item { required int64 id = 1; }",
        ).errors()
        assertError(errors, "contracts.proto:5:", "invalid option value \"010\" for uint32")
    }
}

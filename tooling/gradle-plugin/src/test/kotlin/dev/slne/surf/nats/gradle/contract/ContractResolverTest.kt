package dev.slne.surf.nats.gradle.contract

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.wire.schema.ProtoType
import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.OPTIONS_IMPORT
import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.PROTO2
import dev.slne.surf.nats.gradle.generator.TypeMapping
import dev.slne.surf.nats.gradle.model.IntEncoding
import dev.slne.surf.nats.gradle.model.KotlinTypes
import dev.slne.surf.nats.protocol.RecipientsOption
import dev.slne.surf.nats.protocol.ResultKindOption
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What [ContractResolver] decides for valid declarations, read from the contract instead of generated text.
 */
class ContractResolverTest {
    @TempDir
    lateinit var temp: File

    private fun contract(body: String, mappings: List<TypeMapping> = emptyList()): ContractHarness {
        return ContractHarness(temp, mappings).proto(
            "surf/test/v1/contract.proto",
            """
            |$PROTO2
            |package surf.test.v1;
            |$OPTIONS_IMPORT
            |import "google/protobuf/empty.proto";
            |$body
            """.trimMargin(),
        )
    }

    private fun generated(vararg simpleNames: String) = ClassName(PACKAGE, *simpleNames)

    private fun ContractHarness.member(message: String, name: String): MessageMember =
        members("$PACKAGE.$message").single { it.name == name }

    private fun ContractHarness.field(message: String, name: String): FieldMember =
        member(message, name) as FieldMember

    private fun assertProperty(member: MessageMember, type: TypeName, default: String?) {
        assertEquals(type, member.type, "type of ${member.name}")
        assertEquals(default, member.default?.toString(), "default of ${member.name}")
    }

    @Test
    fun `proto2 labels decide nullability and defaults`() {
        val level = ClassName("dev.example", "Level")
        val levelSerializer = ClassName("dev.example", "LevelSerializer")
        val contract = contract(
            """
            enum Color { COLOR_UNSPECIFIED = 0; COLOR_RED = 1; }
            enum Level { LEVEL_UNSPECIFIED = 0; LEVEL_HIGH = 1; }
            message Part {}
            message Labels {
              required int32 required_int = 1;
              optional int32 optional_int = 2;
              optional int32 hex_default = 3 [default = 0x10];
              optional Color color = 4;
              optional Level level = 5;
              optional Part optional_part = 6;
              required Part required_part = 7;
            }
            """,
            mappings = listOf(
                TypeMapping(ProtoType.get("$PACKAGE.Level"), level.canonicalName, levelSerializer.canonicalName),
            ),
        )
        val mappedLevel = level.copy(annotations = listOf(KotlinTypes.serializableWith(levelSerializer)))
        val part = generated("Part")

        assertProperty(contract.member("Labels", "requiredInt"), INT, default = null)
        assertProperty(contract.member("Labels", "optionalInt"), INT.copy(nullable = true), default = "null")
        assertProperty(contract.member("Labels", "hexDefault"), INT, default = "16")
        assertProperty(contract.member("Labels", "color"), generated("Color"), default = "$PACKAGE.Color.UNSPECIFIED")
        assertProperty(contract.member("Labels", "level"), mappedLevel.copy(nullable = true), default = "null")
        assertProperty(contract.member("Labels", "optionalPart"), part.copy(nullable = true), default = "null")
        assertProperty(contract.member("Labels", "requiredPart"), part, default = null)
    }

    @Test
    fun `repeated and map fields are collections with empty defaults`() {
        val contract = contract(
            """
            message Collections {
              repeated sint64 deltas = 1;
              repeated int32 packed_numbers = 2 [packed = true];
              map<string, sint64> balances = 3;
              map<fixed32, string> labels = 4;
              map<string, int32> counts = 5;
            }
            """
        )

        val deltas = contract.field("Collections", "deltas")
        assertProperty(deltas, LIST.parameterizedBy(LONG), default = "emptyList()")
        assertEquals(IntEncoding.SIGNED, deltas.encoding)
        assertFalse(deltas.packed)

        assertTrue(contract.field("Collections", "packedNumbers").packed)

        val balances = contract.field("Collections", "balances")
        assertProperty(balances, MAP.parameterizedBy(STRING, LONG), default = "emptyMap()")
        assertEquals(IntEncoding.SIGNED, balances.encoding)

        assertEquals(IntEncoding.FIXED, contract.field("Collections", "labels").encoding)
        assertNull(contract.field("Collections", "counts").encoding)
    }

    @Test
    fun `equality follows the element type`() {
        val contract = contract(
            """
            message Equality {
              required bytes blob = 1;
              repeated bytes chunks = 2;
              map<string, bytes> parts = 3;
              required float ratio = 4;
              optional double score = 5;
              required int32 count = 6;
            }
            """
        )

        assertEquals(
            mapOf(
                "blob" to MemberComparison.CONTENT,
                "chunks" to MemberComparison.CONTENT,
                "parts" to MemberComparison.CONTENT,
                "ratio" to MemberComparison.BITS,
                "score" to MemberComparison.NULLABLE_BITS,
                "count" to MemberComparison.EQUALS,
            ),
            contract.members("$PACKAGE.Equality").associate { it.name to it.comparison },
        )
    }

    @Test
    fun `members follow field numbers with a oneof at its lowest case number`() {
        val contract = contract(MESSAGE_WITH_ONEOF)

        assertEquals(listOf("choice", "a", "z"), contract.members("$PACKAGE.A").map { it.name })
    }

    @Test
    fun `a oneof is a nullable interface property with its cases by number`() {
        val choice = contract(MESSAGE_WITH_ONEOF).member("A", "choice") as OneofMember

        assertEquals(generated("A", "Choice"), choice.interfaceName)
        assertProperty(choice, generated("A", "Choice").copy(nullable = true), default = "null")
        assertEquals(
            listOf(generated("A", "Choice", "Number"), generated("A", "Choice", "Text")),
            choice.cases.map { it.className },
        )
        assertEquals(listOf("value", "value"), choice.cases.map { it.value.name })
        assertEquals(listOf<TypeName>(INT, STRING), choice.cases.map { it.value.type })
    }

    @Test
    fun `enum entries drop the value prefix and keep declaration order`() {
        val contract = contract(
            "enum ResultKind { RESULT_KIND_UNSPECIFIED = 0; RESULT_KIND_VALUE = 2; RESULT_KIND_ANSWER = 1; }"
        )

        assertEquals(
            listOf("UNSPECIFIED" to 0, "VALUE" to 2, "ANSWER" to 1),
            contract.entries("$PACKAGE.ResultKind").map { (name, constant) -> name to constant.tag },
        )
    }

    @Test
    fun `methods carry Kotlin names, payloads and options resolved against the service`() {
        val contract = contract(
            """
            service FinderService {
              option (surf.nats.v1.service) = { recipients: NATS_RECIPIENTS_ALL_INSTANCES };
              rpc Find(Query) returns (Reply);
              rpc Broadcast(Query) returns (google.protobuf.Empty);
              rpc Single(Query) returns (Reply) { option (surf.nats.v1.method) = { recipients: NATS_RECIPIENTS_ANY_INSTANCE }; }
              rpc Inherit(Query) returns (Reply) { option (surf.nats.v1.method) = { recipients: NATS_RECIPIENTS_UNSPECIFIED }; }
            }
            service OrderService {
              option (surf.nats.v1.service) = { persistent: true call_timeout_seconds: 30 };
              rpc Place(Query) returns (google.protobuf.Empty);
              rpc Ping(google.protobuf.Empty) returns (Reply) { option (surf.nats.v1.method) = { persistent: false }; }
              rpc Total(google.protobuf.Empty) returns (Reply) { option (surf.nats.v1.method) = { call_timeout_seconds: 0 }; }
              rpc Slow(google.protobuf.Empty) returns (Reply) { option (surf.nats.v1.method) = { call_timeout_seconds: 300 }; }
            }
            service PlainService { rpc Get(google.protobuf.Empty) returns (Reply); }
            message Query { required int64 id = 1; }
            message Reply { optional string value = 1; }
            """
        )
        val query = generated("Query")

        val finder = contract.methods("$PACKAGE.FinderService")
        assertEquals(listOf("find", "broadcast", "single", "inherit"), finder.map { it.kotlinName })
        assertEquals(listOf(query, query, query, query), finder.map { it.request.kotlinType })
        assertEquals(
            listOf(
                RecipientsOption.ALL_INSTANCES,
                RecipientsOption.ALL_INSTANCES,
                RecipientsOption.ANY_INSTANCE,
                RecipientsOption.ALL_INSTANCES,
            ),
            finder.map { it.resolution.recipients },
        )
        assertEquals(
            listOf(ResultKindOption.ANSWER, ResultKindOption.UNIT, ResultKindOption.VALUE, ResultKindOption.ANSWER),
            finder.map { it.resolution.resultKind },
        )

        val orders = contract.methods("$PACKAGE.OrderService")
        assertEquals(listOf(query, UNIT, UNIT, UNIT), orders.map { it.request.kotlinType })
        assertEquals(listOf(true, false, true, true), orders.map { it.resolution.persistent })
        assertEquals(listOf<Int?>(30, 30, 30, 300), orders.map { it.resolution.callTimeoutSeconds })

        val plain = contract.methods("$PACKAGE.PlainService").single().resolution
        assertFalse(plain.persistent)
        assertNull(plain.callTimeoutSeconds)
        assertEquals(RecipientsOption.ANY_INSTANCE, plain.recipients)
    }

    private companion object {
        const val PACKAGE = "surf.test.v1"

        const val MESSAGE_WITH_ONEOF = """
            message A {
              required int32 z = 5;
              oneof choice { string text = 4; int32 number = 2; }
              required int32 a = 3;
            }
            """
    }
}

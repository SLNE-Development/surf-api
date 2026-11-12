package dev.slne.surf.nats.gradle.generator

import com.squareup.wire.schema.ProtoType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

/**
 * The order in which a run reports its problems, with locations relative to the source directory.
 */
class ProblemOrderTest {
    @TempDir
    lateinit var temp: File

    private fun problems(harness: GeneratorHarness, input: GeneratorInput = harness.input()): List<String> {
        val root = harness.sourceDir.absoluteFile.normalize().invariantSeparatorsPath
        return harness.errors(input).map { it.replace("$root/", "") }
    }

    @Test
    fun `problems of a message follow member order, then clashes, event rules and nested types`() {
        val harness = GeneratorHarness(temp).proto(
            "surf/test/v1/order.proto",
            """
            syntax = "proto2";
            package surf.test.v1;
            import "surf/nats/v1/options.proto";
            message Order {
              option (surf.nats.v1.event) = true;
              message Inner { repeated sint64 values = 1 [packed = true]; }
              message Choice {}
              optional string fooBar = 8;
              optional string foo_bar = 7;
              optional int32 nats_descriptor = 6;
              oneof choice {
                int32 limit = 5 [default = 1];
                int32 companion = 4;
              }
              required int32 with_default = 3 [default = 1];
              repeated sint32 list = 2;
              map<sint64, int32> counts = 1;
            }
            """,
        )

        assertEquals(
            listOf(
                "surf/test/v1/order.proto:17:3: map<sint64, int32> 'counts' cannot be encoded by " +
                        "kotlinx-serialization-protobuf: key and value need the same integer encoding (SIGNED vs DEFAULT)",
                "surf/test/v1/order.proto:16:3: sint32 is not supported (kotlinx-serialization-protobuf cannot decode " +
                        "values >= 2^30); use sint64 or int32",
                "surf/test/v1/order.proto:15:3: required field 'with_default' cannot have a [default]; the encoder omits " +
                        "values equal to the default, so readers would reject the message (declare it optional)",
                "surf/test/v1/order.proto:13:5: oneof field 'companion' clashes with the generated companion object",
                "surf/test/v1/order.proto:12:5: [default] is not allowed on oneof field 'limit'",
                "surf/test/v1/order.proto:8:3: 'fooBar' maps to the Kotlin property 'fooBar', which 'foo_bar' " +
                        "(surf/test/v1/order.proto:9:3) already uses",
                "surf/test/v1/order.proto:10:3: event surf.test.v1.Order must not have a field named natsDescriptor",
                "surf/test/v1/order.proto:6:19: [packed = true] is not supported for repeated sint64 'values' " +
                        "(kotlinx-serialization-protobuf ignores the integer encoding of packed lists)",
                "surf/test/v1/order.proto:7:3: nested type Choice clashes with the oneof declared at " +
                        "surf/test/v1/order.proto:11:3",
            ),
            problems(harness),
        )
    }

    @Test
    fun `an event reports a natsDescriptor property at its first owner, then its name`() {
        val harness = GeneratorHarness(temp).proto(
            "surf/test/v1/event.proto",
            """
            syntax = "proto2";
            package surf.test.v1;
            import "surf/nats/v1/options.proto";
            message _Order {
              option (surf.nats.v1.event) = true;
              optional int32 natsDescriptor = 2;
              optional int32 nats_descriptor = 1;
            }
            """,
        )

        assertEquals(
            listOf(
                "surf/test/v1/event.proto:6:3: 'natsDescriptor' maps to the Kotlin property 'natsDescriptor', which " +
                        "'nats_descriptor' (surf/test/v1/event.proto:7:3) already uses",
                "surf/test/v1/event.proto:7:3: event surf.test.v1._Order must not have a field named natsDescriptor",
                "surf/test/v1/event.proto:4:1: event surf.test.v1._Order: every name component must start with an " +
                        "ASCII letter and contain only ASCII letters, digits and '_'",
            ),
            problems(harness),
        )
    }

    @Test
    fun `a nested type clashes with the last oneof of its name in member order`() {
        val harness = GeneratorHarness(temp).proto(
            "surf/test/v1/oneofs.proto",
            """
            syntax = "proto2";
            package surf.test.v1;
            message A {
              oneof fooBar { string second = 2; }
              oneof foo_bar { string first = 1; }
              message FooBar {}
            }
            """,
        )

        assertEquals(
            listOf(
                "surf/test/v1/oneofs.proto:4:3: 'fooBar' maps to the Kotlin property 'fooBar', which 'foo_bar' " +
                        "(surf/test/v1/oneofs.proto:5:3) already uses",
                "surf/test/v1/oneofs.proto:6:3: nested type FooBar clashes with the oneof declared at " +
                        "surf/test/v1/oneofs.proto:4:3",
            ),
            problems(harness),
        )
    }

    @Test
    fun `the name of a mapped nested type is checked as well`() {
        val harness = GeneratorHarness(temp).proto(
            "surf/test/v1/mapped.proto",
            """
            syntax = "proto2";
            package surf.test.v1;
            message A {
              oneof tag { string first = 1; }
              message Tag { repeated sint32 unsupported = 1; }
            }
            """,
        )
        val nestedMapping =
            TypeMapping(ProtoType.get("surf.test.v1.A.Tag"), "dev.example.Tag", "dev.example.TagSerializer")

        assertEquals(
            listOf(
                "surf/test/v1/mapped.proto:5:3: nested type Tag clashes with the oneof declared at " +
                        "surf/test/v1/mapped.proto:4:3",
            ),
            problems(harness, harness.input(mappings = listOf(nestedMapping))),
        )
    }

    @Test
    fun `problems of an enum follow its rules, then its values`() {
        val harness = GeneratorHarness(temp).proto(
            "surf/test/v1/status.proto",
            """
            syntax = "proto2";
            package surf.test.v1;
            enum HTTPStatus {
              option allow_alias = true;
              HTTP_STATUS_OK = 1;
              HTTP_STATUS_FINE = 1;
              HTTP_STATUS_NEG = -1;
              GOLD = 2;
              HTTP_STATUS_2 = 3;
              HTTP_STATUS_name = 4;
            }
            """,
        )

        assertEquals(
            listOf(
                "surf/test/v1/status.proto:3:1: enum HTTPStatus: protobuf-es derives the value prefix H_T_T_P_STATUS_ " +
                        "from the enum name, not HTTP_STATUS_, so TypeScript would keep the full value names; rename the " +
                        "enum to HttpStatus (abbreviations as one word, no '_')",
                "surf/test/v1/status.proto:3:1: enum HTTPStatus: allow_alias is not supported",
                "surf/test/v1/status.proto:3:1: enum HTTPStatus must declare HTTP_STATUS_UNSPECIFIED or " +
                        "HTTP_STATUS_UNKNOWN = 0 as its first value",
                "surf/test/v1/status.proto:7:3: enum value HTTP_STATUS_NEG has a negative number -1",
                "surf/test/v1/status.proto:8:3: enum value GOLD must start with the prefix HTTP_STATUS_",
                "surf/test/v1/status.proto:9:3: enum value HTTP_STATUS_2 without its prefix (2) must start with a letter " +
                        "and contain only letters, digits and '_'",
                "surf/test/v1/status.proto:10:3: enum value HTTP_STATUS_name maps to the reserved Kotlin entry name name",
            ),
            problems(harness),
        )
    }

    @Test
    fun `problems of a service follow its options, its rules, then its rpcs`() {
        val harness = GeneratorHarness(temp).proto(
            "surf/test/v1/orders.proto",
            """
            syntax = "proto2";
            package surf.test.v1;
            import "surf/nats/v1/options.proto";
            import "google/protobuf/empty.proto";
            import "google/protobuf/timestamp.proto";
            message Count {}
            service Orders {
              option (surf.nats.v1.service).call_timeout_seconds = 4294967295;
              rpc Count(google.protobuf.Empty) returns (Count);
              rpc Watch(stream google.protobuf.Empty) returns (google.protobuf.Empty);
              rpc GetOrder(google.protobuf.Empty) returns (google.protobuf.Empty);
              rpc get_order(google.protobuf.Timestamp) returns (google.protobuf.Empty) { option (surf.nats.v1.method).call_timeout_seconds = 4294967295; }
              rpc ToString(google.protobuf.Empty) returns (google.protobuf.Empty) { option (surf.nats.v1.method) = { persistent: true recipients: NATS_RECIPIENTS_ALL_INSTANCES }; }
            }
            """,
        )

        assertEquals(
            listOf(
                "surf/test/v1/orders.proto:7:1: option NatsServiceOptions.call_timeout_seconds must be between 0 and " +
                        "2147483647, was '4294967295'",
                "surf/test/v1/orders.proto:9:3: rpc Count: type 'Count' conflicts with RPC 'Count' in the same service. " +
                        "Use a package-qualified type name.",
                "surf/test/v1/orders.proto:10:3: rpc Watch: streaming rpcs are not supported",
                "surf/test/v1/orders.proto:12:3: option NatsMethodOptions.call_timeout_seconds must be between 0 and " +
                        "2147483647, was '4294967295'",
                "surf/test/v1/orders.proto:12:3: rpc get_order maps to the Kotlin function 'getOrder', already used by " +
                        "the rpc at surf/test/v1/orders.proto:11:3",
                "surf/test/v1/orders.proto:12:3: rpc get_order: request type google.protobuf.Timestamp is mapped to " +
                        "java.time.Instant; only google.protobuf.Empty or generated messages can be requests and responses",
                "surf/test/v1/orders.proto:13:3: rpc ToString: persistent methods cannot address ALL_INSTANCES",
                "surf/test/v1/orders.proto:13:3: rpc ToString maps to the Kotlin function 'toString', which clashes " +
                        "with a member of Any",
            ),
            problems(harness),
        )
    }

    @Test
    fun `a run reports mappings, file rules and clashes of all files, then each file's types before its services`() {
        val harness = GeneratorHarness(temp)
            .proto(
                "a/v1/contract.proto",
                """
                syntax = "proto2";
                package a.v1;
                import "google/protobuf/empty.proto";
                message A {
                  required sint32 x = 1;
                }
                service AService {
                  rpc Watch(stream google.protobuf.Empty) returns (google.protobuf.Empty);
                }
                """,
            )
            .proto(
                "b/v1/contract.proto",
                """
                syntax = "proto2";
                package b.v1;
                option java_package = "a.v1";
                import "google/protobuf/empty.proto";
                message B {
                  required sint32 y = 1;
                }
                service BService {
                  rpc Watch(stream google.protobuf.Empty) returns (google.protobuf.Empty);
                }
                """,
            )
        val unqualifiedMapping = TypeMapping(ProtoType.get("x.v1.Tag"), "tag", "dev.example.TagSerializer")

        assertEquals(
            listOf(
                "mapType(x.v1.Tag): 'tag' is not a fully qualified Kotlin class name",
                "b/v1/contract.proto: proto package 'b.v1' resolves to the Kotlin package 'a.v1', which proto package " +
                        "'a.v1' (a/v1/contract.proto) already uses; give each proto package its own java_package",
                "b/v1/contract.proto: generates Contract.kt in package 'a.v1', which a/v1/contract.proto already generates",
                "a/v1/contract.proto:5:3: sint32 is not supported (kotlinx-serialization-protobuf cannot decode values " +
                        ">= 2^30); use sint64 or int32",
                "a/v1/contract.proto:8:3: rpc Watch: streaming rpcs are not supported",
                "b/v1/contract.proto:6:3: sint32 is not supported (kotlinx-serialization-protobuf cannot decode values " +
                        ">= 2^30); use sint64 or int32",
                "b/v1/contract.proto:9:3: rpc Watch: streaming rpcs are not supported",
            ),
            problems(harness, harness.input(mappings = listOf(unqualifiedMapping))),
        )
    }
}

package dev.slne.surf.nats.gradle.generator

import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.OPTIONS_IMPORT
import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.PROTO2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * One failing proto per generator rule; every error starts with `file:line:col:`.
 */
class ValidationTest {
    @TempDir
    lateinit var temp: File

    private fun harness() = GeneratorHarness(temp)

    private fun errors(body: String, protoPackage: String = "surf.test.v1", header: String = "$PROTO2\npackage $protoPackage;"): List<String> {
        return harness().proto(
            "surf/test/v1/bad.proto",
            """
            |$header
            |$OPTIONS_IMPORT import "google/protobuf/empty.proto";
            |$body
            """.trimMargin(),
        ).errors()
    }

    private fun assertLocated(errors: List<String>, line: Int, vararg fragments: String) {
        assertError(errors, "surf/test/v1/bad.proto:$line:", *fragments)
    }

    // File level

    @Test
    fun `proto3 is rejected`() {
        assertError(
            errors("message A { int32 a = 1; }", header = "syntax = \"proto3\";\npackage surf.test.v1;"),
            "bad.proto", "only syntax = \"proto2\" is supported",
        )
    }

    @Test
    fun `editions are rejected`() {
        assertError(errors("message A {}", header = "edition = \"2023\";\npackage surf.test.v1;"), "bad.proto:1:", "syntax error", "edition")
    }

    @Test
    fun `events and services need a versioned package`() {
        assertError(
            errors("message E { option (surf.nats.v1.event) = true; }", protoPackage = "surf.test"),
            "bad.proto", "package 'surf.test' declares events or services and must end with a major version '.v<N>'",
        )
        assertError(
            errors("service S { rpc A(google.protobuf.Empty) returns (google.protobuf.Empty); }", protoPackage = "surf.test.v01"),
            "package 'surf.test.v01' declares events or services",
        )
    }

    @Test
    fun `the Kotlin package of a versioned proto package must end with the version`() {
        assertError(
            errors("message A {}", header = "$PROTO2\npackage surf.test.v1;\noption java_package = \"dev.example.proto\";"),
            "Kotlin package 'dev.example.proto' of proto package 'surf.test.v1' must end with '.v1'",
        )
    }

    @Test
    fun `two proto packages must not share a Kotlin package`() {
        val errors = harness()
            .proto("a/v1/a.proto", "$PROTO2\npackage a.v1;\noption java_package = \"dev.example.v1\";\nmessage A {}")
            .proto("b/v1/b.proto", "$PROTO2\npackage b.v1;\noption java_package = \"dev.example.v1\";\nmessage B {}")
            .errors()
        assertError(errors, "b/v1/b.proto", "proto package 'b.v1' resolves to the Kotlin package 'dev.example.v1', which proto package 'a.v1'")
    }

    @Test
    fun `two files must not generate the same Kotlin file`() {
        val errors = harness()
            .proto("a/types.proto", "$PROTO2\npackage surf.test.v1;\nmessage A {}")
            .proto("b/types.proto", "$PROTO2\npackage surf.test.v1;\nmessage B {}")
            .errors()
        assertError(errors, "b/types.proto", "generates Types.kt in package 'surf.test.v1'", "a/types.proto")
    }

    @Test
    fun `a message and a service must not share a Kotlin class name`() {
        val errors = errors("message Calc {}\nservice Calc { rpc A(google.protobuf.Empty) returns (google.protobuf.Empty); }")
        assertTrue(errors.any { "Calc" in it }, errors.joinToString("\n"))
    }

    @Test
    fun `surf options require a direct import of options proto`() {
        val errors = harness()
            .proto("surf/test/v1/base.proto", "$PROTO2\npackage surf.test.v1;\n$OPTIONS_IMPORT\nmessage Base {}")
            .proto(
                "surf/test/v1/bad.proto",
                "$PROTO2\npackage surf.test.v1;\nimport \"surf/test/v1/base.proto\";\nmessage E { option (surf.nats.v1.event) = true; }",
            )
            .errors()
        assertError(errors, "surf/test/v1/bad.proto:4:1: surf/test/v1/bad.proto needs to import surf/nats/v1/options.proto")
        assertFalse(errors.any { "surf-nats-protos" in it }, errors.joinToString("\n"))
    }

    @Test
    fun `extensions of messages are rejected`() {
        assertLocated(
            errors("message A { extensions 100 to 200; }\nextend A { optional int32 b = 100; }"),
            5, "extensions of surf.test.v1.A are not supported",
        )
    }

    @Test
    fun `wire link errors are reported with their location`() {
        assertLocated(errors("message A { optional Missing m = 1; }"), 4, "unable to resolve Missing", "in message surf.test.v1.A")
    }

    @Test
    fun `syntax errors are reported with their location`() {
        assertLocated(errors("message A { optional int32 a = 1 }"), 4, "syntax error")
    }

    // Messages

    @Test
    fun `required fields are rejected`() {
        assertLocated(errors("message A {\n  required int32 a = 1;\n}"), 5, "required field 'a' is not supported")
    }

    @Test
    fun `sint32 is rejected`() {
        assertLocated(errors("message A {\n  optional sint32 a = 1;\n}"), 5, "sint32 is not supported")
    }

    @Test
    fun `Empty is rejected as a field type`() {
        assertLocated(errors("message A {\n  optional google.protobuf.Empty e = 1;\n}"), 5, "google.protobuf.Empty can only be an rpc request or response")
    }

    @Test
    fun `nullable is rejected on repeated, map and oneof fields`() {
        val errors = errors(
            """
            message A {
              repeated int32 a = 1 [(surf.nats.v1.nullable) = true];
              map<string, int32> b = 2 [(surf.nats.v1.nullable) = true];
              oneof c { string d = 3 [(surf.nats.v1.nullable) = true]; }
            }
            """.trimIndent()
        )
        assertLocated(errors, 5, "(surf.nats.v1.nullable) is not allowed on repeated field 'a'")
        assertLocated(errors, 6, "(surf.nats.v1.nullable) is not allowed on map field 'b'")
        assertLocated(errors, 7, "(surf.nats.v1.nullable) is not allowed on oneof field 'd'")
    }

    @Test
    fun `nullable fields must not have a default`() {
        assertLocated(
            errors("message A {\n  optional int32 a = 1 [(surf.nats.v1.nullable) = true, default = 5];\n}"),
            5, "'a' cannot be (surf.nats.v1.nullable) and have a [default]",
        )
    }

    @Test
    fun `enum defaults must be the zero value`() {
        assertLocated(
            errors("enum Color { COLOR_UNSPECIFIED = 0; COLOR_RED = 1; }\nmessage A {\n  optional Color c = 1 [default = COLOR_RED];\n}"),
            6, "enum field 'c' may only default to its zero value COLOR_UNSPECIFIED, was COLOR_RED",
        )
    }

    @Test
    fun `defaults are rejected for message fields by wire-schema`() {
        val errors = errors("import \"google/protobuf/timestamp.proto\";\nmessage A {\n  optional google.protobuf.Timestamp t = 1 [default = 5];\n}")
        assertLocated(errors, 6, "invalid default value \"5\" for google.protobuf.Timestamp")
    }

    @Test
    fun `defaults are rejected for oneof fields`() {
        assertLocated(errors("message A {\n  oneof o { int32 x = 2 [default = 1]; }\n}"), 5, "[default] is not allowed on oneof field 'x'")
    }

    @Test
    fun `packed signed and fixed lists are rejected`() {
        val errors = errors(
            """
            message A {
              repeated sint64 a = 1 [packed = true];
              repeated fixed32 b = 2 [packed = true];
              repeated sfixed64 c = 3 [packed = true];
            }
            """.trimIndent()
        )
        assertLocated(errors, 5, "[packed = true] is not supported for repeated sint64 'a'")
        assertLocated(errors, 6, "[packed = true] is not supported for repeated fixed32 'b'")
        assertLocated(errors, 7, "[packed = true] is not supported for repeated sfixed64 'c'")
    }

    @Test
    fun `maps with different key and value integer encodings are rejected`() {
        val errors = errors(
            """
            enum Color { COLOR_UNSPECIFIED = 0; }
            message A {
              map<sint64, int32> a = 1;
              map<fixed32, bool> b = 2;
              map<sint64, Color> c = 3;
              map<int32, fixed64> d = 4;
            }
            """.trimIndent()
        )
        assertLocated(errors, 6, "map<sint64, int32> 'a' cannot be encoded", "(SIGNED vs DEFAULT)")
        assertLocated(errors, 7, "map<fixed32, bool> 'b'", "(FIXED vs DEFAULT)")
        assertLocated(errors, 8, "map<sint64, surf.test.v1.Color> 'c'")
        assertLocated(errors, 9, "map<int32, fixed64> 'd'")
    }

    @Test
    fun `types that are neither generated nor mapped are rejected`() {
        val errors = errors("import \"google/protobuf/any.proto\";\nmessage A {\n  optional google.protobuf.Any a = 1;\n}")
        assertLocated(
            errors, 6,
            "google.protobuf.Any from google/protobuf/any.proto is neither generated nor mapped; add mapType(\"google.protobuf.Any\", ...)",
        )
    }

    @Test
    fun `paper types are unmapped outside paper projects`() {
        val errors = errors("import \"surf/types/paper/v1/location.proto\";\nmessage A {\n  optional surf.types.paper.v1.Location at = 1;\n}")
        assertLocated(errors, 6, "surf.types.paper.v1.Location from surf/types/paper/v1/location.proto is neither generated nor mapped")
    }

    @Test
    fun `lowerCamel property names must be unique`() {
        assertLocated(
            errors("message A {\n  optional int32 foo_bar = 1;\n  optional int32 fooBar = 2;\n}"),
            6, "'fooBar' maps to the Kotlin property 'fooBar', which 'foo_bar'",
        )
    }

    @Test
    fun `oneof names must not clash with nested types or their cases`() {
        val errors = errors(
            """
            message A {
              oneof target { string a = 1; }
              message Target {}
            }
            message B {
              oneof value { string value = 1; }
            }
            """.trimIndent()
        )
        assertLocated(errors, 6, "nested type Target clashes with the oneof declared at")
        assertLocated(errors, 9, "oneof field 'value' has the same Kotlin name as its oneof 'value'")
    }

    @Test
    fun `oneof case classes must be unique and must not clash with companions`() {
        val errors = errors(
            """
            message A {
              oneof choice { string foo_bar = 1; int32 fooBar = 2; int32 companion = 3; }
              oneof companion { string x = 4; }
            }
            message Choice {
              oneof choice { string y = 1; }
            }
            """.trimIndent()
        )
        assertLocated(errors, 5, "oneof field 'fooBar' maps to the Kotlin class FooBar, which 'foo_bar'")
        assertLocated(errors, 5, "oneof field 'companion' clashes with the generated companion object")
        assertLocated(errors, 6, "oneof 'companion' clashes with the generated companion object")
        assertLocated(errors, 9, "oneof 'choice' must not have the name of its enclosing message")
    }

    @Test
    fun `nested types must not be named Companion or like their enclosing type`() {
        val errors = errors("message A {\n  message Companion {}\n  message A {}\n}")
        assertLocated(errors, 5, "nested type name Companion clashes with the generated companion object")
        assertLocated(errors, 6, "nested type A must not have the name of its enclosing type")
    }

    // Enums

    @Test
    fun `enum aliases are rejected`() {
        assertLocated(
            errors("enum Color {\n  option allow_alias = true;\n  COLOR_UNSPECIFIED = 0;\n  COLOR_RED = 1;\n  COLOR_ROT = 1;\n}"),
            4, "enum Color: allow_alias is not supported",
        )
    }

    @Test
    fun `enums must start with a prefixed zero value`() {
        assertLocated(errors("enum Color {\n  COLOR_RED = 1;\n  COLOR_UNSPECIFIED = 0;\n}"), 4, "enum Color must declare COLOR_UNSPECIFIED or COLOR_UNKNOWN = 0 as its first value")
        assertLocated(errors("enum Color {\n  UNSPECIFIED = 0;\n}"), 4, "enum Color must declare COLOR_UNSPECIFIED")
    }

    @Test
    fun `every enum value needs the prefix and a valid remainder`() {
        val errors = errors("enum DeviceTier {\n  DEVICE_TIER_UNKNOWN = 0;\n  GOLD = 1;\n  DEVICE_TIER_1 = 2;\n}")
        assertLocated(errors, 6, "enum value GOLD must start with the prefix DEVICE_TIER_")
        assertLocated(errors, 7, "enum value DEVICE_TIER_1 without its prefix (1) must start with a letter")
    }

    @Test
    fun `enum value remainders must not clash with Kotlin enum members`() {
        val errors = errors(
            "enum Weird {\n  WEIRD_UNSPECIFIED = 0;\n  WEIRD_name = 1;\n  WEIRD_ordinal = 2;\n  WEIRD_entries = 3;\n" +
                    "  WEIRD_Serializer = 4;\n  WEIRD_number = 5;\n}"
        )
        assertLocated(errors, 6, "enum value WEIRD_name maps to the reserved Kotlin entry name name")
        assertLocated(errors, 7, "enum value WEIRD_ordinal maps to the reserved Kotlin entry name ordinal")
        assertLocated(errors, 8, "enum value WEIRD_entries maps to the reserved Kotlin entry name entries")
        assertLocated(errors, 9, "enum value WEIRD_Serializer maps to the reserved Kotlin entry name Serializer")
        assertLocated(errors, 10, "enum value WEIRD_number maps to the reserved Kotlin entry name number")
    }

    @Test
    fun `enum names must give protobuf-es the same value prefix`() {
        val errors = errors(
            "enum HTTPStatus {\n  HTTP_STATUS_UNSPECIFIED = 0;\n  HTTP_STATUS_OK = 1;\n}\n" +
                    "enum Player_State {\n  PLAYER_STATE_UNSPECIFIED = 0;\n}\nenum PlayerID {\n  PLAYER_ID_UNSPECIFIED = 0;\n}"
        )
        assertLocated(
            errors, 4,
            "enum HTTPStatus: protobuf-es derives the value prefix H_T_T_P_STATUS_ from the enum name, not HTTP_STATUS_",
            "rename the enum to HttpStatus",
        )
        assertLocated(errors, 8, "enum Player_State: protobuf-es derives the value prefix PLAYER__STATE_", "rename the enum to PlayerState")
        assertLocated(errors, 11, "enum PlayerID: protobuf-es derives the value prefix PLAYER_I_D_", "rename the enum to PlayerId")
        assertEquals(3, errors.size, errors.joinToString("\n"))
    }

    @Test
    fun `negative enum numbers are rejected`() {
        assertLocated(errors("enum Color {\n  COLOR_UNSPECIFIED = 0;\n  COLOR_NEG = -1;\n}"), 6, "enum value COLOR_NEG has a negative number -1")
    }

    // Events

    @Test
    fun `the event option is a bool`() {
        val errors = errors("message A { option (surf.nats.v1.event) = { subject: \"a\" }; }")
        assertError(errors, "bad.proto:4:", "unable to resolve option subject on bool")
    }

    @Test
    fun `event names must start with a letter`() {
        assertLocated(
            errors("message _Hidden { option (surf.nats.v1.event) = true; }"),
            4, "event surf.test.v1._Hidden: every name component must start with an ASCII letter",
        )
    }

    @Test
    fun `event names must not contain a dot`() {
        assertLocated(
            errors("message E.F { option (surf.nats.v1.event) = true; }"),
            4, "event surf.test.v1.E.F: the declared name 'E.F' must not contain '.'",
        )
    }

    @Test
    fun `events must not declare a natsDescriptor field`() {
        assertLocated(
            errors("message A {\n  option (surf.nats.v1.event) = true;\n  optional int32 nats_descriptor = 1;\n}"),
            6, "event surf.test.v1.A must not have a field named natsDescriptor",
        )
    }

    @Test
    fun `mapped events are rejected`() {
        val errors = harness().proto(
            "surf/test/v1/bad.proto",
            "$PROTO2\npackage surf.test.v1;\n$OPTIONS_IMPORT\nmessage Tag { option (surf.nats.v1.event) = true; optional string v = 1; }",
        ).errors(harness().input(mappings = listOf(TypeMapping("surf.test.v1.Tag", "dev.example.Tag", "dev.example.TagSerializer"))))
        assertLocated(errors, 4, "event message surf.test.v1.Tag is mapped by mapType")
    }

    // Services

    @Test
    fun `streaming rpcs are rejected`() {
        assertLocated(
            errors("service S {\n  rpc A(stream google.protobuf.Empty) returns (google.protobuf.Empty);\n}"),
            5, "rpc A: streaming rpcs are not supported",
        )
    }

    @Test
    fun `rpc types that protoc resolves to an rpc are rejected`() {
        val errors = errors(
            "message Count {}\nservice S {\n  rpc Count(google.protobuf.Empty) returns (Count);\n" +
                    "  rpc Total(google.protobuf.Empty) returns (surf.test.v1.Count);\n}"
        )
        assertLocated(errors, 6, "rpc Count: protoc resolves the type 'Count' to the rpc 'Count' of this service")
        assertFalse(errors.any { "rpc Total" in it }, "a package-qualified type is accepted: $errors")
    }

    @Test
    fun `persistent methods cannot address all instances`() {
        assertLocated(
            errors(
                "service S {\n  option (surf.nats.v1.service) = { recipients: NATS_RECIPIENTS_ALL_INSTANCES };\n" +
                        "  rpc A(google.protobuf.Empty) returns (google.protobuf.Empty) { option (surf.nats.v1.method).persistent = true; }\n}"
            ),
            6, "rpc A: persistent methods cannot address ALL_INSTANCES",
        )
    }

    @Test
    fun `persistent fire timeouts above the duplicate window are rejected`() {
        val errors = errors(
            """
            service S {
              option (surf.nats.v1.service) = { call_timeout_seconds: 180 };
              rpc Core(google.protobuf.Empty) returns (Reply);
              rpc Stored(google.protobuf.Empty) returns (google.protobuf.Empty) { option (surf.nats.v1.method).persistent = true; }
              rpc Total(google.protobuf.Empty) returns (Reply) { option (surf.nats.v1.method).persistent = true; }
            }
            message Reply { optional int64 v = 1; }
            """.trimIndent()
        )
        assertLocated(errors, 7, "rpc Stored: the call timeout of a persistent fire method (180s) must not exceed the 120s duplicate window")
        assertEquals(1, errors.size, errors.joinToString("\n"))
    }

    @Test
    fun `timeout zero inherits instead of being rejected`() {
        val text = harness().proto(
            "surf/test/v1/ok.proto",
            "$PROTO2\npackage surf.test.v1;\n$OPTIONS_IMPORT\nimport \"google/protobuf/empty.proto\";\n" +
                    "service S {\n  option (surf.nats.v1.service) = { call_timeout_seconds: 0 };\n" +
                    "  rpc A(google.protobuf.Empty) returns (google.protobuf.Empty) { option (surf.nats.v1.method) = { call_timeout_seconds: 0 persistent: true }; }\n}",
        ).generateFile("Ok.kt")
        assertContainsAll(text, "callTimeoutSeconds = null,")
    }

    @Test
    fun `mapped types other than Empty are rejected as requests and responses`() {
        val errors = errors(
            "import \"google/protobuf/timestamp.proto\";\nservice S {\n  rpc A(google.protobuf.Timestamp) returns (google.protobuf.Empty);\n" +
                    "  rpc B(google.protobuf.Empty) returns (google.protobuf.Timestamp);\n}"
        )
        assertLocated(errors, 6, "rpc A: request type google.protobuf.Timestamp is mapped to java.time.Instant; only google.protobuf.Empty")
        assertLocated(errors, 7, "rpc B: response type google.protobuf.Timestamp is mapped")
    }

    @Test
    fun `rpc names must map to distinct Kotlin functions`() {
        val errors = errors(
            """
            service S {
              rpc GetPlayer(google.protobuf.Empty) returns (google.protobuf.Empty);
              rpc get_player(google.protobuf.Empty) returns (google.protobuf.Empty);
              rpc ToString(google.protobuf.Empty) returns (google.protobuf.Empty);
            }
            """.trimIndent()
        )
        assertLocated(errors, 6, "rpc get_player maps to the Kotlin function 'getPlayer', already used by the rpc at")
        assertLocated(errors, 7, "rpc ToString maps to the Kotlin function 'toString', which clashes with a member of Any")
        assertEquals(2, errors.size, errors.joinToString("\n"))
    }

    @Test
    fun `stream names of services are limited to 255 characters`() {
        // SURF_PERSIST_ (13) + surf_test_v1_ (13) + the service name
        val longest = "S" + "x".repeat(228)
        val tooLong = "T" + "x".repeat(229)
        val errors = errors(
            """
            service $longest { rpc A(google.protobuf.Empty) returns (google.protobuf.Empty); }
            service $tooLong { rpc A(google.protobuf.Empty) returns (google.protobuf.Empty); }
            service Dlq { rpc A(google.protobuf.Empty) returns (google.protobuf.Empty); }
            """.trimIndent()
        )
        assertLocated(
            errors, 5,
            "service surf.test.v1.$tooLong: its stream name SURF_PERSIST_surf_test_v1_$tooLong has 256 characters, more than 255",
        )
        assertEquals(1, errors.size, errors.joinToString("\n"))
    }

    @Test
    fun `underscores count twice towards the stream name limit`() {
        val tooLong = "T_" + "x".repeat(227)
        assertLocated(
            errors("service $tooLong { rpc A(google.protobuf.Empty) returns (google.protobuf.Empty); }"),
            4, "its stream name SURF_PERSIST_surf_test_v1_T__${"x".repeat(227)} has 256 characters",
        )
    }

    @Test
    fun `service names must start with a letter`() {
        assertLocated(
            errors("service _Hidden { rpc A(google.protobuf.Empty) returns (google.protobuf.Empty); }"),
            4, "service surf.test.v1._Hidden: every name component must start with an ASCII letter",
        )
    }

    @Test
    fun `service names must not contain a dot`() {
        assertLocated(
            errors("service S.T { rpc A(google.protobuf.Empty) returns (google.protobuf.Empty); }"),
            4, "service surf.test.v1.S.T: the declared name 'S.T' must not contain '.'",
        )
    }

    @Test
    fun `rpc names must be single identifiers`() {
        val errors = errors(
            "service S {\n" +
                    "  rpc A.B(google.protobuf.Empty) returns (google.protobuf.Empty) { option (surf.nats.v1.method).persistent = true; }\n" +
                    "  rpc _C(google.protobuf.Empty) returns (google.protobuf.Empty);\n}"
        )
        assertLocated(errors, 5, "rpc surf.test.v1.S.A.B: the declared name 'A.B' must not contain '.'")
        assertLocated(errors, 6, "rpc surf.test.v1.S._C: every name component must start with an ASCII letter")
    }

    @Test
    fun `a full name must not be declared in two files`() {
        val service = "service S { rpc A(google.protobuf.Empty) returns (google.protobuf.Empty); }"
        val event = "message E { option (surf.nats.v1.event) = true; }"
        fun file(javaPackage: String) =
            "$PROTO2\npackage a.v1;\noption java_package = \"$javaPackage\";\n$OPTIONS_IMPORT import \"google/protobuf/empty.proto\";\n$service\n$event"

        val errors = harness()
            .proto("a/v1/one.proto", file("x.one.v1"))
            .proto("a/v1/two.proto", file("x.two.v1"))
            .errors()
        assertError(errors, "a/v1/two.proto:5:", "service a.v1.S: the full name is already declared at", "a/v1/one.proto:5:")
        assertError(errors, "a/v1/two.proto:6:", "message a.v1.E: the full name is already declared at", "a/v1/one.proto:6:")
    }

    @Test
    fun `services need at least one rpc`() {
        assertLocated(errors("service Empty {}"), 4, "service surf.test.v1.Empty declares no rpcs")
    }

    @Test
    fun `option values are range checked`() {
        val errors = errors(
            "service S {\n  option (surf.nats.v1.service).call_timeout_seconds = 4294967295;\n" +
                    "  rpc A(google.protobuf.Empty) returns (google.protobuf.Empty);\n}"
        )
        assertLocated(errors, 4, "option NatsServiceOptions.call_timeout_seconds must be between 0 and 2147483647, was '4294967295'")
        val hex = errors(
            "service S {\n  option (surf.nats.v1.service).call_timeout_seconds = 0x80000000;\n" +
                    "  rpc A(google.protobuf.Empty) returns (google.protobuf.Empty);\n}"
        )
        assertLocated(hex, 4, "option NatsServiceOptions.call_timeout_seconds must be between 0 and 2147483647, was '0x80000000'")
    }

    // Type mappings

    @Test
    fun `Empty cannot be remapped and mapped class names must be qualified`() {
        val harness = harness().proto("surf/test/v1/ok.proto", "$PROTO2\npackage surf.test.v1;\nmessage A {}")
        val errors = harness.errors(
            harness.input(
                mappings = listOf(
                    TypeMapping("google.protobuf.Empty", "dev.example.Nothing", "dev.example.NothingSerializer"),
                    TypeMapping("surf.test.v1.Tag", "tag", "dev.example.TagSerializer"),
                )
            )
        )
        assertError(errors, "mapType(google.protobuf.Empty)", "cannot be remapped")
        assertError(errors, "mapType(surf.test.v1.Tag)", "'tag' is not a fully qualified Kotlin class name")
    }

    @Test
    fun `all problems of a run are reported together`() {
        val errors = errors(
            """
            message A {
              required int32 a = 1;
              optional sint32 b = 2;
            }
            service S { rpc X(stream google.protobuf.Empty) returns (google.protobuf.Empty); }
            """.trimIndent()
        )
        assertEquals(3, errors.size, errors.joinToString("\n"))
    }
}

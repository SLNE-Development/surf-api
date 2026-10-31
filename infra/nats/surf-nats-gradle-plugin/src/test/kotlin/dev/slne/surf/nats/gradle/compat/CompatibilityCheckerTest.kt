package dev.slne.surf.nats.gradle.compat

import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.OPTIONS_IMPORT
import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.PROTO2
import dev.slne.surf.nats.gradle.generator.assertContainsAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompatibilityCheckerTest {

    @TempDir
    lateinit var root: File

    private val baselineDir get() = root.resolve("baseline")
    private val currentDir get() = root.resolve("current")

    private fun write(dir: File, path: String, content: String) {
        val file = dir.resolve(path)
        file.parentFile.mkdirs()
        file.writeText(content.trimIndent() + "\n")
    }

    /**
     * Writes [baseline] and [current] as `demo/v1/contract.proto` and returns the violations as text.
     */
    private fun check(baseline: String, current: String): List<String> {
        return report(baseline, current).violations.map { it.toString() }
    }

    private fun report(baseline: String, current: String): CompatibilityReport {
        write(baselineDir, CONTRACT, baseline)
        write(currentDir, CONTRACT, current)
        return CompatibilityChecker.check(listOf(baselineDir), listOf(currentDir), emptyList())
    }

    /**
     * Returns the warnings of a compatible change as text.
     */
    private fun warnings(baseline: String, current: String, imports: String = OPTIONS_IMPORT): List<String> {
        val report = report(proto(baseline, imports), proto(current, imports))
        assertTrue(report.violations.isEmpty(), "expected no violations, got:\n${report.violations.joinToString("\n")}")
        return report.warnings.map { it.toString() }
    }

    private fun proto(body: String, imports: String = OPTIONS_IMPORT): String = """
        |$PROTO2
        |package surf.demo.v1;
        |$imports
        |${body.trimIndent()}
    """.trimMargin()

    private fun assertCompatible(baseline: String, current: String) {
        val violations = check(proto(baseline), proto(current))
        assertTrue(violations.isEmpty(), "expected no violations, got:\n${violations.joinToString("\n")}")
    }

    private fun assertViolation(baseline: String, current: String, vararg fragments: String): List<String> {
        val violations = check(proto(baseline), proto(current))
        assertTrue(
            violations.any { violation -> fragments.all { it in violation } },
            "no violation contains ${fragments.toList()}; violations:\n${violations.joinToString("\n")}",
        )
        return violations
    }

    @Test
    fun `an unchanged contract is compatible`() {
        val contract = """
            message Player {
              option (surf.nats.v1.event) = true;
              optional int64 id = 1;
              optional string name = 2 [(surf.nats.v1.nullable) = true];
              repeated string tags = 3;
              map<string, int32> counts = 4;
              oneof target { string server = 5; int64 region = 6; }
              optional int32 level = 7 [default = 1];
            }
            enum Rank { RANK_UNSPECIFIED = 0; RANK_ADMIN = 1; }
            service PlayerService { rpc Find(Player) returns (Player); }
        """
        assertCompatible(contract, contract)
    }

    @Test
    fun `a changed field number is reported with file, type and field`() {
        val violations = assertViolation(
            "message Player { optional int64 id = 1; }",
            "message Player { optional int64 id = 2; }",
            "surf.demo.v1.Player.id", "field number changed from 1 to 2",
        )
        assertTrue(violations.single().startsWith("$CONTRACT:"), violations.single())
    }

    @Test
    fun `removing a non-null field without default is incompatible even when reserved`() {
        assertViolation(
            "message Player { optional int64 id = 1; optional string name = 2; }",
            "message Player { optional int64 id = 1; reserved 2; reserved \"name\"; }",
            "baseline $CONTRACT", "surf.demo.v1.Player.name", "non-null without a [default]",
        )
    }

    @Test
    fun `removing a nullable field requires its number and name to be reserved`() {
        val baseline = "message Player { optional int64 id = 1; optional string name = 2 [(surf.nats.v1.nullable) = true]; }"
        assertViolation(baseline, "message Player { optional int64 id = 1; }", "Player.name", "add `reserved 2;` `reserved \"name\";`")
        assertViolation(baseline, "message Player { optional int64 id = 1; reserved 2; }", "Player.name", "add `reserved \"name\";` to")
        assertViolation(baseline, "message Player { optional int64 id = 1; reserved \"name\"; }", "Player.name", "add `reserved 2;` to")
        assertCompatible(baseline, "message Player { optional int64 id = 1; reserved 2; reserved \"name\"; }")
    }

    @Test
    fun `fields with a default, repeated, map and oneof fields may be removed when reserved`() {
        assertCompatible(
            """
            message Player {
              optional int64 id = 1;
              optional int32 level = 2 [default = 1];
              repeated string tags = 3;
              map<string, int32> counts = 4;
              oneof target { string server = 5; int64 region = 6; }
            }
            """,
            """
            message Player {
              optional int64 id = 1;
              oneof target { string server = 5; }
              reserved 2 to 4, 6;
              reserved "level", "tags", "counts", "region";
            }
            """,
        )
    }

    @Test
    fun `a changed field type is incompatible, also when wire compatible`() {
        assertViolation(
            "message Player { optional int32 id = 1; }",
            "message Player { optional int64 id = 1; }",
            "Player.id", "type changed from int32 to int64",
        )
        assertViolation(
            "message Player { map<string, int32> counts = 1; }",
            "message Player { map<string, int64> counts = 1; }",
            "Player.counts", "type changed",
        )
    }

    @Test
    fun `a changed label or oneof membership is incompatible`() {
        assertViolation(
            "message Player { optional string tag = 1; }",
            "message Player { repeated string tag = 1; }",
            "Player.tag", "label changed from optional to repeated",
        )
        assertViolation(
            "message Player { optional string server = 1 [(surf.nats.v1.nullable) = true]; }",
            "message Player { oneof target { string server = 1; } }",
            "Player.server", "label changed from optional to oneof target",
        )
        assertViolation(
            "message Player { oneof target { string server = 1; } }",
            "message Player { oneof location { string server = 1; } }",
            "Player.server", "label changed from oneof target to oneof location",
        )
    }

    @Test
    fun `a new non-null field without default is incompatible`() {
        assertViolation(
            "message Player { optional int64 id = 1; }",
            "message Player { optional int64 id = 1; optional string name = 2; }",
            "Player.name", "new field 'name' = 2 is non-null without a [default]",
        )
    }

    @Test
    fun `new nullable, defaulted, repeated, map and oneof fields are compatible`() {
        assertCompatible(
            "message Player { optional int64 id = 1; }",
            """
            message Player {
              optional int64 id = 1;
              optional string name = 2 [(surf.nats.v1.nullable) = true];
              optional int32 level = 3 [default = 1];
              repeated string tags = 4;
              map<string, int32> counts = 5;
              oneof target { string server = 6; int64 region = 7; }
            }
            """,
        )
    }

    @Test
    fun `adding, removing or changing a default is incompatible`() {
        assertViolation(
            "message Player { optional int32 level = 1; }",
            "message Player { optional int32 level = 1 [default = 2]; }",
            "Player.level", "[default = 2] was added",
        )
        assertViolation(
            "message Player { optional int32 level = 1 [default = 2]; }",
            "message Player { optional int32 level = 1; }",
            "Player.level", "[default = 2] was removed",
        )
        assertViolation(
            "message Player { optional int32 level = 1 [default = 2]; }",
            "message Player { optional int32 level = 1 [default = 3]; }",
            "Player.level", "[default] changed from 2 to 3",
        )
    }

    @Test
    fun `an equal default written differently is compatible`() {
        assertCompatible(
            "message Player { optional int32 level = 1 [default = 16]; optional string name = 2 [default = \"a\"]; }",
            "message Player { optional int32 level = 1 [default = 0x10]; optional string name = 2 [default = 'a']; }",
        )
    }

    @Test
    fun `a changed nullable option is incompatible`() {
        assertViolation(
            "message Player { optional string name = 1; }",
            "message Player { optional string name = 1 [(surf.nats.v1.nullable) = true]; }",
            "Player.name", "(surf.nats.v1.nullable) changed from false to true",
        )
        assertViolation(
            "message Player { optional string name = 1 [(surf.nats.v1.nullable) = true]; }",
            "message Player { optional string name = 1; }",
            "Player.name", "(surf.nats.v1.nullable) changed from true to false",
        )
    }

    @Test
    fun `removed or renamed messages, enums, services and rpcs are incompatible`() {
        val baseline = """
            message Player { optional int64 id = 1; }
            message Unused { optional int64 id = 1; }
            enum Rank { RANK_UNSPECIFIED = 0; }
            service PlayerService { rpc Find(Player) returns (Player); rpc Count(Player) returns (Player); }
            service AdminService { rpc Kick(Player) returns (Player); }
        """
        val current = """
            message Player { optional int64 id = 1; }
            message Renamed { optional int64 id = 1; }
            service PlayerService { rpc Find(Player) returns (Player); }
        """
        val violations = check(proto(baseline), proto(current))
        val expected = listOf(
            "surf.demo.v1.Unused: message surf.demo.v1.Unused was removed or renamed",
            "surf.demo.v1.Rank: enum surf.demo.v1.Rank was removed or renamed",
            "surf.demo.v1.PlayerService.Count: rpc Count was removed or renamed",
            "surf.demo.v1.AdminService: service surf.demo.v1.AdminService was removed or renamed",
        )
        for (fragment in expected) {
            assertTrue(violations.any { fragment in it && it.startsWith("baseline $CONTRACT:") }, "missing $fragment in\n$violations")
        }
        assertEquals(expected.size, violations.size, violations.joinToString("\n"))
    }

    @Test
    fun `nested types are compared by their full name`() {
        assertViolation(
            "message Player { optional Stats stats = 1; message Stats { optional int32 kills = 1; } }",
            "message Player { optional Stats stats = 1; message Stats { optional int64 kills = 1; } }",
            "surf.demo.v1.Player.Stats.kills", "type changed",
        )
    }

    @Test
    fun `a message that became an enum is incompatible`() {
        assertViolation(
            "message Rank { optional int32 id = 1; }",
            "enum Rank { RANK_UNSPECIFIED = 0; }",
            "surf.demo.v1.Rank", "changed from message to enum",
        )
    }

    @Test
    fun `enum values must not be removed without reserving number and name`() {
        val baseline = "enum Rank { RANK_UNSPECIFIED = 0; RANK_ADMIN = 1; RANK_MOD = 2; }"
        assertViolation(baseline, "enum Rank { RANK_UNSPECIFIED = 0; RANK_ADMIN = 1; }", "surf.demo.v1.Rank.RANK_MOD", "`reserved 2;` `reserved \"RANK_MOD\";`")
        assertViolation(baseline, "enum Rank { RANK_UNSPECIFIED = 0; RANK_ADMIN = 1; reserved 2; }", "Rank.RANK_MOD", "was removed")
        assertCompatible(baseline, "enum Rank { RANK_UNSPECIFIED = 0; RANK_ADMIN = 1; reserved 2; reserved \"RANK_MOD\"; }")
        assertCompatible(baseline, "enum Rank { RANK_UNSPECIFIED = 0; RANK_ADMIN = 1; RANK_MOD = 2; RANK_OWNER = 3; }")
    }

    @Test
    fun `a changed enum value number is incompatible`() {
        assertViolation(
            "enum Rank { RANK_UNSPECIFIED = 0; RANK_ADMIN = 1; }",
            "enum Rank { RANK_UNSPECIFIED = 0; RANK_ADMIN = 2; }",
            "Rank.RANK_ADMIN", "number changed from 1 to 2",
        )
    }

    @Test
    fun `a changed enum zero value is incompatible`() {
        assertViolation(
            "enum Rank { RANK_UNSPECIFIED = 0; RANK_ADMIN = 1; }",
            "enum Rank { RANK_UNKNOWN = 0; RANK_ADMIN = 1; reserved \"RANK_UNSPECIFIED\"; }",
            "surf.demo.v1.Rank", "zero value changed from RANK_UNSPECIFIED to RANK_UNKNOWN",
        )
    }

    @Test
    fun `changed rpc request and response types are incompatible`() {
        val messages = "message A { optional int64 id = 1; } message B { optional int64 id = 1; }"
        assertViolation(
            "$messages service S { rpc Call(A) returns (B); }",
            "$messages service S { rpc Call(B) returns (B); }",
            "surf.demo.v1.S.Call", "request type changed from surf.demo.v1.A to surf.demo.v1.B",
        )
        assertViolation(
            "$messages service S { rpc Call(A) returns (B); }",
            "$messages service S { rpc Call(A) returns (A); }",
            "surf.demo.v1.S.Call", "response type changed from surf.demo.v1.B to surf.demo.v1.A",
        )
    }

    @Test
    fun `dropping the event opt-in is incompatible`() {
        assertViolation(
            "message PlayerJoined { option (surf.nats.v1.event) = true; }",
            "message PlayerJoined { }",
            "surf.demo.v1.PlayerJoined", "is no longer an event",
        )
        assertViolation(
            "message PlayerJoined { option (surf.nats.v1.event) = true; }",
            "message PlayerJoined { option (surf.nats.v1.event) = false; }",
            "surf.demo.v1.PlayerJoined", "is no longer an event",
        )
        assertCompatible("message PlayerJoined { }", "message PlayerJoined { option (surf.nats.v1.event) = true; }")
        assertCompatible(
            "message PlayerJoined { option (surf.nats.v1.event) = false; }",
            "message PlayerJoined { option (surf.nats.v1.event) = true; }",
        )
    }

    @Test
    fun `renaming an event or a service removes it`() {
        assertViolation(
            "message PlayerJoined { option (surf.nats.v1.event) = true; }",
            "message PlayerEntered { option (surf.nats.v1.event) = true; }",
            "surf.demo.v1.PlayerJoined", "message surf.demo.v1.PlayerJoined was removed or renamed",
        )
        val message = "message A { optional int64 id = 1; }"
        assertViolation(
            "$message service PlayerService { rpc Call(A) returns (A); }",
            "$message service Players { rpc Call(A) returns (A); }",
            "surf.demo.v1.PlayerService", "service surf.demo.v1.PlayerService was removed or renamed",
        )
    }

    @Test
    fun `rpc renames, resolved persistent and recipients changes are incompatible`() {
        val message = "message A { optional int64 id = 1; }"
        assertViolation(
            "$message service S { rpc FindPlayer(A) returns (A); }",
            "$message service S { rpc Find(A) returns (A); }",
            "surf.demo.v1.S.FindPlayer", "rpc FindPlayer was removed or renamed",
        )
        assertViolation(
            "$message service S { rpc Call(A) returns (A); }",
            "$message service S { option (surf.nats.v1.service) = { persistent: true }; rpc Call(A) returns (A); }",
            "surf.demo.v1.S.Call", "persistent changed from false to true",
        )
        assertCompatible(
            "$message service S { rpc Call(A) returns (A); }",
            "$message service S { option (surf.nats.v1.service) = { persistent: true }; " +
                    "rpc Call(A) returns (A) { option (surf.nats.v1.method) = { persistent: false }; } }",
        )
        assertViolation(
            "$message service S { rpc Call(A) returns (A); }",
            "$message service S { option (surf.nats.v1.service) = { recipients: NATS_RECIPIENTS_ALL_INSTANCES }; rpc Call(A) returns (A); }",
            "surf.demo.v1.S.Call", "recipients changed from ANY_INSTANCE to ALL_INSTANCES",
        )
        assertCompatible(
            "$message service S { rpc Call(A) returns (A); }",
            "$message service S { rpc Call(A) returns (A) { option (surf.nats.v1.method) = { recipients: NATS_RECIPIENTS_ANY_INSTANCE }; } }",
        )
    }

    @Test
    fun `call timeouts are not compared`() {
        val message = "message A { optional int64 id = 1; }"
        assertCompatible(
            "$message service S { rpc Call(A) returns (A); }",
            "$message service S { option (surf.nats.v1.service) = { call_timeout_seconds: 30 }; " +
                    "rpc Call(A) returns (A) { option (surf.nats.v1.method) = { call_timeout_seconds: 10 }; } }",
        )
    }

    @Test
    fun `a new major version is never compared with the previous one`() {
        val v1 = proto("message Player { optional int64 id = 1; }")
        write(baselineDir, CONTRACT, v1)
        write(currentDir, CONTRACT, v1)
        write(
            currentDir,
            "demo/v2/contract.proto",
            "$PROTO2\npackage surf.demo.v2;\nmessage Player { optional string id = 1; optional string name = 2; }\n",
        )
        assertEquals(emptyList(), CompatibilityChecker.check(listOf(baselineDir), listOf(currentDir), emptyList()).violations)
    }

    @Test
    fun `dropping a major version reports its elements as removed`() {
        write(baselineDir, CONTRACT, proto("message Player { optional int64 id = 1; }"))
        write(currentDir, "demo/v2/contract.proto", "$PROTO2\npackage surf.demo.v2;\nmessage Player { optional int64 id = 1; }\n")
        val violations = CompatibilityChecker.check(listOf(baselineDir), listOf(currentDir), emptyList()).violations.map { it.toString() }
        assertEquals(listOf("baseline $CONTRACT:4:1: surf.demo.v1.Player: message surf.demo.v1.Player was removed or renamed"), violations)
    }

    @Test
    fun `without current sources every baseline element is removed`() {
        write(baselineDir, CONTRACT, proto("message Player { optional int64 id = 1; } service S { rpc Call(Player) returns (Player); }"))
        currentDir.mkdirs()
        val violations = CompatibilityChecker.check(listOf(baselineDir), listOf(currentDir), emptyList()).violations
        assertEquals(listOf("surf.demo.v1.Player", "surf.demo.v1.S"), violations.map { it.element })
    }

    @Test
    fun `all violations of a change set are reported together`() {
        val violations = check(
            proto(
                """
                message Player { optional int64 id = 1; optional string name = 2; }
                enum Rank { RANK_UNSPECIFIED = 0; RANK_ADMIN = 1; }
                """
            ),
            proto(
                """
                message Player { optional int32 id = 3; optional string nickname = 4; }
                enum Rank { RANK_UNSPECIFIED = 0; }
                """
            ),
        )
        val elements = violations.map { it.substringAfter(": ").substringBefore(": ") }
        assertEquals(
            listOf(
                "surf.demo.v1.Player.id",
                "surf.demo.v1.Player.id",
                "surf.demo.v1.Player.name",
                "surf.demo.v1.Player.nickname",
                "surf.demo.v1.Rank.RANK_ADMIN",
            ),
            elements,
            violations.joinToString("\n"),
        )
    }

    @Test
    fun `map types are compared by their resolved key and value types`() {
        assertCompatible(
            "message Value { optional int64 id = 1; } message Player { map<string, Value> values = 1; }",
            "message Value { optional int64 id = 1; } message Player { map<string, .surf.demo.v1.Value> values = 1; }",
        )
        assertViolation(
            "message Value { optional int64 id = 1; } message Player { map<string, Value> values = 1; }",
            "message Value { optional int64 id = 1; } message Player { map<string, Value> values = 1; " +
                    "enum Value { VALUE_UNSPECIFIED = 0; } }",
            "surf.demo.v1.Player.values", "type changed from map<string, surf.demo.v1.Value> to map<string, surf.demo.v1.Player.Value>",
        )
    }

    @Test
    fun `string and bytes defaults compare by the value the generator emits`() {
        assertCompatible(
            "message Player { optional string name = 1 [default = \"\\303\\251\"]; }",
            "message Player { optional string name = 1 [default = \"é\"]; }",
        )
        assertViolation(
            "message Player { optional bytes data = 1 [default = \"\\351\"]; }",
            "message Player { optional bytes data = 1 [default = \"é\"]; }",
            "Player.data", "[default] changed",
        )
    }

    @Test
    fun `a changed java_package is incompatible`() {
        val violations = check(
            proto("message Player { optional int64 id = 1; } service S { rpc Call(Player) returns (Player); }",
                "$OPTIONS_IMPORT\noption java_package = \"dev.slne.demo.v1\";"),
            proto("message Player { optional int64 id = 1; } service S { rpc Call(Player) returns (Player); }",
                "$OPTIONS_IMPORT\noption java_package = \"dev.slne.other.v1\";"),
        )
        assertEquals(
            listOf("surf.demo.v1.Player", "surf.demo.v1.S"),
            violations.filter { "java_package changed the Kotlin package from 'dev.slne.demo.v1' to 'dev.slne.other.v1'" in it }
                .map { it.substringAfter(": ").substringBefore(": ") },
            violations.joinToString("\n"),
        )
    }

    @Test
    fun `reservations must be kept`() {
        assertViolation(
            "message Player { optional int64 id = 1; reserved 2, 5 to 9; reserved \"name\"; }",
            "message Player { optional int64 id = 1; reserved 2, 5 to 8; }",
            "surf.demo.v1.Player", "reserved 5 to 9 is no longer reserved",
        )
        assertViolation(
            "message Player { optional int64 id = 1; reserved 2; reserved \"name\"; }",
            "message Player { optional int64 id = 1; optional string name = 2 [(surf.nats.v1.nullable) = true]; }",
            "surf.demo.v1.Player", "reserved \"name\" is no longer reserved",
        )
        assertCompatible(
            "message Player { optional int64 id = 1; reserved 2, 5 to 9; reserved \"name\"; }",
            "message Player { optional int64 id = 1; reserved 2 to 3, 5, 6 to 12; reserved \"name\", \"tag\"; }",
        )
        assertViolation(
            "enum Rank { RANK_UNSPECIFIED = 0; reserved 3; }",
            "enum Rank { RANK_UNSPECIFIED = 0; RANK_MOD = 3; }",
            "surf.demo.v1.Rank", "reserved 3 is no longer reserved",
        )
    }

    @Test
    fun `defaults after a literal with an escaped newline are compared with their own field`() {
        val baseline = """
            message Greeting {
              optional string greeting = 1 [default = "Hello\nWorld"];
              optional string farewell = 2 [default = "Bye"];
              optional string third = 3 [default = "Third"];
            }
        """
        assertViolation(baseline, baseline.replace("\"Bye\"", "\"Ciao\""), "Greeting.farewell", "[default] changed from Bye to Ciao")

        val violations = check(proto(baseline), proto(baseline.replace("\"Third\"", "\"Drei\"")))
        assertEquals(listOf("surf.demo.v1.Greeting.third"), violations.map { it.substringAfter(": ").substringBefore(": ") }, violations.joinToString("\n"))
    }

    @Test
    fun `a new field numbered below the baseline maximum warns that positional parameters shift`() {
        val service = "service BanService { rpc Ban(BanRequest) returns (google.protobuf.Empty); }"
        val warnings = warnings(
            "message BanRequest { optional string player = 1; optional string reason = 3 [(surf.nats.v1.nullable) = true]; } $service",
            "message BanRequest { optional string player = 1; optional string moderator = 2 [(surf.nats.v1.nullable) = true]; " +
                    "optional string reason = 3 [(surf.nats.v1.nullable) = true]; } $service",
            imports = "$OPTIONS_IMPORT\nimport \"google/protobuf/empty.proto\";",
        )
        val warning = warnings.single()
        assertTrue(warning.startsWith("$CONTRACT:"), warning)
        assertContainsAll(
            warning,
            "surf.demo.v1.BanRequest",
            "new field 'moderator' = 2 is numbered below the baseline maximum 3",
            "'reason' moves from position 2 to 3",
            "the constructor of BanRequest and the parameters of BanService.Ban",
            "new fields always get a number above the current maximum",
        )
    }

    @Test
    fun `a new oneof case below the lowest case number warns when it moves the oneof`() {
        val warning = warnings(
            "message Target { optional string name = 1; optional int32 level = 3; oneof where { string server = 5; } }",
            "message Target { optional string name = 1; optional int32 level = 3; oneof where { string region = 2; string server = 5; } }",
        ).single()
        assertContainsAll(
            warning,
            "surf.demo.v1.Target",
            "new oneof case 'region' = 2 is numbered below the lowest baseline case 5 of oneof 'where'",
            "'where' moves from position 3 to 2",
            "'level' moves from position 2 to 3",
        )
    }

    @Test
    fun `new members that keep the parameter order do not warn`() {
        val baseline = "message Target { optional string name = 1; oneof where { string server = 2; string region = 10; } }"
        assertEquals(
            emptyList(),
            warnings(baseline, "message Target { optional string name = 1; oneof where { string server = 2; string region = 10; } " +
                    "optional int32 level = 11 [default = 1]; repeated string tags = 12; }"),
        )
        // Below the maximum 10, but behind every existing parameter
        assertEquals(
            emptyList(),
            warnings(baseline, "message Target { optional string name = 1; oneof where { string server = 2; string region = 10; } " +
                    "optional int32 level = 5 [default = 1]; }"),
        )
        // A new lowest case that stays behind name
        assertEquals(
            emptyList(),
            warnings(
                "message Target { optional string name = 1; oneof where { string server = 5; } }",
                "message Target { optional string name = 1; oneof where { string region = 3; string server = 5; } }",
            ),
        )
    }

    @Test
    fun `a new oneof below the baseline maximum warns`() {
        val warning = warnings(
            "message Target { optional string name = 1; optional int32 level = 4; }",
            "message Target { optional string name = 1; oneof where { string server = 2; string region = 3; } optional int32 level = 4; }",
        ).single()
        assertContainsAll(warning, "new oneof 'where' = 2 is numbered below the baseline maximum 4", "'level' moves from position 2 to 3")
    }

    private companion object {
        const val CONTRACT = "demo/v1/contract.proto"
    }
}

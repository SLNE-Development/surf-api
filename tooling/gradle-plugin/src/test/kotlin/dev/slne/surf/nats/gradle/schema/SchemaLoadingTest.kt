package dev.slne.surf.nats.gradle.schema

import com.squareup.wire.schema.ProtoType
import dev.slne.surf.nats.gradle.generator.*
import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.OPTIONS_IMPORT
import dev.slne.surf.nats.gradle.generator.GeneratorHarness.Companion.PROTO2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchemaLoadingTest {
    @TempDir
    lateinit var temp: File

    @Test
    fun `bundled protos cover the options and every surf type`() {
        assertEquals(
            listOf(
                "surf/nats/v1/options.proto",
                "surf/types/paper/v1/item_stack.proto",
                "surf/types/paper/v1/location.proto",
                "surf/types/v1/big_decimal.proto",
                "surf/types/v1/big_integer.proto",
                "surf/types/v1/component.proto",
                "surf/types/v1/inet_socket_address.proto",
                "surf/types/v1/ip_address.proto",
                "surf/types/v1/key.proto",
                "surf/types/v1/local_date.proto",
                "surf/types/v1/local_date_time.proto",
                "surf/types/v1/local_time.proto",
                "surf/types/v1/offset_date_time.proto",
                "surf/types/v1/shadow_color.proto",
                "surf/types/v1/sound.proto",
                "surf/types/v1/text_color.proto",
                "surf/types/v1/title.proto",
                "surf/types/v1/uuid.proto",
                "surf/types/v1/zoned_date_time.proto",
            ),
            BundledProtos.paths,
        )
        for (path in BundledProtos.paths) {
            assertTrue(BundledProtos.open(path).use { it.reader().readText() }.contains("syntax = \"proto2\";"), path)
        }
    }

    @Test
    fun `loadSchema returns only the source files, sorted by path`() {
        val src = temp.resolve("src")
        src.resolve("b/v1/b.proto").apply { parentFile.mkdirs() }.writeText("$PROTO2\npackage b.v1;\nmessage B {}\n")
        src.resolve("a/v1/a.proto").apply { parentFile.mkdirs() }
            .writeText("$PROTO2\npackage a.v1;\nimport \"surf/types/v1/uuid.proto\";\nmessage A { required surf.types.v1.Uuid id = 1; }\n")
        val loaded = loadSchema(listOf(src), emptyList())
        assertEquals(listOf("a/v1/a.proto", "b/v1/b.proto"), loaded.sourceFiles.map { it.location.path })
        assertTrue(loaded.schema.getType("surf.types.v1.Uuid") != null)
    }

    @Test
    fun `proto-path copies of bundled protos are hidden so the bundled copy wins`() {
        val protoPath = temp.resolve("path")
        protoPath.resolve("surf/nats/v1/options.proto").apply { parentFile.mkdirs() }.writeText("this is not a proto")
        protoPath.resolve("other/v1/shared.proto").apply { parentFile.mkdirs() }
            .writeText("$PROTO2\npackage other.v1;\nmessage Shared { required int32 x = 1; }\n")

        val harness = GeneratorHarness(temp).proto(
            "surf/test/v1/a.proto",
            """
            $PROTO2
            package surf.test.v1;
            $OPTIONS_IMPORT
            import "other/v1/shared.proto";
            message A {
              option (surf.nats.v1.event) = true;
              required other.v1.Shared shared = 1;
            }
            """,
        )
        val text = harness.generateFile(
            "A.kt",
            harness.input(
                protoPathDirs = listOf(protoPath),
                mappings = listOf(TypeMapping(ProtoType.get("other.v1.Shared"), "dev.example.Shared", "dev.example.SharedSerializer")),
            ),
        )
        assertContainsAll(text, "public val shared: @Serializable(with = SharedSerializer::class) Shared,", ") : NatsEvent {")
    }

    @Test
    fun `proto-path types must be mapped to be used`() {
        val protoPath = temp.resolve("path")
        protoPath.resolve("other/v1/shared.proto").apply { parentFile.mkdirs() }
            .writeText("$PROTO2\npackage other.v1;\nmessage Shared { required int32 x = 1; }\n")
        val harness = GeneratorHarness(temp).proto(
            "surf/test/v1/a.proto",
            "$PROTO2\npackage surf.test.v1;\nimport \"other/v1/shared.proto\";\nmessage A { required other.v1.Shared shared = 1; }",
        )
        assertError(
            harness.errors(harness.input(protoPathDirs = listOf(protoPath))),
            "a.proto:4:", "other.v1.Shared from other/v1/shared.proto is neither generated nor mapped",
        )
    }

    @Test
    fun `bundled paths inside a source dir are rejected`() {
        val src = temp.resolve("src")
        src.resolve("surf/types/v1/uuid.proto").apply { parentFile.mkdirs() }.writeText("$PROTO2\npackage surf.types.v1;\n")
        val exception = assertThrows<SurfNatsGeneratorException> { loadSchema(listOf(src), emptyList()) }
        assertError(exception.errors, "surf/types/v1/uuid.proto: surf/types/v1/uuid.proto is bundled with the surf-nats plugin")
    }

    @Test
    fun `errors in bundled files name the plugin`() {
        val src = temp.resolve("src")
        src.resolve("surf/test/v1/a.proto").apply { parentFile.mkdirs() }
            .writeText("$PROTO2\npackage surf.test.v1;\nimport \"surf/types/v1/uuid.proto\";\nmessage A { required surf.types.v1.Uuid.Missing x = 1; }\n")
        val exception = assertThrows<SurfNatsGeneratorException> { loadSchema(listOf(src), emptyList()) }
        assertTrue(exception.errors.none { "surf-nats-protos" in it }, exception.errors.joinToString("\n"))
        assertError(exception.errors, "surf/test/v1/a.proto:4:", "unable to resolve surf.types.v1.Uuid.Missing")
    }

    @Test
    fun `the bundled proto root is named after the plugin`() {
        val src = temp.resolve("src")
        src.resolve("surf/test/v1/a.proto").apply { parentFile.mkdirs() }
            .writeText("$PROTO2\npackage surf.test.v1;\nimport \"missing/v1/x.proto\";\nmessage A {}\n")
        val exception = assertThrows<SurfNatsGeneratorException> { loadSchema(listOf(src), emptyList()) }
        assertError(exception.errors, "unable to find missing/v1/x.proto", "searching 1 proto paths:, surf-nats-plugin)")
    }

    @Test
    fun `missing sources are reported`() {
        val exception = assertThrows<SurfNatsGeneratorException> { loadSchema(listOf(temp.resolve("missing")), emptyList()) }
        assertError(exception.errors, "no .proto files found")
    }
}

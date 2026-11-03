package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.testkit.TarGz
import dev.slne.surf.nats.gradle.testkit.TestBuild
import dev.slne.surf.nats.gradle.testkit.TestBuild.Companion.CONTRACT_PATH
import dev.slne.surf.nats.gradle.testkit.TestKitSupport
import dev.slne.surf.nats.gradle.testkit.WeaverStub
import dev.slne.surf.nats.gradle.testkit.outcome
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runs the compatibility check against baselines that the weaver stub serves.
 */
internal class CompatibilityEndToEndTest : TestKitSupport() {

    private val group = "dev.slne.demo"
    private val name = "demo"
    private val baseline = "message Player { optional int64 id = 1; }"
    private val report get() = build.resolve("build/reports/surf-nats/compatibility.txt")

    private fun module(current: String, extra: String = "", published: Boolean = true) {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${if (published) TestBuild.PUBLISHING else ""}
            ${weaverStub()}
            group = '$group'
            version = '1.1.0-SNAPSHOT'
            $extra
            """
        )
        build.proto(CONTRACT_PATH, TestBuild.contract(current))
    }

    private fun serveBaseline(contract: String = baseline, version: String = "1.0.0", withVersionList: Boolean = true) {
        if (withVersionList) stub.versions(group, name, version to false)
        stub.archive(group, name, version, TarGz.create(mapOf(CONTRACT_PATH to TestBuild.contract(contract))))
    }

    @Test
    fun `a changed field number fails the check against the released baseline`() {
        module("message Player { optional int64 id = 2; }")
        serveBaseline()

        val result = build.runAndFail("checkSurfNatsCompatibility")

        assertEquals(TaskOutcome.SUCCESS, result.outcome(":fetchSurfNatsBaseline"))
        assertEquals(TaskOutcome.FAILED, result.outcome(":checkSurfNatsCompatibility"))
        assertContains(result.output, "surf-nats contracts are incompatible with the baseline")
        assertContains(result.output, "surf.demo.v1.Player.id")
        assertContains(result.output, "field number changed from 1 to 2")
        assertContains(report.readText(), "field number changed from 1 to 2")
    }

    @Test
    fun `a compatible change passes the check and is reported as compatible`() {
        module("message Player { optional int64 id = 1; optional string name = 2 [default = \"anonymous\"]; }")
        serveBaseline()

        val result = build.run("checkSurfNatsCompatibility")

        assertEquals(TaskOutcome.SUCCESS, result.outcome(":checkSurfNatsCompatibility"))
        assertTrue(report.readText().startsWith("compatible"), report.readText())
    }

    @Test
    fun `the check compares against the baseline's own files only`() {
        module("message Player { optional int64 id = 1; }")
        stub.versions(group, name, "1.0.0" to false)
        stub.archive(
            group, name, "1.0.0",
            TarGz.create(mapOf(CONTRACT_PATH to TestBuild.contract(baseline), "surf/demo/v1/gone.proto" to TestBuild.contract("message Gone { optional int64 id = 1; }"))),
        )

        val result = build.runAndFail("checkSurfNatsCompatibility")

        assertContains(result.output, "surf/demo/v1/gone.proto")
    }

    @Test
    fun `an explicit baseline activates the check for an unpublished module`() {
        module(
            "message Player { optional int64 id = 2; }",
            extra = "surfNats { compatibilityBaseline('1.0.0') }",
            published = false,
        )
        serveBaseline(withVersionList = false)

        val result = build.runAndFail("checkSurfNatsCompatibility")

        assertContains(result.output, "field number changed from 1 to 2")
        assertEquals(listOf("GET ${WeaverStub.artifactPath(WeaverStub.REPOSITORY, group, name)}/1.0.0/archive"), stub.requests.map { "${it.method} ${it.path}" })
    }

    @Test
    fun `the check is inactive for an unpublished module without an explicit baseline`() {
        module("message Player { optional int64 id = 2; }", published = false)
        serveBaseline()

        val result = build.run("checkSurfNatsCompatibility")

        assertEquals(TaskOutcome.SKIPPED, result.outcome(":fetchSurfNatsBaseline"))
        assertEquals(TaskOutcome.SKIPPED, result.outcome(":checkSurfNatsCompatibility"))
        assertEquals(emptyList(), stub.requests.toList())
    }

    @Test
    fun `the check can be disabled for a published module`() {
        module("message Player { optional int64 id = 2; }", extra = "surfNats { compatibilityCheck = false }")
        serveBaseline()

        val result = build.run("checkSurfNatsCompatibility")

        assertEquals(TaskOutcome.SKIPPED, result.outcome(":fetchSurfNatsBaseline"))
        assertEquals(TaskOutcome.SKIPPED, result.outcome(":checkSurfNatsCompatibility"))
        assertEquals(emptyList(), stub.requests.toList())
    }

    @Test
    fun `a baseline archive without protos fails the check`() {
        module("message Player { optional int64 id = 1; }", extra = "surfNats { compatibilityBaseline('1.0.0') }")
        stub.archive(group, name, "1.0.0", TarGz.create(mapOf("README.txt" to "no protos")))

        val result = build.runAndFail("checkSurfNatsCompatibility")

        assertContains(result.output, "The surf-nats compatibility baseline 1.0.0 contains no .proto files")
    }

    @Test
    fun `an incompatible change stops publishProtos before the upload`() {
        module("message Player { optional int64 id = 2; }")
        serveBaseline()
        stub.acceptPublish(group, name, "1.1.0-SNAPSHOT")

        val result = build.runAndFail("publish")

        assertEquals(TaskOutcome.FAILED, result.outcome(":checkSurfNatsCompatibility"))
        assertEquals(null, result.task(":publishProtos"))
        assertEquals(emptyList(), stub.requests("PUT"))
        assertFalse(build.resolve("build/repo").exists())
    }

    @Test
    fun `a compatible change is published after the check`() {
        module("message Player { optional int64 id = 1; optional string name = 2 [default = \"anonymous\"]; }")
        serveBaseline()
        stub.acceptPublish(group, name, "1.1.0-SNAPSHOT")

        val result = build.run("publish")

        val tasks = result.tasks.map { it.path }
        assertTrue(tasks.indexOf(":checkSurfNatsCompatibility") < tasks.indexOf(":publishProtos"), tasks.toString())
        assertEquals(1, stub.requests("PUT").size)
    }
}

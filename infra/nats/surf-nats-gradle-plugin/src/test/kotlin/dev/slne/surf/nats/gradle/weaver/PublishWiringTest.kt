package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.testkit.StubResponse
import dev.slne.surf.nats.gradle.testkit.TarGz
import dev.slne.surf.nats.gradle.testkit.TestBuild
import dev.slne.surf.nats.gradle.testkit.TestBuild.Companion.CONTRACT_PATH
import dev.slne.surf.nats.gradle.testkit.TestKitSupport
import dev.slne.surf.nats.gradle.testkit.outcome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class PublishWiringTest : TestKitSupport() {

    private val publishTask = ":publishMPublicationToLocalRepository"

    private fun publishedModule(extra: String = "") {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${TestBuild.PUBLISHING}
            ${weaverStub()}
            group = 'dev.slne.demo'
            version = '1.2.0-SNAPSHOT'
            $extra
            """
        )
    }

    private fun contract() {
        build.proto(CONTRACT_PATH, TestBuild.contract("message Player { optional int64 id = 1; }"))
    }

    private fun assertBefore(plan: List<String>, first: String, second: String) {
        assertContains(plan, first)
        assertContains(plan, second)
        assertTrue(plan.indexOf(first) < plan.indexOf(second), "$first must run before $second in $plan")
    }

    @Test
    fun `publish depends on publishProtos, which depends on generation and the check`() {
        publishedModule()

        val plan = build.plan("publish")

        assertBefore(plan, ":generateSurfNatsKotlin", ":publishProtos")
        assertBefore(plan, ":checkSurfNatsCompatibility", ":publishProtos")
        assertBefore(plan, ":fetchSurfNatsBaseline", ":checkSurfNatsCompatibility")
        assertBefore(plan, ":stageSurfNatsWeaverSource", ":publishProtos")
        assertContains(plan, ":publish")
    }

    @Test
    fun `maven publications run after publishProtos without depending on it`() {
        publishedModule()

        val alone = build.plan(publishTask)
        assertContains(alone, publishTask)
        assertFalse(":publishProtos" in alone, "publishing to Maven alone does not publish protos: $alone")

        val reversed = build.plan(publishTask, "publishProtos")
        assertBefore(reversed, ":publishProtos", publishTask)

        val viaPublish = build.plan("publish")
        assertBefore(viaPublish, ":publishProtos", publishTask)
    }

    @Test
    fun `publishToMavenLocal does not publish protos`() {
        publishedModule()

        val plan = build.plan("publishToMavenLocal")

        assertContains(plan, ":publishMPublicationToMavenLocal")
        assertFalse(":publishProtos" in plan, plan.toString())
    }

    @Test
    fun `without a publishing repository publish does not publish protos`() {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            apply plugin: 'maven-publish'
            publishing { publications { m(MavenPublication) { } } }
            """
        )

        val plan = build.plan("publish")

        assertContains(plan, ":publish")
        assertFalse(":publishProtos" in plan, plan.toString())
    }

    @Test
    fun `a repository added after the plugin is applied still wires publishing`() {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            apply plugin: 'maven-publish'
            publishing { publications { m(MavenPublication) { } } }
            afterEvaluate { publishing { repositories { maven { name = 'late'; url = uri('build/late') } } } }
            """
        )

        val plan = build.plan("publish")

        assertContains(plan, ":publishProtos")
        assertBefore(plan, ":publishProtos", ":publish")
    }

    @Test
    fun `publishProtos is skipped when there are no proto files`() {
        publishedModule()

        val result = build.run("publishProtos")

        assertEquals(TaskOutcome.SKIPPED, result.outcome(":publishProtos"), result.output)
        assertEquals(emptyList(), stub.requests("PUT"))
    }

    @Test
    fun `publish uploads the staged protos to the registry and then publishes to Maven`() {
        publishedModule()
        contract()
        build.file("src/main/proto/readme.txt", "ignored")
        stub.acceptPublish("dev.slne.demo", "demo", "1.2.0-SNAPSHOT")

        val result = build.run("publish")

        assertEquals(TaskOutcome.SUCCESS, result.outcome(":publishProtos"), result.output)
        assertEquals(TaskOutcome.SUCCESS, result.outcome(publishTask))
        val tasks = result.tasks.map { it.path }
        assertTrue(tasks.indexOf(":publishProtos") < tasks.indexOf(publishTask), tasks.toString())

        val upload = stub.requests("PUT").single()
        assertEquals("/api/v1/repos/proto-releases-public/artifacts/dev.slne.demo/demo/1.2.0-SNAPSHOT", upload.path)
        val manifest = Json.parseToJsonElement(String(assertNotNull(upload.part("manifest")))).jsonObject
        assertEquals("dev.slne.demo", manifest.getValue("group").jsonPrimitive.content)
        assertEquals("demo", manifest.getValue("name").jsonPrimitive.content)
        assertEquals("1.2.0-SNAPSHOT", manifest.getValue("version").jsonPrimitive.content)
        assertEquals(listOf(CONTRACT_PATH), TarGz.names(assertNotNull(upload.part("archive"))))
        assertTrue(build.resolve("build/repo/dev/slne/demo/demo/1.2.0-SNAPSHOT").isDirectory)
    }

    @Test
    fun `a rejected upload fails publish before anything reaches Maven`() {
        publishedModule()
        contract()
        stub.route("PUT", "/api/v1/repos/proto-releases-public/artifacts/dev.slne.demo/demo/1.2.0-SNAPSHOT") {
            StubResponse(
                422,
                """{"error":{"code":"validation_failed","message":"rejected","details":{"issues":[{"path":"a.proto","line":3,"message":"nope"}]}}}""",
            )
        }

        val result = build.runAndFail("publish")

        assertEquals(TaskOutcome.FAILED, result.outcome(":publishProtos"))
        assertContains(result.output, "validation_failed: rejected")
        assertEquals(null, result.task(publishTask), "the Maven publication must not start after a failed upload")
        assertFalse(build.resolve("build/repo").exists())
    }

    @Test
    fun `releases are published to the release repository and snapshots to the snapshot repository when set`() {
        publishedModule(
            """
            weaver { publishing { snapshotRepository = 'proto-snapshots' } }
            """
        )
        contract()
        stub.acceptPublish("dev.slne.demo", "demo", "1.2.0-SNAPSHOT", repository = "proto-snapshots")

        build.run("publishProtos")

        assertEquals(
            "/api/v1/repos/proto-snapshots/artifacts/dev.slne.demo/demo/1.2.0-SNAPSHOT",
            stub.requests("PUT").single().path,
        )
    }

    @Test
    fun `publishProtos runs after the publishProtos of local weaver modules without depending on it`() {
        build.settings("a", "b")
        build.script("allprojects { group = 'dev.slne.demo'; version = '1.0.0' }")
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${TestBuild.PUBLISHING}
            weaver { publishing { name = 'a' } }
            """,
            project = "a",
        )
        build.proto("surf/a/v1/a.proto", TestBuild.contract("message A { optional int64 id = 1; }", pkg = "surf.a.v1"), project = "a")
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${TestBuild.PUBLISHING}
            weaver { publishing { name = 'b' } }
            dependencies { weaver project(':a') }
            """,
            project = "b",
        )
        build.proto("surf/b/v1/b.proto", TestBuild.contract("message B { optional int64 id = 1; }", pkg = "surf.b.v1"), project = "b")

        val plan = build.plan("publish", "--parallel")
        assertBefore(plan, ":a:publishProtos", ":b:publishProtos")

        val reversed = build.plan(":b:publishProtos", ":a:publishProtos")
        assertBefore(reversed, ":a:publishProtos", ":b:publishProtos")

        val alone = build.plan(":b:publishProtos")
        assertFalse(":a:publishProtos" in alone, alone.toString())
    }
}

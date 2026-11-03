package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.testkit.TestBuild
import dev.slne.surf.nats.gradle.testkit.TestKitSupport
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class WeaverImportWiringTest : TestKitSupport() {

    private val consumerTasks = arrayOf("generateSurfNatsKotlin", "checkSurfNatsCompatibility")

    private fun singleProject(dependencies: String) {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            dependencies { $dependencies }
            """
        )
    }

    private fun twoProjects() {
        build.settings("a", "b")
        build.script("allprojects { group = 'dev.slne.demo'; version = '1.0.0' }")
        build.script(TestBuild.PLUGIN, project = "a")
        build.proto("surf/a/v1/a.proto", TestBuild.contract("message Tag { optional string value = 1; }", pkg = "surf.a.v1"), project = "a")
        build.script(
            """
            ${TestBuild.PLUGIN}
            dependencies { weaver project(':a') }
            surfNats { mapType('surf.a.v1.Tag', 'dev.demo.Tag', 'dev.demo.TagSerializer') }
            """,
            project = "b",
        )
        build.proto(
            "surf/b/v1/b.proto",
            """
            syntax = "proto2";
            package surf.b.v1;
            import "surf/a/v1/a.proto";
            message Holder { optional surf.a.v1.Tag tag = 1; }
            """.trimIndent(),
            project = "b",
        )
    }

    @Test
    fun `without a weaver dependency the code generation does not fetch protos`() {
        singleProject("")

        val plan = build.plan(*consumerTasks)

        assertContains(plan, ":generateSurfNatsKotlin")
        assertContains(plan, ":checkSurfNatsCompatibility")
        assertFalse(":fetchProtos" in plan, plan.toString())
    }

    @Test
    fun `an external weaver dependency makes generation and the check fetch protos first`() {
        singleProject("weaver 'dev.slne.demo:demo:1.0.0'")

        val plan = build.plan(*consumerTasks)

        assertContains(plan, ":fetchProtos")
        assertTrue(plan.indexOf(":fetchProtos") < plan.indexOf(":generateSurfNatsKotlin"), plan.toString())
        assertTrue(plan.indexOf(":fetchProtos") < plan.indexOf(":checkSurfNatsCompatibility"), plan.toString())
    }

    @Test
    fun `fetchProtos is only attached to the task whose weaver configuration has a dependency`() {
        twoProjects()

        val plan = build.plan(":b:generateSurfNatsKotlin", ":a:generateSurfNatsKotlin")

        assertContains(plan, ":b:fetchProtos")
        assertFalse(":a:fetchProtos" in plan, plan.toString())
    }

    @Test
    fun `a project dependency stages the other project and fetches its protos before generation`() {
        twoProjects()

        val plan = build.plan(":b:generateSurfNatsKotlin")

        for (task in listOf(":a:stageSurfNatsWeaverSource", ":a:weaverManifest", ":b:fetchProtos")) {
            assertContains(plan, task)
            assertTrue(plan.indexOf(task) < plan.indexOf(":b:generateSurfNatsKotlin"), "$task in $plan")
        }
        assertTrue(plan.indexOf(":a:stageSurfNatsWeaverSource") < plan.indexOf(":b:fetchProtos"), plan.toString())
        assertFalse(":a:generateSurfNatsKotlin" in plan, "importing must not generate the other project: $plan")
    }

    @Test
    fun `the automatic types dependency of the publish manifest does not make generation fetch protos`() {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${TestBuild.PUBLISHING}
            """
        )

        val plan = build.plan("generateSurfNatsKotlin", "publishProtos")

        assertFalse(":fetchProtos" in plan, plan.toString())
    }

    @Test
    fun `a project dependency makes the protos of the other project importable offline and without registry access`() {
        twoProjects()
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${weaverStub()}
            dependencies { weaver project(':a') }
            surfNats { mapType('surf.a.v1.Tag', 'dev.demo.Tag', 'dev.demo.TagSerializer') }
            """,
            project = "b",
        )

        build.run("--offline", ":b:generateSurfNatsKotlin")

        val generated = build.resolve("b/build/generated/surf-nats/kotlin").walkTopDown().filter { it.extension == "kt" }.toList()
        val holder = generated.single { "Holder" in it.readText() }.readText()
        assertContains(holder, "Tag")
        assertFalse(generated.any { "class Tag" in it.readText() }, "the imported message must not be generated in :b")
        assertTrue(
            build.resolve("b/build/weaver/protos/surf/a/v1/a.proto").isFile,
            "fetchProtos copies the protos of :a into the weaver proto root",
        )
        assertFalse(build.resolve("a/build/generated").exists(), ":a is not generated by importing it")
        assertEquals(emptyList(), stub.requests.toList(), "no request must reach the registry")
    }

    @Test
    fun `two unpublished project dependencies do not collide on a default coordinate`() {
        build.settings("a", "c", "b")
        build.script("allprojects { group = 'dev.slne.demo'; version = '1.0.0' }")
        build.script(TestBuild.PLUGIN, project = "a")
        build.proto("surf/a/v1/a.proto", TestBuild.contract("message Tag { optional string value = 1; }", pkg = "surf.a.v1"), project = "a")
        build.script(TestBuild.PLUGIN, project = "c")
        build.proto("surf/c/v1/c.proto", TestBuild.contract("message Label { optional string value = 1; }", pkg = "surf.c.v1"), project = "c")
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${weaverStub()}
            dependencies { weaver project(':a'); weaver project(':c') }
            surfNats {
                mapType('surf.a.v1.Tag', 'dev.demo.Tag', 'dev.demo.TagSerializer')
                mapType('surf.c.v1.Label', 'dev.demo.Label', 'dev.demo.LabelSerializer')
            }
            """,
            project = "b",
        )
        build.proto(
            "surf/b/v1/b.proto",
            """
            syntax = "proto2";
            package surf.b.v1;
            import "surf/a/v1/a.proto";
            import "surf/c/v1/c.proto";
            message Holder { optional surf.a.v1.Tag tag = 1; optional surf.c.v1.Label label = 2; }
            """.trimIndent(),
            project = "b",
        )

        build.run("--offline", ":b:generateSurfNatsKotlin")

        assertTrue(build.resolve("b/build/weaver/protos/surf/a/v1/a.proto").isFile)
        assertTrue(build.resolve("b/build/weaver/protos/surf/c/v1/c.proto").isFile)
        assertEquals(emptyList(), stub.requests.toList(), "no request must reach the registry")
    }

    @Test
    fun `without the weaver dependency the same proto fails on its unresolved import`() {
        twoProjects()
        build.script(
            """
            ${TestBuild.PLUGIN}
            surfNats { mapType('surf.a.v1.Tag', 'dev.demo.Tag', 'dev.demo.TagSerializer') }
            """,
            project = "b",
        )

        val result = build.runAndFail("--offline", ":b:generateSurfNatsKotlin")

        assertContains(result.output, "surf/a/v1/a.proto")
    }

    @Test
    fun `imported types still need a type mapping`() {
        twoProjects()
        build.script(
            """
            ${TestBuild.PLUGIN}
            dependencies { weaver project(':a') }
            """,
            project = "b",
        )

        val result = build.runAndFail("--offline", ":b:generateSurfNatsKotlin")

        assertContains(result.output, "surf.a.v1.Tag from surf/a/v1/a.proto is neither generated nor mapped")
    }

    @Test
    fun `a change in the imported project reruns the generation of the importing project`() {
        twoProjects()
        build.run("--offline", ":b:generateSurfNatsKotlin")

        build.proto(
            "surf/a/v1/a.proto",
            TestBuild.contract("message Tag { optional string value = 1; }\nmessage Other { optional string value = 1; }", pkg = "surf.a.v1"),
            project = "a",
        )
        val result = build.run("--offline", ":b:generateSurfNatsKotlin")

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":b:generateSurfNatsKotlin")?.outcome,
            result.output,
        )
    }
}

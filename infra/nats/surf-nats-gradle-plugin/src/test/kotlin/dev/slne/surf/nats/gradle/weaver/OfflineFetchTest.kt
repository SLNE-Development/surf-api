package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.testkit.TestBuild
import dev.slne.surf.nats.gradle.testkit.TestKitSupport
import dev.slne.surf.nats.gradle.testkit.outcome
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class OfflineFetchTest : TestKitSupport() {

    private fun externalDependency() {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${weaverStub()}
            dependencies { weaver 'dev.slne.demo:demo:1.0.0' }
            """
        )
    }

    private fun localDependency(externalInA: String? = null) {
        build.settings("a", "b")
        build.script("allprojects { group = 'dev.slne.demo'; version = '1.0.0' }")
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${externalInA?.let { "dependencies { weaver '$it' }" }.orEmpty()}
            """,
            project = "a",
        )
        build.proto("surf/a/v1/a.proto", TestBuild.contract("message Tag { optional string value = 1; }", pkg = "surf.a.v1"), project = "a")
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${weaverStub()}
            dependencies { weaver project(':a') }
            """,
            project = "b",
        )
    }

    @Test
    fun `offline an external weaver dependency fails fetchProtos with the surf-nats message`() {
        externalDependency()

        val result = build.runAndFail("--offline", "fetchProtos")

        assertEquals(TaskOutcome.FAILED, result.outcome(":fetchProtos"))
        assertContains(
            result.output,
            "surf-nats: weaver dependencies dev.slne.demo:demo:1.0.0 need the network, but Gradle runs with --offline",
        )
        assertEquals(emptyList(), stub.requests.toList(), "the guard fails before any request")
    }

    @Test
    fun `offline a project dependency alone fetches without the network`() {
        localDependency()

        val result = build.run("--offline", ":b:fetchProtos")

        assertEquals(TaskOutcome.SUCCESS, result.outcome(":b:fetchProtos"))
        assertTrue(build.resolve("b/build/weaver/protos/surf/a/v1/a.proto").isFile)
        assertEquals(emptyList(), stub.requests.toList())
    }

    @Test
    fun `online a project dependency alone does not contact the registry either`() {
        localDependency()

        build.run(":b:fetchProtos")

        assertEquals(emptyList(), stub.requests.toList())
    }

    @Test
    fun `offline an external dependency of a local module is reported through its manifest`() {
        localDependency(externalInA = "dev.slne.demo:remote:2.0.0")

        val result = build.runAndFail("--offline", ":b:fetchProtos")

        assertContains(result.output, "surf-nats: weaver dependencies dev.slne.demo:remote:2.0.0 need the network")
    }

    @Test
    fun `offline an external root that a local module publishes is substituted without the network`() {
        build.settings("a", "b", "c")
        build.script("allprojects { group = 'dev.slne.demo'; version = '1.0.0' }")
        build.script(
            """
            ${TestBuild.PLUGIN}
            weaver { publishing { name = 'a' } }
            dependencies { weaver 'dev.slne.demo:c:1.0.0' }
            """,
            project = "a",
        )
        build.proto("surf/a/v1/a.proto", TestBuild.contract("message Tag { optional string value = 1; }", pkg = "surf.a.v1"), project = "a")
        build.script(
            """
            ${TestBuild.PLUGIN}
            weaver { publishing { name = 'c' } }
            """,
            project = "c",
        )
        build.proto("surf/c/v1/c.proto", TestBuild.contract("message Code { optional string value = 1; }", pkg = "surf.c.v1"), project = "c")
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${weaverStub()}
            dependencies { weaver project(':a'); weaver project(':c') }
            """,
            project = "b",
        )

        val result = build.run("--offline", ":b:fetchProtos")

        assertEquals(TaskOutcome.SUCCESS, result.outcome(":b:fetchProtos"))
        assertTrue(build.resolve("b/build/weaver/protos/surf/c/v1/c.proto").isFile)
        assertEquals(emptyList(), stub.requests.toList())
    }

    @Test
    fun `online an external dependency is resolved against the registry`() {
        externalDependency()
        stub.acceptResolve()

        val result = build.run("fetchProtos")

        assertEquals(TaskOutcome.SUCCESS, result.outcome(":fetchProtos"))
        val resolve = stub.requests.single { it.path == "/api/v1/resolve" }
        assertEquals("POST", resolve.method)
        assertContains(String(resolve.body), "dev.slne.demo:demo:1.0.0")
        assertFalse(result.output.contains("need the network"))
    }
}

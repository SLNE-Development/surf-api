package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.SurfNatsPluginVersion
import dev.slne.surf.nats.gradle.testkit.StubRequest
import dev.slne.surf.nats.gradle.testkit.TestBuild
import dev.slne.surf.nats.gradle.testkit.TestBuild.Companion.CONTRACT_PATH
import dev.slne.surf.nats.gradle.testkit.TestKitSupport
import dev.slne.surf.nats.gradle.testkit.shown
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The manifest `publishProtos` sends to the registry, as the stub receives it.
 */
internal class PublishManifestTest : TestKitSupport() {

    private val common = "${SurfNatsPluginVersion.GROUP}:surf-nats-types-common:${SurfNatsPluginVersion.VERSION}"

    private fun publishedModule(dependencies: String = "") {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${TestBuild.PUBLISHING}
            ${weaverStub()}
            group = 'dev.slne.demo'
            version = '1.2.0'
            dependencies { $dependencies }
            ${TestBuild.showTask(
                "weaverConfiguration" to "configurations.weaver.dependencies.collect { it.group + ':' + it.name + ':' + it.version }",
                "fetchDependencies" to "tasks.fetchProtos.dependencies.get()",
            )}
            """
        )
        build.proto(CONTRACT_PATH, TestBuild.contract("message Player { required int64 id = 1; }"))
        stub.acceptPublish("dev.slne.demo", "demo", "1.2.0")
        stub.acceptResolve()
    }

    private fun manifestDependencies(request: StubRequest): List<String> =
        Json.parseToJsonElement(String(assertNotNull(request.part("manifest")))).jsonObject
            .getValue("dependencies").jsonArray.map { it.jsonPrimitive.content }

    @Test
    fun `the manifest declares the common types and the weaver configuration stays empty`() {
        publishedModule()

        build.run("publishProtos")

        assertEquals(listOf(common), manifestDependencies(stub.requests("PUT").single()))
        val shown = build.run("show", "-q").shown()
        assertEquals("[]", shown["weaverConfiguration"])
        assertEquals("[]", shown["fetchDependencies"])
        assertEquals(emptyList(), stub.requests("POST"), "without weaver dependencies nothing is resolved")
    }

    @Test
    fun `declared dependencies are listed with the common types`() {
        publishedModule("weaver 'dev.slne.demo:other:2.0.0'")

        build.run("publishProtos")

        assertEquals(
            listOf("dev.slne.demo:other:2.0.0", common),
            manifestDependencies(stub.requests("PUT").single()),
        )
        assertEquals("[dev.slne.demo:other:2.0.0]", build.run("show", "-q").shown()["weaverConfiguration"])
    }

    @Test
    fun `a user declared version of the types module replaces the automatic one`() {
        publishedModule("weaver '${SurfNatsPluginVersion.GROUP}:surf-nats-types-common:0.0.1'")

        build.run("publishProtos")

        assertEquals(
            listOf("${SurfNatsPluginVersion.GROUP}:surf-nats-types-common:0.0.1"),
            manifestDependencies(stub.requests("PUT").single()),
        )
    }
}

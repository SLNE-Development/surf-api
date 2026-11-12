package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.testkit.TestBuild
import dev.slne.surf.nats.gradle.testkit.TestKitSupport
import dev.slne.surf.nats.gradle.testkit.shown
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class WeaverConventionsTest : TestKitSupport() {

    private val showWeaver = TestBuild.showTask(
        "url" to "weaver.url.getOrNull()",
        "token" to "weaver.token.getOrNull()",
        "repositories" to "weaver.repositories.get()",
        "repository" to "weaver.publishing.repository.getOrNull()",
        "snapshotRepository" to "weaver.publishing.snapshotRepository.getOrNull()",
        "group" to "weaver.publishing.group.getOrNull()",
        "name" to "weaver.publishing.name.getOrNull()",
        "version" to "weaver.publishing.version.getOrNull()",
    )

    private fun show(environment: Map<String, String> = emptyMap()): Map<String, String> =
        build.run("show", "-q", environment = environment).shown()

    @Test
    fun `without environment the conventions point at the public registry and the project coordinates`() {
        build.settings(rootName = "demo-root")
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${TestBuild.PUBLISHING}
            group = 'dev.slne.demo'
            version = '1.2.0-SNAPSHOT'
            $showWeaver
            """
        )

        val shown = show()

        assertEquals("https://weaver.slne.dev", shown["url"])
        assertEquals("null", shown["token"])
        assertEquals("[proto-releases-public]", shown["repositories"])
        assertEquals("proto-releases-public", shown["repository"])
        assertEquals("null", shown["snapshotRepository"])
        assertEquals("dev.slne.demo", shown["group"])
        assertEquals("demo-root", shown["name"])
        assertEquals("1.2.0-SNAPSHOT", shown["version"])
    }

    @Test
    fun `group and version are read lazily from the project`() {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            // Set after the plugin was applied and after weaver { } was configured.
            weaver { publishing { repository = 'private' } }
            group = 'dev.slne.late'
            version = '3.4.5'
            $showWeaver
            """
        )

        val shown = show()

        assertEquals("dev.slne.late", shown["group"])
        assertEquals("3.4.5", shown["version"])
        assertEquals("private", shown["repository"])
    }

    @Test
    fun `an unset project group leaves the weaver group absent`() {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            version = '1.0.0'
            $showWeaver
            """
        )

        assertEquals("null", show()["group"])
    }

    @Test
    fun `the WEAVER_URL and WEAVER_TOKEN environment variables win over the default url and no token`() {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            $showWeaver
            """
        )

        val shown = show(mapOf("WEAVER_URL" to "http://env.example:1234", "WEAVER_TOKEN" to "env-token"))

        assertEquals("http://env.example:1234", shown["url"])
        assertEquals("env-token", shown["token"])
    }

    @Test
    fun `the weaver block overrides every convention, also over the environment`() {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            group = 'dev.slne.demo'
            version = '1.2.0'
            weaver {
                url = 'http://dsl.example'
                token = 'dsl-token'
                repositories('first', 'second')
                publishing {
                    repository = 'private-releases'
                    snapshotRepository = 'private-snapshots'
                    group = 'dev.slne.custom'
                    name = 'custom-name'
                    version = '9.9.9'
                }
            }
            $showWeaver
            """
        )

        val shown = show(mapOf("WEAVER_URL" to "http://env.example:1234", "WEAVER_TOKEN" to "env-token"))

        assertEquals("http://dsl.example", shown["url"])
        assertEquals("dsl-token", shown["token"])
        assertEquals("[first, second]", shown["repositories"])
        assertEquals("private-releases", shown["repository"])
        assertEquals("private-snapshots", shown["snapshotRepository"])
        assertEquals("dev.slne.custom", shown["group"])
        assertEquals("custom-name", shown["name"])
        assertEquals("9.9.9", shown["version"])
    }

    @Test
    fun `the weaver name defaults to the root project name in published subprojects`() {
        build.settings("sub", rootName = "demo-root")
        build.script("allprojects { group = 'dev.slne.demo'; version = '1.0.0' }")
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${TestBuild.PUBLISHING}
            $showWeaver
            """,
            project = "sub",
        )

        val shown = build.run(":sub:show", "-q").shown()

        assertEquals("demo-root", shown["name"])
        assertEquals("dev.slne.demo", shown["group"])
    }

    @Test
    fun `unpublished modules have no default weaver name`() {
        build.settings("a", "b", rootName = "demo-root")
        build.script("allprojects { group = 'dev.slne.demo'; version = '1.0.0' }")
        for (project in listOf("a", "b")) build.script("${TestBuild.PLUGIN}\n$showWeaver", project = project)

        assertEquals("null", build.run(":a:show", "-q").shown()["name"])
        assertEquals("null", build.run(":b:show", "-q").shown()["name"])
    }

    @Test
    fun `an unset group in subprojects leaves the weaver group absent instead of the derived default`() {
        build.settings("a", "b:c", rootName = "my-root")
        build.script("allprojects { version = '1.0.0' }")
        for (project in listOf("a", "b/c")) build.script("${TestBuild.PLUGIN}\n$showWeaver", project = project)

        assertEquals("null", build.run(":a:show", "-q").shown()["group"])
        assertEquals("null", build.run(":b:c:show", "-q").shown()["group"])
    }

    @Test
    fun `an explicit group in a subproject is kept`() {
        build.settings("a", rootName = "my-root")
        build.script("${TestBuild.PLUGIN}\ngroup = 'dev.slne.sub'\n$showWeaver", project = "a")

        assertEquals("dev.slne.sub", build.run(":a:show", "-q").shown()["group"])
    }
}

package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.SurfNatsPlugin
import dev.slne.surf.nats.gradle.testkit.*
import dev.slne.surf.nats.gradle.testkit.TestBuild.Companion.CONTRACT_PATH
import dev.slne.weaver.gradle.WeaverExtension
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class WeaverSourceStagingTest : TestKitSupport() {

    @TempDir
    lateinit var projectDir: File

    private val staged get() = build.resolve("build/surf-nats/weaver-source")

    private fun stagedFiles(): Set<String> =
        staged.walkTopDown().filter { it.isFile }.map { it.relativeTo(staged).invariantSeparatorsPath }.toSet()

    private fun twoSourceDirectories() {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            surfNats { protoSourceDirs.from('src/extra/proto') }
            """
        )
        build.proto(CONTRACT_PATH, TestBuild.contract("message Main { required string a = 1; }"))
        build.file("src/main/proto/README.txt", "not a proto")
        build.file("src/extra/proto/surf/extra/v1/extra.proto", TestBuild.contract("message Extra { required string a = 1; }"))
        build.file("src/extra/proto/notes.md", "not a proto")
    }

    @Test
    fun `stageSurfNatsWeaverSource copies the protos of every source directory and nothing else`() {
        twoSourceDirectories()

        val result = build.run(SurfNatsPlugin.STAGE_TASK)

        assertEquals(TaskOutcome.SUCCESS, result.outcome(":${SurfNatsPlugin.STAGE_TASK}"))
        assertEquals(setOf(CONTRACT_PATH, "surf/extra/v1/extra.proto"), stagedFiles())
    }

    @Test
    fun `stageSurfNatsWeaverSource removes staged files whose source is gone`() {
        twoSourceDirectories()
        build.run(SurfNatsPlugin.STAGE_TASK)

        build.resolve("src/extra/proto/surf/extra/v1/extra.proto").delete()
        build.run(SurfNatsPlugin.STAGE_TASK)

        assertEquals(setOf(CONTRACT_PATH), stagedFiles())
    }

    @Test
    fun `weaver publishing source is the staged directory and packProtos packs it after staging`() {
        twoSourceDirectories()
        build.script(
            """
            ${TestBuild.PLUGIN}
            surfNats { protoSourceDirs.from('src/extra/proto') }
            ${TestBuild.showTask("source" to "weaver.publishing.source.get().asFile.absolutePath")}
            """
        )

        val shown = build.run("show", "-q").shown()
        assertEquals(staged.canonicalFile, File(shown.getValue("source")).canonicalFile)

        val result = build.run("packProtos")
        assertEquals(TaskOutcome.SUCCESS, result.outcome(":${SurfNatsPlugin.STAGE_TASK}"))
        assertEquals(
            setOf(CONTRACT_PATH, "surf/extra/v1/extra.proto"),
            TarGz.names(build.resolve("build/weaver/publish/protos.tar.gz")).toSet(),
        )
    }

    @Test
    fun `weaver publishing source cannot be changed`() {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply(SurfNatsPlugin::class.java)
        val weaver = project.extensions.getByType(WeaverExtension::class.java)

        assertFailsWith<IllegalStateException> { weaver.publishing.source.set(projectDir.resolve("other")) }
        assertFailsWith<IllegalStateException> { weaver.publishing.source.fileValue(projectDir.resolve("other")) }
        assertTrue(weaver.publishing.source.get().asFile.path.replace('\\', '/').endsWith("build/surf-nats/weaver-source"))
        assertFalse(weaver.publishing.source.get().asFile.path.replace('\\', '/').endsWith("src/main/proto"))
    }
}

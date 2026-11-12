package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.SurfNatsPlugin
import dev.slne.surf.nats.gradle.testkit.TestBuild
import dev.slne.surf.nats.gradle.testkit.TestBuild.Companion.CONTRACT_PATH
import dev.slne.surf.nats.gradle.testkit.TestKitSupport
import org.gradle.api.tasks.bundling.Tar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Protos reach consumers through weaver; no jar carries them in `META-INF/surf-nats/proto` anymore.
 */
internal class NoJarProtoPackagingTest : TestKitSupport() {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `the jar of a project with the plugin contains no packed protos`() {
        build.settings()
        build.script(
            """
            plugins { id 'java'; id 'dev.slne.surf.nats' }
            """
        )
        build.proto(CONTRACT_PATH, TestBuild.contract("message Player { required int64 id = 1; }"))
        build.file("src/main/resources/app.properties", "key=value")

        build.run("jar")

        val jar = build.resolve("build/libs").listFiles().orEmpty().single { it.extension == "jar" }
        val entries = ZipFile(jar).use { zip -> zip.entries().asSequence().map { it.name }.toList() }
        assertTrue("app.properties" in entries, entries.toString())
        assertFalse(entries.any { it.startsWith("META-INF/surf-nats") }, entries.toString())
        assertFalse(entries.any { it.endsWith(".proto") }, entries.toString())
        assertFalse(build.resolve("build/resources/main/META-INF/surf-nats").exists())
    }

    @Test
    fun `the jar and resource tasks do not depend on staging or packing protos`() {
        build.settings()
        build.script(
            """
            plugins { id 'java'; id 'dev.slne.surf.nats' }
            """
        )

        val plan = build.plan("jar")

        assertFalse(plan.any { it.endsWith("packProtos") || it.endsWith(SurfNatsPlugin.STAGE_TASK) }, plan.toString())
    }

    @Test
    fun `the jar configurations and archive helpers of the jar based baseline are gone`() {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply(SurfNatsPlugin::class.java)

        assertNull(project.configurations.findByName("surfNatsProtoPath"))
        assertNull(project.configurations.findByName("surfNatsCompatibilityBaseline"))
        assertFailsWith<ClassNotFoundException> { Class.forName("dev.slne.surf.nats.gradle.tasks.ProtoArchives") }
        assertFailsWith<ClassNotFoundException> { Class.forName("dev.slne.surf.nats.gradle.tasks.ProtoArchiveTransform") }
        assertEquals("weaver", project.tasks.getByName("packProtos").group, "packProtos is weaver's own task")
        assertTrue(project.tasks.getByName("packProtos") is Tar)
    }
}

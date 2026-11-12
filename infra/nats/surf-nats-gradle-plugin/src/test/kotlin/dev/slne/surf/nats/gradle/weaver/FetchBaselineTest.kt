package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.testkit.StubResponse
import dev.slne.surf.nats.gradle.testkit.TarGz
import dev.slne.surf.nats.gradle.testkit.TestBuild
import dev.slne.surf.nats.gradle.testkit.TestBuild.Companion.CONTRACT_PATH
import dev.slne.surf.nats.gradle.testkit.TestKitSupport
import dev.slne.surf.nats.gradle.testkit.WeaverStub
import dev.slne.surf.nats.gradle.testkit.outcome
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FetchBaselineTest : TestKitSupport() {

    private val task = ":fetchSurfNatsBaseline"
    private val group = "dev.slne.demo"
    private val name = "demo"
    private val versionsPath = WeaverStub.artifactPath(WeaverStub.REPOSITORY, group, name)

    private val baselineDir get() = build.resolve("build/surf-nats/baseline")
    private val recordedVersion get() = baselineDir.resolve("version.txt").readText()

    private fun module(extra: String = "", published: Boolean = true, version: String = "1.2.0-SNAPSHOT") {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${if (published) TestBuild.PUBLISHING else ""}
            ${weaverStub()}
            group = '$group'
            version = '$version'
            $extra
            """
        )
        build.proto(CONTRACT_PATH, TestBuild.contract("message Player { required int64 id = 1; }"))
    }

    private fun serveArchive(version: String, entries: Map<String, String>, sha256: String? = null) {
        val bytes = TarGz.create(entries)
        stub.archive(group, name, version, bytes, sha256 = sha256 ?: WeaverStub.sha256(bytes))
    }

    private fun baselineContract(version: String) =
        mapOf(CONTRACT_PATH to TestBuild.contract("message Player { required int64 id = 1; } // baseline $version"))

    private fun baselineFile(): File = baselineDir.resolve("protos/$CONTRACT_PATH")

    private fun extractedFiles(): Set<String> {
        val root = baselineDir.resolve("protos")
        return root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).invariantSeparatorsPath }.toSet()
    }

    /**
     * Gradle's own tar reader rejects such names before the task's check runs; either message is a rejection.
     */
    private fun assertRejectedAsUnsafe(output: String) {
        assertTrue(
            "escapes the baseline directory" in output || "is not a safe archive entry" in output,
            output,
        )
    }

    private fun paths() = stub.requests.map { "${it.method} ${it.path}" }

    @Test
    fun `picks the highest release below the project version and ignores snapshots and higher versions`() {
        module()
        stub.versions(
            group, name,
            "0.9.0" to false, "1.0.0" to false, "1.1.0" to false, "1.1.5-SNAPSHOT" to true,
            "1.2.0" to false, "1.3.0" to false, "2.0.0" to false,
        )
        for (version in listOf("0.9.0", "1.0.0", "1.1.0", "1.3.0")) serveArchive(version, baselineContract(version))

        val result = build.run("fetchSurfNatsBaseline")

        assertEquals(TaskOutcome.SUCCESS, result.outcome(task))
        assertEquals("1.1.0", recordedVersion)
        assertContains(baselineFile().readText(), "baseline 1.1.0")
        assertEquals(listOf("GET $versionsPath", "GET $versionsPath/1.1.0/archive"), paths())
    }

    @Test
    fun `numeric parts compare numerically`() {
        module(version = "2.0.0")
        stub.versions(group, name, "1.9.0" to false, "1.10.0" to false, "1.2.0" to false)
        serveArchive("1.10.0", baselineContract("1.10.0"))

        build.run("fetchSurfNatsBaseline")

        assertEquals("1.10.0", recordedVersion)
    }

    @Test
    fun `a pre-release of the current version is an earlier release`() {
        module()
        stub.versions(group, name, "1.1.0" to false, "1.2.0-rc.1" to false, "1.2.0" to false)
        serveArchive("1.2.0-rc.1", baselineContract("1.2.0-rc.1"))

        build.run("fetchSurfNatsBaseline")

        assertEquals("1.2.0-rc.1", recordedVersion)
    }

    @Test
    fun `an explicit compatibilityBaseline replaces the lookup, also for unpublished modules`() {
        module(extra = "surfNats { compatibilityBaseline('1.0.0') }", published = false)
        serveArchive("1.0.0", baselineContract("1.0.0"))
        serveArchive("1.1.0", baselineContract("1.1.0"))

        build.run("fetchSurfNatsBaseline")

        assertEquals("1.0.0", recordedVersion)
        assertContains(baselineFile().readText(), "baseline 1.0.0")
        assertEquals(listOf("GET $versionsPath/1.0.0/archive"), paths())
    }

    @Test
    fun `404 for the artifact means no baseline and skips the check without an error`() {
        module()

        val result = build.run("checkSurfNatsCompatibility")

        assertEquals(TaskOutcome.SUCCESS, result.outcome(task))
        assertEquals(TaskOutcome.SKIPPED, result.outcome(":checkSurfNatsCompatibility"))
        assertEquals("none", recordedVersion)
        assertContains(result.output, "no earlier release of $group:$name in ${WeaverStub.REPOSITORY}")
        assertEquals(listOf("GET $versionsPath"), paths())
    }

    @Test
    fun `404 in a non-default repository without a token warns that the repository may be private`() {
        module(extra = "weaver { publishing { repository = 'private-releases' } }")

        val result = build.run("fetchSurfNatsBaseline")

        assertEquals("none", recordedVersion)
        assertContains(result.output, "no earlier release of $group:$name in private-releases")
        assertContains(result.output, "may be private while WEAVER_TOKEN is not set")
    }

    @Test
    fun `404 in the default repository does not warn about a private repository`() {
        module()

        val result = build.run("fetchSurfNatsBaseline")

        assertFalse("may be private" in result.output, result.output)
    }

    @Test
    fun `no release below the project version means no baseline`() {
        module(version = "1.0.0")
        stub.versions(group, name, "1.0.0" to false, "1.1.0" to false, "0.9.0-SNAPSHOT" to true)

        build.run("fetchSurfNatsBaseline")

        assertEquals("none", recordedVersion)
        assertEquals(listOf("GET $versionsPath"), paths())
        assertEquals(emptyList(), baselineDir.resolve("protos").listFiles().orEmpty().toList())
    }

    @Test
    fun `a checksum mismatch fails the download and leaves no archive behind`() {
        module()
        stub.versions(group, name, "1.1.0" to false)
        serveArchive("1.1.0", baselineContract("1.1.0"), sha256 = "0".repeat(64))

        val result = build.runAndFail("fetchSurfNatsBaseline")

        assertEquals(TaskOutcome.FAILED, result.outcome(task))
        assertContains(result.output, "Checksum mismatch for $group:$name:1.1.0")
        assertFalse(baselineFile().exists())
    }

    @Test
    fun `an archive without a checksum header is accepted`() {
        module()
        stub.versions(group, name, "1.1.0" to false)
        stub.archive(group, name, "1.1.0", TarGz.create(baselineContract("1.1.0")), sha256 = null)

        build.run("fetchSurfNatsBaseline")

        assertEquals("1.1.0", recordedVersion)
        assertTrue(baselineFile().isFile)
    }

    @Test
    fun `an archive entry that escapes the baseline directory is rejected`() {
        module()
        stub.versions(group, name, "1.1.0" to false)
        serveArchive("1.1.0", baselineContract("1.1.0") + ("../evil.proto" to "syntax = \"proto2\";"))

        val result = build.runAndFail("fetchSurfNatsBaseline", "--stacktrace")

        assertEquals(TaskOutcome.FAILED, result.outcome(task))
        assertRejectedAsUnsafe(result.output)
        assertFalse(baselineDir.resolve("evil.proto").exists())
        assertFalse(build.resolve("build/surf-nats/evil.proto").exists())
    }

    @Test
    fun `an entry with a parent segment in the middle is rejected too`() {
        module()
        stub.versions(group, name, "1.1.0" to false)
        serveArchive("1.1.0", mapOf("surf/../../../evil.proto" to "syntax = \"proto2\";"))

        val result = build.runAndFail("fetchSurfNatsBaseline", "--stacktrace")

        assertRejectedAsUnsafe(result.output)
        assertFalse(build.resolve("build/surf-nats/evil.proto").exists())
    }

    @Test
    fun `only proto files are extracted and files of an earlier baseline are removed`() {
        module()
        stub.versions(group, name, "1.1.0" to false)
        serveArchive("1.1.0", baselineContract("1.1.0") + mapOf("README.txt" to "x", "surf/demo/v1/old.proto" to "syntax = \"proto2\";"))
        build.run("fetchSurfNatsBaseline")
        assertEquals(setOf(CONTRACT_PATH, "surf/demo/v1/old.proto"), extractedFiles())

        // Incompressible and therefore larger than the first archive, so a leftover download file cannot matter.
        serveArchive("1.1.0", mapOf(CONTRACT_PATH to TestBuild.contract("message Player { required int64 id = 1; } // $NOISE")))
        build.run("fetchSurfNatsBaseline")

        assertEquals(setOf(CONTRACT_PATH), extractedFiles())
    }

    @Test
    fun `a smaller archive downloaded after a larger one is not corrupted by the earlier download`() {
        module()
        stub.versions(group, name, "1.1.0" to false)
        serveArchive("1.1.0", mapOf(CONTRACT_PATH to TestBuild.contract("message Player { required int64 id = 1; } // $NOISE")))
        build.run("fetchSurfNatsBaseline")

        serveArchive("1.1.0", baselineContract("1.1.0"))
        val result = build.run("fetchSurfNatsBaseline")

        assertEquals(TaskOutcome.SUCCESS, result.outcome(task))
        assertContains(baselineFile().readText(), "baseline 1.1.0")
    }

    @Test
    fun `offline the task is skipped with a warning and records no baseline`() {
        module()

        val result = build.run("--offline", "fetchSurfNatsBaseline")

        assertEquals(TaskOutcome.SUCCESS, result.outcome(task))
        assertContains(result.output, "Gradle runs with --offline, skipping the compatibility check against weaver")
        assertEquals("none", recordedVersion)
        assertEquals(emptyList(), stub.requests.toList())
    }

    @Test
    fun `offline the compatibility check is skipped without failing`() {
        module(extra = "surfNats { compatibilityBaseline('1.0.0') }")

        val result = build.run("--offline", "checkSurfNatsCompatibility")

        assertEquals(TaskOutcome.SKIPPED, result.outcome(":checkSurfNatsCompatibility"))
        assertEquals(emptyList(), stub.requests.toList())
    }

    @Test
    fun `the token from the environment is sent as a bearer header on every request`() {
        module()
        stub.versions(group, name, "1.1.0" to false)
        serveArchive("1.1.0", baselineContract("1.1.0"))

        build.run("fetchSurfNatsBaseline", environment = mapOf("WEAVER_TOKEN" to "env-secret"))

        assertEquals(2, stub.requests.size)
        assertEquals(listOf("Bearer env-secret"), stub.requests.map { it.header("authorization") }.distinct())
    }

    @Test
    fun `the token of the weaver block wins over the environment`() {
        module(extra = "weaver { token = 'dsl-secret' }")
        stub.versions(group, name, "1.1.0" to false)
        serveArchive("1.1.0", baselineContract("1.1.0"))

        build.run("fetchSurfNatsBaseline", environment = mapOf("WEAVER_TOKEN" to "env-secret"))

        assertEquals(listOf("Bearer dsl-secret"), stub.requests.map { it.header("authorization") }.distinct())
    }

    @Test
    fun `without a token no authorization header is sent`() {
        module()
        stub.versions(group, name, "1.1.0" to false)
        serveArchive("1.1.0", baselineContract("1.1.0"))

        build.run("fetchSurfNatsBaseline")

        assertEquals(2, stub.requests.size)
        assertTrue(stub.requests.all { it.header("authorization") == null })
    }

    @Test
    fun `lookup uses the weaver repository, group and name`() {
        module(
            extra = """
                weaver { publishing { repository = 'private-releases'; group = 'dev.slne.other'; name = 'custom' } }
            """
        )
        stub.versions("dev.slne.other", "custom", "1.1.0" to false, repository = "private-releases")
        stub.archive(
            "dev.slne.other", "custom", "1.1.0", TarGz.create(baselineContract("1.1.0")), repository = "private-releases",
        )

        build.run("fetchSurfNatsBaseline")

        assertEquals(
            listOf(
                "GET /api/v1/repos/private-releases/artifacts/dev.slne.other/custom",
                "GET /api/v1/repos/private-releases/artifacts/dev.slne.other/custom/1.1.0/archive",
            ),
            paths(),
        )
    }

    @Test
    fun `a server error on the version list fails with the status`() {
        module()
        stub.route("GET", versionsPath) { StubResponse(500, "boom") }

        val result = build.runAndFail("fetchSurfNatsBaseline")

        assertContains(result.output, "failed with HTTP 500: boom")
    }

    @Test
    fun `an unreadable version list fails with a clear message`() {
        module()
        stub.route("GET", versionsPath) { StubResponse(200, "{\"unexpected\":true}") }

        val result = build.runAndFail("fetchSurfNatsBaseline")

        assertContains(result.output, "Unexpected response from")
    }

    @Test
    fun `a listed version without an archive fails instead of skipping the check`() {
        module()
        stub.versions(group, name, "1.1.0" to false)

        val result = build.runAndFail("fetchSurfNatsBaseline")

        assertContains(result.output, "failed with HTTP 404")
    }

    @Test
    fun `an unreachable registry fails with the url`() {
        module(extra = "weaver { url = 'http://127.0.0.1:1' }")

        val result = build.runAndFail("fetchSurfNatsBaseline")

        assertContains(result.output, "surf-nats could not reach http://127.0.0.1:1/")
    }

    @Test
    fun `the task is skipped when the module is neither published nor has an explicit baseline`() {
        module(published = false)

        val result = build.run("fetchSurfNatsBaseline")

        assertEquals(TaskOutcome.SKIPPED, result.outcome(task))
        assertEquals(emptyList(), stub.requests.toList())
        assertNull(baselineDir.resolve("version.txt").takeIf { it.exists() })
    }

    @Test
    fun `the task is skipped when the compatibility check is disabled`() {
        module(extra = "surfNats { compatibilityCheck = false }")

        val result = build.run("fetchSurfNatsBaseline")

        assertEquals(TaskOutcome.SKIPPED, result.outcome(task))
        assertEquals(emptyList(), stub.requests.toList())
    }

    @Test
    fun `the task always asks the registry again`() {
        module()
        stub.versions(group, name, "1.1.0" to false)
        serveArchive("1.1.0", baselineContract("1.1.0"))

        build.run("fetchSurfNatsBaseline")
        val second = build.run("fetchSurfNatsBaseline")

        assertEquals(TaskOutcome.SUCCESS, second.outcome(task))
        assertEquals(4, stub.requests.size)
    }

    private companion object {
        /**
         * Incompressible text, so that an archive containing it is larger than one without it after gzip.
         */
        val NOISE: String = java.util.Base64.getEncoder().encodeToString(java.util.Random(42).let { random ->
            ByteArray(3000).also(random::nextBytes)
        })
    }
}

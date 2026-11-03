package dev.slne.surf.nats.gradle.weaver

import dev.slne.surf.nats.gradle.testkit.TestBuild
import dev.slne.surf.nats.gradle.testkit.TestBuild.Companion.CONTRACT_PATH
import dev.slne.surf.nats.gradle.testkit.TestKitSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

internal class CoordinateValidationTest : TestKitSupport() {

    private fun publishedModule(coordinates: String, withProtos: Boolean = true, publishing: String = TestBuild.PUBLISHING) {
        build.settings()
        build.script(
            """
            ${TestBuild.PLUGIN}
            $publishing
            $coordinates
            """
        )
        if (withProtos) build.proto(CONTRACT_PATH, TestBuild.contract("message Player { optional int64 id = 1; }"))
    }

    private fun failure(): String = build.runAndFail("help").output

    @Test
    fun `a bad group fails configuration naming the property and the project`() {
        publishedModule("group = 'Dev.Slne'; version = '1.0.0'")

        val output = failure()

        assertContains(output, "surf-nats: invalid weaver configuration in :")
        assertContains(output, "weaver.publishing.group 'Dev.Slne' must be a lowercase reverse-DNS name")
    }

    @Test
    fun `a bad version fails configuration`() {
        publishedModule("group = 'dev.slne.demo'; version = '1.2.3.4.5'")

        assertContains(failure(), "weaver.publishing.version '1.2.3.4.5' must have 1 to 4 numeric parts")
    }

    @Test
    fun `a bad name or repository fails configuration`() {
        publishedModule(
            """
            group = 'dev.slne.demo'; version = '1.0.0'
            weaver { publishing { name = 'My_Project'; repository = 'Bad Repo' }; repositories('also bad') }
            """
        )

        val output = failure()

        assertContains(output, "weaver.publishing.name 'My_Project'")
        assertContains(output, "weaver.publishing.repository 'Bad Repo'")
        assertContains(output, "weaver.repositories entry 'also bad'")
    }

    @Test
    fun `a missing group and version are reported together`() {
        publishedModule("")

        val output = failure()

        assertContains(output, "weaver.publishing.group is not set")
        assertContains(output, "weaver.publishing.version is not set")
    }

    @Test
    fun `a publishing repository added in a later afterEvaluate is still validated`() {
        publishedModule(
            "group = 'Dev.Slne'; version = '1.0.0'",
            publishing = """
                apply plugin: 'maven-publish'
                afterEvaluate { publishing { repositories { maven { name = 'late'; url = uri('build/late') } } } }
            """,
        )

        assertContains(failure(), "weaver.publishing.group 'Dev.Slne' must be a lowercase reverse-DNS name")
    }

    @Test
    fun `an ungrouped published subproject reports the group as not set`() {
        build.settings("sub", rootName = "demo")
        build.script("${TestBuild.PLUGIN}\n${TestBuild.PUBLISHING}\nversion = '1.0.0'", project = "sub")
        build.proto(CONTRACT_PATH, TestBuild.contract("message Player { optional int64 id = 1; }"), project = "sub")

        assertContains(failure(), "weaver.publishing.group is not set")
    }

    @Test
    fun `valid coordinates configure without errors`() {
        publishedModule("group = 'dev.slne.demo'; version = '1.2.3.4-rc.1'")

        build.run("help")
    }

    @Test
    fun `modules without a publishing repository are not validated`() {
        publishedModule(
            "group = 'Dev.Slne'; version = 'v1'",
            publishing = "apply plugin: 'maven-publish'",
        )

        build.run("help")
    }

    @Test
    fun `modules without maven-publish are not validated`() {
        publishedModule("group = 'Dev.Slne'; version = 'v1'", publishing = "")

        build.run("help")
    }

    @Test
    fun `published modules without proto sources are not validated`() {
        publishedModule("group = 'Dev.Slne'; version = 'v1'", withProtos = false)

        build.run("help")
    }

    private fun twoPublishedModules(secondName: String? = null, secondPublished: Boolean = true, secondProtos: Boolean = true) {
        build.settings("a", "b", rootName = "demo")
        build.script("allprojects { group = 'dev.slne.demo'; version = '1.0.0' }")
        build.script("${TestBuild.PLUGIN}\n${TestBuild.PUBLISHING}", project = "a")
        build.proto(CONTRACT_PATH, TestBuild.contract("message A { optional int64 id = 1; }"), project = "a")
        build.script(
            """
            ${TestBuild.PLUGIN}
            ${if (secondPublished) TestBuild.PUBLISHING else "apply plugin: 'maven-publish'"}
            ${secondName?.let { "weaver { publishing { name = '$it' } }" }.orEmpty()}
            """,
            project = "b",
        )
        if (secondProtos) {
            build.proto("surf/b/v1/b.proto", TestBuild.contract("message B { optional int64 id = 1; }"), project = "b")
        }
    }

    @Test
    fun `two published projects with the same coordinates fail naming both projects and the property to change`() {
        twoPublishedModules()

        val output = failure()

        assertContains(output, "projects :a and :b both publish the weaver artifact dev.slne.demo:demo")
        assertContains(output, "weaver { publishing { name = \"...\" } }")
    }

    @Test
    fun `projects whose repositories are added in afterEvaluate still clash`() {
        build.settings("a", "b", rootName = "demo")
        build.script("allprojects { group = 'dev.slne.demo'; version = '1.0.0' }")
        for (project in listOf("a", "b")) {
            build.script(
                """
                ${TestBuild.PLUGIN}
                apply plugin: 'maven-publish'
                afterEvaluate { publishing { repositories { maven { name = 'late'; url = uri('build/late') } } } }
                """,
                project = project,
            )
            build.proto(
                "surf/$project/v1/$project.proto",
                TestBuild.contract("message M { optional int64 id = 1; }"),
                project = project,
            )
        }

        assertContains(failure(), "projects :a and :b both publish the weaver artifact dev.slne.demo:demo")
    }

    @Test
    fun `distinct weaver names resolve the clash`() {
        twoPublishedModules(secondName = "demo-b")

        build.run("help")
    }

    @Test
    fun `an unpublished project with the same coordinates is not a clash`() {
        twoPublishedModules(secondPublished = false)

        build.run("help")
    }

    @Test
    fun `a project without protos is not a clash`() {
        twoPublishedModules(secondProtos = false)

        val result = build.run("help")

        assertFalse("both publish" in result.output)
    }
}

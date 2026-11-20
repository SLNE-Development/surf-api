package dev.slne.surf.api.gradle.pluginclass

import dev.slne.surf.api.gradle.generators.pluginfiles.VelocityPluginFile
import dev.slne.surf.api.gradle.platform.paper.plugin.PaperPluginSurfExtension
import dev.slne.surf.api.gradle.platform.paper.plugin.PaperPluginSurfPlugin
import dev.slne.surf.api.gradle.platform.velocity.VelocitySurfExtension
import dev.slne.surf.api.gradle.platform.velocity.VelocitySurfPlugin
import net.minecrell.pluginyml.GeneratePluginDescription
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Wiring of the plugin class tasks and the Paper dependency DSL, on projects that are evaluated but never built.
 */
class PluginClassWiringTest {

    private inline fun <reified E : Any> evaluatedProject(
        plugin: Class<out Plugin<Project>>,
        configure: E.() -> Unit = {},
    ): Project {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(plugin)
        project.extensions.getByType<E>().configure()
        (project as ProjectInternal).evaluate()
        return project
    }

    private fun Project.paperDescription() = extensions.getByType<PaperPluginDescription>()

    private fun Task.runs() = (this as TaskInternal).onlyIf.isSatisfiedBy(this)

    private fun Project.dependsOnNames(taskName: String): Set<String> =
        tasks.getByName(taskName).taskDependencies.getDependencies(null).map { it.name }.toSet()

    @Test
    fun `paper plugin evaluates without a main class`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java)

        assertNotNull(project.tasks.findByName("generateSurfPluginClasses"))
        assertNotNull(project.tasks.findByName("computeSurfBasePackage"))
    }

    @Test
    fun `surfPluginDependency registers a required server and bootstrap dependency`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            surfPluginDependency("surf-core-paper")
        }

        for (dependencies in listOf(project.paperDescription().serverDependencies, project.paperDescription().bootstrapDependencies)) {
            val dependency = assertNotNull(dependencies.findByName("surf-core-paper"))
            assertTrue(dependency.required)
            assertTrue(dependency.joinClasspath)
            assertEquals(RelativeLoadOrder.BEFORE, dependency.load)
        }
    }

    @Test
    fun `softSurfPluginDependency registers an optional server and bootstrap dependency`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            softSurfPluginDependency("surf-lobby-paper")
        }

        for (dependencies in listOf(project.paperDescription().serverDependencies, project.paperDescription().bootstrapDependencies)) {
            val dependency = assertNotNull(dependencies.findByName("surf-lobby-paper"))
            assertFalse(dependency.required)
            assertTrue(dependency.joinClasspath)
            assertEquals(RelativeLoadOrder.BEFORE, dependency.load)
        }
    }

    @Test
    fun `dependencies of other plugins are never mirrored into the bootstrap dependencies`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            serverDependencies { register("LuckPerms") }
        }

        assertNotNull(project.paperDescription().serverDependencies.findByName("LuckPerms"))
        assertEquals(null, project.paperDescription().bootstrapDependencies.findByName("LuckPerms"))
    }

    @Test
    fun `CommandAPI is always a required server dependency with join-classpath`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java)

        val commandApi = assertNotNull(project.paperDescription().serverDependencies.findByName("CommandAPI"))
        assertTrue(commandApi.required)
        assertTrue(commandApi.joinClasspath)
        assertEquals(RelativeLoadOrder.BEFORE, commandApi.load)
        assertEquals(null, project.paperDescription().bootstrapDependencies.findByName("CommandAPI"))
    }

    @Test
    fun `a CommandAPI dependency of the build script is kept`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            serverDependencies {
                register("CommandAPI") {
                    required = false
                    load = RelativeLoadOrder.AFTER
                }
            }
        }

        val commandApi = assertNotNull(project.paperDescription().serverDependencies.findByName("CommandAPI"))
        assertFalse(commandApi.required)
        assertEquals(RelativeLoadOrder.AFTER, commandApi.load)
    }

    @Test
    fun `a surf plugin dependency yields to a build script dependency of the same name`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            surfPluginDependency("surf-core-paper")
            serverDependencies { register("surf-core-paper") { required = false } }
            bootstrapDependencies { register("surf-core-paper") { load = RelativeLoadOrder.AFTER } }
        }

        val server = assertNotNull(project.paperDescription().serverDependencies.findByName("surf-core-paper"))
        assertFalse(server.required)
        val bootstrap = assertNotNull(project.paperDescription().bootstrapDependencies.findByName("surf-core-paper"))
        assertEquals(RelativeLoadOrder.AFTER, bootstrap.load)
    }

    @Test
    fun `the Paper description provider is installed on the Paper description task only`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(PaperPluginSurfPlugin::class.java)
        project.plugins.apply("de.eldoria.plugin-yml.bukkit")
        (project as ProjectInternal).evaluate()

        val bukkit = project.tasks.getByName("generateBukkitPluginDescription") as GeneratePluginDescription

        assertTrue(bukkit.pluginDescription.get() is BukkitPluginDescription)
        assertFalse("computeSurfBasePackage" in project.dependsOnNames("generateBukkitPluginDescription"))
        assertTrue("computeSurfBasePackage" in project.dependsOnNames("generatePaperPluginDescription"))
    }

    @Test
    fun `the base package is only needed when something generated uses it`() {
        fun needsBasePackage(project: Project) =
            (project.tasks.getByName("computeSurfBasePackage") as ComputeBasePackageTask).generatesCode.get()

        val generated = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java)
        val bootstrapperOnly = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            bootstrapper("com.example.Boot")
        }
        val named = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            mainClass("com.example.Main")
            bootstrapper("com.example.Boot")
        }
        val namedWithLoader = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            mainClass("com.example.Main")
            bootstrapper("com.example.Boot")
            generateLibraryLoader(true)
        }
        val velocityNamed = evaluatedProject<VelocitySurfExtension>(VelocitySurfPlugin::class.java)
        velocityNamed.extensions.getByType<VelocityPluginFile>().main.set("com.example.Main")

        assertTrue(needsBasePackage(generated))
        assertTrue(needsBasePackage(bootstrapperOnly))
        assertFalse(needsBasePackage(named))
        assertTrue(needsBasePackage(namedWithLoader))
        assertFalse(needsBasePackage(velocityNamed))
    }

    @Test
    fun `the runtime scans from the named bootstrapper on Paper and the named main class elsewhere`() {
        val paper = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            mainClass("com.example.Main")
            bootstrapper("com.example.boot.Boot")
        }
        val velocity = evaluatedProject<VelocitySurfExtension>(VelocitySurfPlugin::class.java)
        velocity.extensions.getByType<VelocityPluginFile>().main.set("com.example.Main")

        fun scanned(project: Project) =
            (project.tasks.getByName("computeSurfBasePackage") as ComputeBasePackageTask).scannedClass.orNull

        assertEquals("com.example.boot.Boot", scanned(paper))
        assertEquals("com.example.Main", scanned(velocity))
    }

    @Test
    fun `generatePluginClass off generates no class on Paper and Velocity`() {
        val paper = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            generatePluginClass.set(false)
        }
        val velocity = evaluatedProject<VelocitySurfExtension>(VelocitySurfPlugin::class.java) {
            generatePluginClass.set(false)
        }

        for (project in listOf(paper, velocity)) {
            val generate = project.tasks.getByName("generateSurfPluginClasses") as GeneratePluginClassesTask
            assertEquals(emptyList(), generate.classes.get())
        }
    }

    @Test
    fun `without a generated class a Velocity plugin has a main class only when the build script names one`() {
        val unnamed = evaluatedProject<VelocitySurfExtension>(VelocitySurfPlugin::class.java) {
            generatePluginClass.set(false)
        }
        val named = evaluatedProject<VelocitySurfExtension>(VelocitySurfPlugin::class.java) {
            generatePluginClass.set(false)
        }
        named.extensions.getByType<VelocityPluginFile>().main.set("com.example.Main")

        assertFalse(unnamed.extensions.getByType<VelocityPluginFile>().effectiveMain().isPresent)
        assertEquals("com.example.Main", named.extensions.getByType<VelocityPluginFile>().effectiveMain().get())
    }

    @Test
    fun `the class check runs before every jar and the Spring check only before the shadow jar`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java)

        val check = project.dependsOnNames("check")
        assertTrue("checkSurfPluginClasses" in check, check.toString())
        assertTrue("checkNoBundledSpring" in check, check.toString())

        val jar = project.dependsOnNames("jar")
        assertTrue("checkSurfPluginClasses" in jar, jar.toString())
        assertFalse("checkNoBundledSpring" in jar, jar.toString())

        val shadowJar = project.dependsOnNames("shadowJar")
        assertTrue("checkSurfPluginClasses" in shadowJar, shadowJar.toString())
        assertTrue("checkNoBundledSpring" in shadowJar, shadowJar.toString())

        assertFalse("checkSurfPluginClasses" in project.dependsOnNames("sourcesJar"))
        assertTrue(project.tasks.getByName("sourcesJar") is Jar)
    }

    @Test
    fun `the Spring check is skipped when the build script allows Spring`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            allowBundledSpring.set(true)
        }

        assertFalse(project.tasks.getByName("checkNoBundledSpring").runs())
    }

    @Test
    fun `the Spring check is skipped when surf-api is not compileOnly`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            surfApiScope("implementation")
        }

        val task = project.tasks.getByName("checkNoBundledSpring")
        assertFalse(task.runs())
    }

    @Test
    fun `the Spring check runs by default`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java)

        val task = project.tasks.getByName("checkNoBundledSpring")
        assertTrue(task.runs())
    }

    @Test
    fun `the class check has nothing to do without a named class`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java)

        val task = project.tasks.getByName("checkSurfPluginClasses")
        assertFalse(task.runs())
    }

    @Test
    fun `the class check runs for a named main class`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            mainClass("com.example.Main")
        }

        val task = project.tasks.getByName("checkSurfPluginClasses")
        assertTrue(task.runs())
    }

    @Test
    fun `the class check is off when verifyPluginClasses is false`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            mainClass("com.example.Main")
            verifyPluginClasses.set(false)
        }

        val task = project.tasks.getByName("checkSurfPluginClasses")
        assertFalse(task.runs())
    }

    @Test
    fun `the generated main class is not generated when the build script names one`() {
        val project = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java) {
            mainClass("com.example.Main")
            bootstrapper("com.example.Boot")
        }

        val generate = project.tasks.getByName("generateSurfPluginClasses") as GeneratePluginClassesTask
        assertEquals(emptyList(), generate.classes.get())
    }

    @Test
    fun `Paper generates both classes by default and Velocity only a main class`() {
        val paper = evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java)
        val velocity = evaluatedProject<VelocitySurfExtension>(VelocitySurfPlugin::class.java)

        assertEquals(
            listOf(GeneratedPluginClass.PAPER_BOOTSTRAPPER, GeneratedPluginClass.PAPER_PLUGIN),
            (paper.tasks.getByName("generateSurfPluginClasses") as GeneratePluginClassesTask).classes.get(),
        )
        assertEquals(
            listOf(GeneratedPluginClass.VELOCITY_PLUGIN),
            (velocity.tasks.getByName("generateSurfPluginClasses") as GeneratePluginClassesTask).classes.get(),
        )
    }

    @Test
    fun `every platform plugin applies the Spring compiler plugin and enables javaParameters`() {
        val projects = listOf(
            evaluatedProject<PaperPluginSurfExtension>(PaperPluginSurfPlugin::class.java),
            evaluatedProject<VelocitySurfExtension>(VelocitySurfPlugin::class.java),
        )

        for (project in projects) {
            assertTrue(project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.spring"))
            assertTrue(
                project.extensions.getByType<KotlinJvmProjectExtension>().compilerOptions.javaParameters.get()
            )
        }
    }
}

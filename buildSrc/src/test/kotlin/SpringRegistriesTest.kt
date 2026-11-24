import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class SpringRegistriesTest {

    private val directory = createTempDirectory("spring-registries")
        .toFile()
        .apply { deleteOnExit() }

    @Test
    fun `spring factories append the values of a key`() {
        val merged = shadow(
            "META-INF/spring.factories" to "demo.Initializer=first.Impl\n",
            "META-INF/spring.factories" to "demo.Initializer=second.Impl\n",
        ).properties("META-INF/spring.factories")

        assertEquals("first.Impl,second.Impl", merged.getProperty("demo.Initializer"))
    }

    @Test
    fun `spring handlers and schemas keep one value of a repeated key`() {
        val jar = shadow(
            "META-INF/spring.handlers" to "http\\://demo/schema=first.Handler\n",
            "META-INF/spring.handlers" to "http\\://demo/schema=second.Handler\nhttp\\://demo/other=other.Handler\n",
            "META-INF/spring.schemas" to "http\\://demo/schema.xsd=first.xsd\n",
            "META-INF/spring.schemas" to "http\\://demo/schema.xsd=second.xsd\n",
        )

        val handlers = jar.properties("META-INF/spring.handlers")
        assertEquals("first.Handler", handlers.getProperty("http://demo/schema"))
        assertEquals("other.Handler", handlers.getProperty("http://demo/other"))
        assertEquals(
            "first.xsd",
            jar.properties("META-INF/spring.schemas").getProperty("http://demo/schema.xsd"),
        )
    }

    private fun shadow(vararg entries: Pair<String, String>): File {
        val project = ProjectBuilder.builder().withProjectDir(directory).build()
        project.pluginManager.apply("java")
        project.pluginManager.apply("com.gradleup.shadow")
        val task = project.tasks.named("shadowJar", ShadowJar::class.java).get()
        entries.forEachIndexed { index, (path, content) ->
            task.from(project.zipTree(jarWith(index, path, content)))
        }
        task.mergeSpringRegistries()
        task.archiveFileName.set("merged.jar")
        task.destinationDirectory.set(File(directory, "out").apply { mkdirs() })

        for (action in task.actions) {
            action.execute(task)
        }

        return task.archiveFile.get().asFile
    }

    private fun jarWith(index: Int, path: String, content: String): File {
        val file = File(directory, "input-$index.jar")
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(path))
            zip.write(content.toByteArray())
            zip.closeEntry()
        }

        return file
    }

    private fun File.properties(path: String): Properties = ZipFile(this).use { zip ->
        Properties().apply { zip.getInputStream(zip.getEntry(path)).use(::load) }
    }
}

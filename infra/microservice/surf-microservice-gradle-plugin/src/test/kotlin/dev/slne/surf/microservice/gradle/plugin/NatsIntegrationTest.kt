package dev.slne.surf.microservice.gradle.plugin

import dev.slne.surf.api.gradle.platform.common.CommonSurfExtension
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NatsIntegrationTest {

    private val natsIntegration = "dev.slne.surf.microservice:surf-microservice-nats:+"

    private fun evaluatedProject(
        surfNats: Boolean,
        configure: MicroserviceExtension.() -> Unit,
    ): Project {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("dev.slne.surf.api.gradle.standalone")
        project.pluginManager.apply(MicroservicePlugin::class.java)

        if (surfNats) {
            project.extensions.getByType<CommonSurfExtension>().withSurfNats()
        }
        project.extensions.getByType<MicroserviceExtension>().configure()

        (project as ProjectInternal).evaluate()
        return project
    }

    private fun Project.runtimeOnlyNotations() =
        configurations.getByName("runtimeOnly").dependencies
            .map { "${it.group}:${it.name}:${it.version}" }

    @Test
    fun `microservice with withSurfNats gets the nats integration at runtime`() {
        val project = evaluatedProject(surfNats = true) { withMicroserviceApi() }

        assertTrue(natsIntegration in project.runtimeOnlyNotations())
    }

    @Test
    fun `microservice without withSurfNats gets no nats integration`() {
        val project = evaluatedProject(surfNats = false) { withMicroserviceApi() }

        assertFalse(natsIntegration in project.runtimeOnlyNotations())
    }

    @Test
    fun `common module with withSurfNats gets no nats integration`() {
        val project = evaluatedProject(surfNats = true) { withCommonApi() }

        assertFalse(natsIntegration in project.runtimeOnlyNotations())
    }
}

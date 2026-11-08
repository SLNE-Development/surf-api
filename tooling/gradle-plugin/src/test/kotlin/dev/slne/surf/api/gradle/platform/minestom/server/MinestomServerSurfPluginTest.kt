package dev.slne.surf.api.gradle.platform.minestom.server

import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MinestomServerSurfPluginTest {

    private val project = ProjectBuilder.builder().build().also {
        it.plugins.apply(MinestomServerSurfPlugin::class.java)
    }

    @Test
    fun `shaded configuration is resolvable but not consumable`() {
        val shaded = project.configurations.getByName("shaded")

        assertTrue(shaded.isCanBeResolved)
        assertFalse(shaded.isCanBeConsumed)
        assertNotNull(shaded.description)
    }

    @Test
    fun `runtimeOnly extends shaded`() {
        val shaded = project.configurations.getByName("shaded")

        assertTrue(shaded in project.configurations.getByName("runtimeOnly").extendsFrom)
    }

    @Test
    fun `downloadSurfModules is disabled by default and downloadLibraries enabled`() {
        val extension = project.extensions.getByType<MinestomServerSurfExtension>()

        assertFalse(extension.downloadSurfModules.get())
        assertTrue(extension.downloadLibraries.get())
    }
}

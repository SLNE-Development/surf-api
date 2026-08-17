package dev.slne.surf.api.gradle.platform.minestom

import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.common.testing.SurfTestingConfigurer
import dev.slne.surf.api.gradle.platform.core.AbstractCoreSurfPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal class MinestomSurfPlugin : AbstractCoreSurfPlugin<MinestomSurfExtension>(
    platformName = "minestom",
    platform = SurfApiPlatform.MINESTOM,
) {
    init {
        "it.unimi.dsi.fastutil" relocatesTo "fastutil"
        "me.devnatan.inventoryframework" relocatesTo "devnatan.inventoryframework"
    }

    override val extensionClass = MinestomSurfExtension::class.java

    override fun Project.platformTestDependencies(extension: MinestomSurfExtension) {
        val testing = extension.testing
        if (!testing.minestomTesting.get()) return
        dependencies {
            add(
                SurfTestingConfigurer.TEST_IMPLEMENTATION,
                "net.minestom:testing:${testing.minestomTestingVersion.get()}"
            )
        }
    }
}

package dev.slne.surf.surfapi.gradle.platform.canvas

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.platform.paper.plugin.AbstractPaperPluginSurfPlugin
import dev.slne.surf.surfapi.gradle.util.canvasMaven
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal class CanvasPluginSurfPlugin :
    AbstractPaperPluginSurfPlugin<CanvasSurfExtension>("canvasPlugin") {

    override val extensionClass = CanvasSurfExtension::class.java

    override fun Project.applyRepositories0() {
        repositories {
            canvasMaven()
        }
    }

    override fun Project.configure0() {
        dependencies {
            add(COMPILE_ONLY, Constants.CANVAS_API)
        }

        // Canvas provides the org.bukkit:bukkit capability (as a Paper fork), so we resolve
        // any capability conflict in favour of canvas-api over paper-api or plain bukkit.
        configurations.all {
            resolutionStrategy.capabilitiesResolution.withCapability("org.bukkit:bukkit") {
                select(Constants.CANVAS_API)
            }
        }
    }
}

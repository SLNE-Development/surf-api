package dev.slne.surf.api.gradle.platform.paper

import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.core.AbstractCoreSurfPlugin
import dev.slne.surf.api.gradle.util.canvasMaven
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal abstract class AbstractPaperSurfPlugin<E : AbstractPaperSurfExtension>(platformName: String) :
    AbstractCoreSurfPlugin<E>(platformName, SurfApiPlatform.PAPER) {

    init {
        "me.devnatan.inventoryframework" relocatesTo "devnatan.inventoryframework"
    }

    override fun Project.configure0() {
    }

    final override fun Project.afterEvaluated1(extension: E) {
        if (extension.useCanvasMc.get()) {
            repositories {
                canvasMaven()
            }
        }

        dependencies {
            if (extension.useCanvasMc.get()) {
                add(COMPILE_ONLY, Constants.CANVAS_API)
            } else {
                add(COMPILE_ONLY, Constants.PAPER_API)
            }
        }

        if (extension.useCanvasMc.get()) {
            configurations.all {
                resolutionStrategy.capabilitiesResolution.withCapability("org.bukkit:bukkit") {
                    select(Constants.CANVAS_API)
                }
            }
        }

        afterEvaluated2(extension)
    }

    open fun Project.afterEvaluated2(extension: E) {

    }
}
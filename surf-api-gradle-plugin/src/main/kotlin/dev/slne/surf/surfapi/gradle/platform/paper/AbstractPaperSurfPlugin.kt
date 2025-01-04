package dev.slne.surf.surfapi.gradle.platform.paper

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.core.AbstractCoreSurfPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal abstract class AbstractPaperSurfPlugin<E : AbstractPaperSurfExtension>(platformName: String) :
    AbstractCoreSurfPlugin<E>(platformName, SurfApiPlatform.PAPER) {

    override fun Project.configure0() {
        dependencies {
            add(COMPILE_ONLY, Constants.PAPER_API)
        }
    }
}
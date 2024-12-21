package dev.slne.surf.surfapi.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

const val VERSION = "1.21.4+-SNAPSHOT"

class SurfApiPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("surfApi", SurfApiExtension::class.java)

        target.afterEvaluate {
            configureDependencies(target, extension.mode)
        }
    }

    private fun configureDependencies(project: Project, mode: SurfApiExtension.SurfApiMode?) {
        if (mode == null) {
            project.logger.error("No mode specified for Surf API")
            return
        }

        project.dependencies.add(mode.scope, mode.dependency)
    }
}

open class SurfApiExtension {
    var mode: SurfApiMode? = null

    enum class SurfApiMode(val dependency: String, val scope: String = "compileOnly") {
        CORE("dev.slne.surf:surf-api-core-api:$VERSION"),
        BUKKIT("dev.slne.surf:surf-api-bukkit-api:$VERSION"),
        VELOCITY("dev.slne.surf:surf-api-velocity-api:$VERSION"),
        STANDALONE("dev.slne.surf:surf-api-standalone:$VERSION", "api"),
    }
}
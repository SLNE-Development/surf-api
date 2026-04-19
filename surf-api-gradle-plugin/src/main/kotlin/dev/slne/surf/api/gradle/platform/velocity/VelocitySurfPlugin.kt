package dev.slne.surf.api.gradle.platform.velocity

import dev.slne.surf.api.gradle.generated.Constants
import dev.slne.surf.api.gradle.generators.pluginfiles.VelocityPluginFile
import dev.slne.surf.api.gradle.generators.pluginfiles.dto.VelocityPluginFileDto
import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.common.CommonSurfPluginWithPluginFile
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal class VelocitySurfPlugin :
    CommonSurfPluginWithPluginFile<VelocitySurfExtension, VelocityPluginFile, VelocityPluginFileDto>(
        "velocity",
        SurfApiPlatform.VELOCITY,
        "velocity-plugin.json"
    ) {

    init {
        "it.unimi.dsi.fastutil" relocatesTo "fastutil"
    }

    override val extensionClass = VelocitySurfExtension::class.java
    override val dtoSerializer = VelocityPluginFileDto.serializer()

    override fun Project.configure0() {
        dependencies {
            add(COMPILE_ONLY, Constants.VELOCITY_API)
//            add("annotationProcessor", Constants.VELOCITY_API)
        }
    }

    override fun createPluginFile(
        project: Project
    ) = project.extensions.create<VelocityPluginFile>("velocityPluginFile").apply {
        id.convention(project.name.lowercase())
        name.convention(project.provider { project.name })
        version.convention(project.provider { project.version.toString() })
        description.convention(project.provider { project.description ?: "" })
        url.convention(project.providers.gradleProperty("url"))

        val ext = project.extensions.getByType(VelocitySurfExtension::class.java)
        val coreEnabled = ext.coreModule.map { true }.orElse(false)
        val surfRedisRelocationPresent = ext.surfRedisRelocation.map { true }.orElse(false)
        val redisEnabled =
            ext.withSurfRedis.zip(surfRedisRelocationPresent) { withRedis, relocPresent ->
                withRedis && !relocPresent
            }

        pluginDependencies {
            register("surf-api-velocity") {
                optional.convention(false)
                enabled.convention(true)
            }

            register("luckperms") {
                optional.convention(false)
                enabled.convention(true)
            }

            register("surf-core-velocity") {
                optional.convention(false)
                enabled.convention(coreEnabled)
            }

            register("surf-redis-velocity") {
                optional.convention(false)
                enabled.convention(redisEnabled)
            }
        }
    }

    override fun createPluginFileDto(pluginFile: VelocityPluginFile): VelocityPluginFileDto {
        return VelocityPluginFileDto.fromFile(pluginFile)
    }
}
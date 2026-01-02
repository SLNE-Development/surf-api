package dev.slne.surf.surfapi.gradle.platform.core

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.common.CommonSurfPlugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.utils.API
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal abstract class AbstractCoreSurfPlugin<E : CoreSurfExtension>(
    platformName: String, platform: SurfApiPlatform,
) : CommonSurfPlugin<E>(platformName, platform) {
    init {
        "com.mojang.serialization" relocatesTo "mojang.serialization"
        "com.mojang.datafixers" relocatesTo "mojang.datafixers"
        "net.kyori.adventure.nbt" relocatesTo "kyori.nbt"
    }

    final override fun Project.afterEvaluated0(extension: E) {
        if (extension.withSurfRedis.get()) {
            if (extension.surfRedisRelocation.isPresent) {
                dependencies {
                    add(API, "dev.slne.surf:surf-redis:${extension.surfRedisVersion.get()}")
                }
                tasks.withType<ShadowJar>().configureEach {
                    doFirst {
                        relocate("dev.slne.surf.redis", extension.surfRedisRelocation.get())
                    }
                }
            } else {
                dependencies {
                    add(COMPILE_ONLY, "dev.slne.surf:surf-redis-api:${Constants.SURF_API_VERSION}")
                }
            }
        }

        if (extension.withSurfDatabaseR2dbc.get()) {
            dependencies {
                add(API, "dev.slne.surf:surf-database-r2dbc:${extension.surfDatabaseR2dbcVersion.get()}")
            }

            tasks.withType<ShadowJar>().configureEach {
                doFirst {
                    relocate("dev.slne.surf.database", extension.surfDatabaseR2dbcRelocation.get())
                }
            }
        }

        afterEvaluated1(extension)
    }

    protected open fun Project.afterEvaluated1(extension: E) {
    }
}

internal class CoreSurfPlugin :
    AbstractCoreSurfPlugin<CoreSurfExtension>("core", SurfApiPlatform.CORE) {
    override fun createExtension(objects: ObjectFactory, project: Project) =
        CoreSurfExtension(objects)
}
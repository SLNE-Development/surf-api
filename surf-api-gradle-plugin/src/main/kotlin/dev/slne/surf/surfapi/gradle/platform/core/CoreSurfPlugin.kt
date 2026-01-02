package dev.slne.surf.surfapi.gradle.platform.core

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.common.CommonSurfPlugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

internal abstract class AbstractCoreSurfPlugin<E : CoreSurfExtension>(
    platformName: String, platform: SurfApiPlatform,
) : CommonSurfPlugin<E>(platformName, platform) {
    init {
        "com.mojang.serialization" relocatesTo "mojang.serialization"
        "com.mojang.datafixers" relocatesTo "mojang.datafixers"
        "net.kyori.adventure.nbt" relocatesTo "kyori.nbt"
    }

    final override fun Project.afterEvaluated0(extension: E) {
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
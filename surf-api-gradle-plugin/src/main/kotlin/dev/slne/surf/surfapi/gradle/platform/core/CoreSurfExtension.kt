package dev.slne.surf.surfapi.gradle.platform.core

import dev.slne.surf.surfapi.gradle.SurfCloudModules
import dev.slne.surf.surfapi.gradle.platform.common.CommonSurfExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.listProperty
import javax.inject.Inject

open class CoreSurfExtension @Inject constructor(objects: ObjectFactory) :
    CommonSurfExtension(objects) {
    val authors = objects.listProperty<String>().convention(mutableListOf("SLNE Development"))

    fun withCloudCommon() {
        cloudModule.set(SurfCloudModules.COMMON)
    }

    fun withCloudClientCommon() {
        cloudModule.set(SurfCloudModules.CLIENT_COMMON)
    }

    fun withCloudServer() {
        cloudModule.set(SurfCloudModules.SERVER)
    }

    fun migrationMainClass(value: String) {
        migrationMainClass.set(value)
        migrationMainClass.finalizeValue()
    }
}
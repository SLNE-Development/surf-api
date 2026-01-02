package dev.slne.surf.surfapi.gradle.platform.core

import dev.slne.surf.surfapi.gradle.SurfCloudModules
import dev.slne.surf.surfapi.gradle.SurfCoreModules
import dev.slne.surf.surfapi.gradle.platform.common.CommonSurfExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class CoreSurfExtension @Inject constructor(objects: ObjectFactory) :
    CommonSurfExtension(objects) {
    val authors = objects.listProperty<String>().convention(mutableListOf("SLNE Development"))

    internal val withSurfRedis = objects.property<Boolean>().convention(false)
    internal val surfRedisVersion = objects.property<String>()
    internal val surfRedisRelocation = objects.property<String>()

    internal val withSurfDatabaseR2dbc = objects.property<Boolean>().convention(false)
    internal val surfDatabaseR2dbcVersion = objects.property<String>()
    internal val surfDatabaseR2dbcRelocation = objects.property<String>()

    fun withSurfRedis() {
        withSurfRedis.set(true)
        withSurfRedis.finalizeValue()
    }

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Plugins no longer shade surf-redis. Use the standard withSurfRedis() method instead.",
        replaceWith = ReplaceWith("withSurfRedis()")
    )
    fun withSurfRedis(version: String, relocation: String) {
        withSurfRedis.set(true)
        withSurfRedis.finalizeValue()
        surfRedisVersion.set(version)
        surfRedisVersion.finalizeValue()
        surfRedisRelocation.set(relocation)
        surfRedisRelocation.finalizeValue()
    }

    fun withSurfDatabaseR2dbc(version: String, relocation: String) {
        withSurfDatabaseR2dbc.set(true)
        withSurfDatabaseR2dbc.finalizeValue()
        surfDatabaseR2dbcVersion.set(version)
        surfDatabaseR2dbcVersion.finalizeValue()
        surfDatabaseR2dbcRelocation.set(relocation)
        surfDatabaseR2dbcRelocation.finalizeValue()
    }

    fun withCloudCommon() {
        cloudModule.set(SurfCloudModules.COMMON)
    }

    fun withCloudClientCommon() {
        cloudModule.set(SurfCloudModules.CLIENT_COMMON)
    }

    fun withCloudServer() {
        cloudModule.set(SurfCloudModules.SERVER)
    }

    fun withCoreCommon() {
        coreModule.set(SurfCoreModules.COMMON)
    }

    fun withCorePaper() {
        coreModule.set(SurfCoreModules.PAPER)
    }

    fun withCoreVelocity() {
        coreModule.set(SurfCoreModules.VELOCITY)
    }

    fun migrationMainClass(value: String) {
        migrationMainClass.set(value)
        migrationMainClass.finalizeValue()
    }
}
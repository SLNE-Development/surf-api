package dev.slne.surf.api.gradle.platform.common

import dev.slne.surf.api.gradle.SurfCoreModules
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import org.jetbrains.annotations.MustBeInvokedByOverriders

abstract class CommonSurfExtension(protected val objects: ObjectFactory) {
    internal val addSurfApiToClasspath = objects.property<Boolean>().convention(true)
    internal val surfApiScope = objects.property<String>()
    internal val publishingUrl =
        objects.property<String>().convention("https://reposilite.slne.dev/releases")
    internal val publishingRepoName =
        objects.property<String>().convention("slne-repository-releases")
    internal val coreModule = objects.property<SurfCoreModules>()
    internal val withSurfRedis = objects.property<Boolean>().convention(false)
    internal val surfRedisVersion = objects.property<String>()
    internal val surfRedisRelocation = objects.property<String>()
    internal val withSurfDatabaseR2dbc = objects.property<Boolean>().convention(false)
    internal val surfDatabaseR2dbcVersion = objects.property<String>()
    internal val surfDatabaseR2dbcRelocation = objects.property<String>()
    internal val withApiValidation = objects.property<Boolean>().convention(false)

    fun withApiValidation() {
        withApiValidation.set(true)
        withApiValidation.finalizeValue()
    }

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

    fun addSurfApiToClasspath(value: Boolean) {
        addSurfApiToClasspath.set(value)
        addSurfApiToClasspath.finalizeValue()
    }

    fun surfApiScope(value: String) {
        surfApiScope.set(value)
        surfApiScope.finalizeValue()
    }

    fun publishingUrl(value: String, repoName: String) {
        publishingUrl.set(value)
        publishingUrl.finalizeValue()

        publishingRepoName.set(repoName)
        publishingRepoName.finalizeValue()
    }

    @MustBeInvokedByOverriders
    internal open fun validate() {

    }
}
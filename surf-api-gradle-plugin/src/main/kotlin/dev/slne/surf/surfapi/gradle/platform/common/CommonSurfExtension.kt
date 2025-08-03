package dev.slne.surf.surfapi.gradle.platform.common

import dev.slne.surf.surfapi.gradle.SurfCloudModules
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import org.jetbrains.annotations.MustBeInvokedByOverriders

abstract class CommonSurfExtension(protected val objects: ObjectFactory) {
    internal val addSurfApiToClasspath = objects.property<Boolean>().convention(true)
    internal val surfApiScope = objects.property<String>()
    internal val publishingUrl = objects.property<String>().convention("https://repo.slne.dev/repository/maven-releases")
    internal val publishingRepoName = objects.property<String>().convention("maven-releases")
    internal val cloudModule = objects.property<SurfCloudModules>()
    internal val migrationMainClass = objects.property<String>()

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
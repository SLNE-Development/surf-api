package dev.slne.surf.api.gradle.platform.common

import dev.slne.surf.api.gradle.SurfCoreModules
import dev.slne.surf.api.gradle.platform.common.testing.SurfTestingExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.jetbrains.annotations.MustBeInvokedByOverriders

abstract class CommonSurfExtension(protected val objects: ObjectFactory) {
    internal val addSurfApiToClasspath = objects.property<Boolean>().convention(true)
    internal val surfApiScope = objects.property<String>()
    internal val publishingUrl = objects.property<String>()
        .convention("https://reposilite.slne.dev/releases")
    internal val publishingRepoName = objects.property<String>()
        .convention("slne-repository-releases")
    internal val coreModule = objects.property<SurfCoreModules>()
    internal val withSurfRedis = objects.property<Boolean>().convention(false)
    internal val withApiValidation = objects.property<Boolean>().convention(false)
    internal val apiValidationExcludedMarkers = objects.setProperty<String>()
    internal val allowedInternalApiMarkers = objects.setProperty<String>()

    val testing: SurfTestingExtension = objects.newInstance<SurfTestingExtension>()

    fun testing(action: Action<SurfTestingExtension>) {
        action.execute(testing)
    }

    /**
     * Turns on Kotlin's ABI validation for the project.
     *
     * @param excludedMarkers binary names of the annotations whose declarations the ABI dump
     *   leaves out, such as the project's own internal API marker.
     */
    fun withApiValidation(vararg excludedMarkers: String) {
        withApiValidation.set(true)
        withApiValidation.finalizeValue()

        apiValidationExcludedMarkers.set(excludedMarkers.toSet())
        apiValidationExcludedMarkers.finalizeValue()
    }

    /**
     * Lets the project's code reference declarations marked with [markers].
     *
     * `checkInternalApiUsage` fails the build when the project references a dependency's
     * declaration that carries an internal API marker, an annotation annotated with
     * `@InternalAPIMarker`. Only surf libraries built against that internal API, such as the
     * surf-api infra modules with `@InternalSurfApi`, allow a marker.
     *
     * @param markers binary names of the allowed markers, such as
     *   `dev.slne.surf.api.shared.api.util.InternalSurfApi`.
     */
    fun allowInternalApi(vararg markers: String) {
        allowedInternalApiMarkers.addAll(*markers)
    }

    fun withSurfRedis() {
        withSurfRedis.set(true)
        withSurfRedis.finalizeValue()
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
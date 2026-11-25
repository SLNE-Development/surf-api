package dev.slne.surf.api.gradle.internalapi

import dev.slne.surf.api.gradle.internalapi.fixture.consumer.CallsPublicApi
import java.io.File

/**
 * Locates the compiled fixtures: the consumer classes, and the test classes directory that holds
 * the library and the marker stand-ins.
 */
internal object InternalApiFixtures {
    const val INTERNAL_SURF_API = "dev.slne.surf.api.shared.api.util.InternalSurfApi"
    const val INTERNAL_LIBRARY_API =
        "dev.slne.surf.api.gradle.internalapi.fixture.library.InternalLibraryApi"

    private const val CONSUMER_PACKAGE_PATH =
        "dev/slne/surf/api/gradle/internalapi/fixture/consumer"

    /**
     * The test classes directory, used as the consumer's compile classpath.
     */
    val testClasses: File = File(
        CallsPublicApi::class.java.protectionDomain.codeSource.location.toURI(),
    )

    /**
     * Copies the consumer's classes into [target], keeping their package directories, and
     * returns [target].
     */
    fun copyConsumerClasses(target: File): File {
        testClasses.resolve(CONSUMER_PACKAGE_PATH)
            .copyRecursively(target.resolve(CONSUMER_PACKAGE_PATH), overwrite = true)

        return target
    }
}

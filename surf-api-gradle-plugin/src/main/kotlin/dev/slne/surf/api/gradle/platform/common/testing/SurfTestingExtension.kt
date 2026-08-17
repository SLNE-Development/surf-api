package dev.slne.surf.api.gradle.platform.common.testing

import dev.slne.surf.api.gradle.generated.Constants
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * Configures the default-on testing support.
 *
 * All toggles default to enabled; versions default to the values baked into the
 * plugin at build time. Surf-internal dependencies mirrored onto the test
 * classpath keep their dynamic `+` version.
 */
open class SurfTestingExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property<Boolean>().convention(true)

    val useJUnitPlatform: Property<Boolean> = objects.property<Boolean>().convention(true)

    val mockk: Property<Boolean> = objects.property<Boolean>().convention(true)
    val lincheck: Property<Boolean> = objects.property<Boolean>().convention(true)
    val mockBukkit: Property<Boolean> = objects.property<Boolean>().convention(true)
    val minestomTesting: Property<Boolean> = objects.property<Boolean>().convention(true)

    val junitVersion: Property<String> =
        objects.property<String>().convention(Constants.TEST_JUNIT_VERSION)
    val junitPlatformLauncherVersion: Property<String> =
        objects.property<String>().convention(Constants.TEST_JUNIT_PLATFORM_LAUNCHER_VERSION)
    val mockkVersion: Property<String> =
        objects.property<String>().convention(Constants.TEST_MOCKK_VERSION)
    val lincheckVersion: Property<String> =
        objects.property<String>().convention(Constants.TEST_LINCHECK_VERSION)
    val mockBukkitVersion: Property<String> =
        objects.property<String>().convention(Constants.TEST_MOCKBUKKIT_VERSION)
    val minestomTestingVersion: Property<String> =
        objects.property<String>().convention(Constants.TEST_MINESTOM_TESTING_VERSION)
    val coroutinesTestVersion: Property<String> =
        objects.property<String>().convention(Constants.TEST_COROUTINES_VERSION)
}

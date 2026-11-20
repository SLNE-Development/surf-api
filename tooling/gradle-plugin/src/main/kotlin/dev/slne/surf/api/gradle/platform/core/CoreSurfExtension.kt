package dev.slne.surf.api.gradle.platform.core

import dev.slne.surf.api.gradle.SurfCoreModules
import dev.slne.surf.api.gradle.platform.common.CommonSurfExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class CoreSurfExtension @Inject constructor(objects: ObjectFactory) :
    CommonSurfExtension(objects) {
    val authors = objects.listProperty<String>().convention(mutableListOf("SLNE Development"))

    /**
     * The package the generated classes live in: the main class, the Paper bootstrapper and the
     * Paper library loader. The plugin's component scan starts there as well.
     *
     * Applies only to generated classes. When the build script names the class the runtime scans
     * from (the Paper bootstrapper, the Velocity or Minestom main class), the scan starts at that
     * class's package and this property is ignored.
     *
     * Defaults to the longest common package of the plugin's own source files. Set it when the
     * sources share no package or only one that belongs to surf-api itself (`dev.slne`,
     * `dev.slne.surf`, `dev.slne.surf.api`). The build warns when the package does not contain
     * every source package.
     */
    val basePackage = objects.property<String>()

    /**
     * Whether the build generates the plugin's main class, and for Paper the bootstrapper.
     *
     * Turn it off for a module that applies the platform plugin only for its dependencies and
     * is not a plugin itself. The build then generates no class, and writes no Velocity or Minestom
     * plugin file and no Minestom service entry unless the build script names a main class. A Paper
     * descriptor takes its main class and bootstrapper from the build script alone.
     */
    val generatePluginClass = objects.property<Boolean>().convention(true)

    /**
     * Whether the build checks that a main class or bootstrapper named in the build script extends
     * the surf base class of the platform. The generated classes need no check.
     */
    val verifyPluginClasses = objects.property<Boolean>().convention(true)

    /**
     * Whether the plugin jar may contain Spring. surf-api ships Spring; a plugin that bundles its
     * own copy fails `checkNoBundledSpring` unless this is set.
     */
    val allowBundledSpring = objects.property<Boolean>().convention(false)

    fun withCoreCommon() {
        coreModule.set(SurfCoreModules.COMMON)
    }

    fun withCorePaper() {
        coreModule.set(SurfCoreModules.PAPER)
    }

    fun withCoreVelocity() {
        coreModule.set(SurfCoreModules.VELOCITY)
    }

    fun withCoreMinestom() {
        coreModule.set(SurfCoreModules.MINESTOM)
    }
}
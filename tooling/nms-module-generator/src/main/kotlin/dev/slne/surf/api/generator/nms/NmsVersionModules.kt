package dev.slne.surf.api.generator.nms

import dev.slne.surf.api.shared.internal.nms.NmsVersion

/**
 * Package segment of this version's module (e.g. `"v26_1"`).
 */
val NmsVersion.versionId: String
    get() = name.lowercase()

/**
 * Prefix of this version's class names (e.g. `"V26_1"`).
 */
val NmsVersion.classPrefix: String
    get() = name

/**
 * Directory name of this version's module, as `settings.gradle.kts` lists it (e.g. `"v26-1"`).
 */
val NmsVersion.slug: String
    get() = versionId.replace('_', '-')

/**
 * Relative path from the repository root to this version's Gradle module.
 */
val NmsVersion.modulePath: String
    get() = "$MODULE_BASE_PATH$slug"

/**
 * Flat Gradle project path of this version's module (e.g. `":surf-api-paper-nms-v26-1"`).
 *
 * `settings.gradle.kts` attaches each module directory to a single-segment project named after
 * the published artifact, so the project path does not mirror [modulePath].
 */
val NmsVersion.gradlePath: String
    get() = ":$MODULE_NAME_PREFIX$slug"

/**
 * Whether this version's module compiles the shared 26.x sources of `platform/paper/nms/shared-26`.
 */
val NmsVersion.usesShared26Template: Boolean
    get() = components.first() == 26

private const val MODULE_BASE_PATH = "platform/paper/nms/"
private const val MODULE_NAME_PREFIX = "surf-api-paper-nms-"
